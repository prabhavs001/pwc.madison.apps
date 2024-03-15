package com.pwc.madison.core.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.text.csv.Csv;
import com.pwc.madison.core.beans.VitalStatsReportRow;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.VitalStatsReportService;

/**
 * The Class VitalStatsReportServiceImpl is a service to retrieve the Vital
 * Stats Report as a List or Csv
 */
@Component(service = { VitalStatsReportService.class }, immediate = true)
public class VitalStatsReportServiceImpl implements VitalStatsReportService {

	private static final Logger LOG = LoggerFactory.getLogger(VitalStatsReportServiceImpl.class);

	private static final String ERROR_PARSING_DITA = "An error ocurred while parsing dita content";

	private static final String CHARACTERS = "CHARACTERS";
	
    /**
     *  QueryBuilder Service     
     */
    @Reference
    private QueryBuilder queryBuilder;
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.pwc.madison.core.services.VitalStatsReportService#getVitalStatsReport(
	 * java.lang.String, org.apache.sling.api.resource.ResourceResolver)
	 */
	@Override
	public List<VitalStatsReportRow> getVitalStatsReport(String path, ResourceResolver resourceResolver) {
		Resource folderResource = resourceResolver.getResource(path);
		List<VitalStatsReportRow> vitalStatsReport = new ArrayList<>();
		Iterator<Asset> assetIterator = DamUtil.getAssets(folderResource);
		while (assetIterator.hasNext()) {
			Asset currentAsset = assetIterator.next();
			String damSha1 = "";
			if(currentAsset.getMetadata(DamConstants.PN_SHA1)!=null) {
				damSha1 = currentAsset.getMetadata(DamConstants.PN_SHA1).toString();
			}
			VitalStatsReportRow vitalStatsReportRow = new VitalStatsReportRow();
			if (DITAConstants.APPLICATION_XML.equals(currentAsset.getMimeType())
					&& (currentAsset.getName().endsWith(DITAConstants.DITA_EXTENSION)
							|| currentAsset.getName().endsWith(DITAConstants.DITAMAP_EXT))) {
				InputStream ditaContent = getDitaContent(currentAsset);
				extractDetailsFromDita(ditaContent, vitalStatsReportRow);
			}
			vitalStatsReportRow.setAssetPath(currentAsset.getPath());
			vitalStatsReportRow.setDamSha1(damSha1);
			vitalStatsReport.add(vitalStatsReportRow);
		}
		return vitalStatsReport;
	}

	/**
	 * Extract details like words, links, paragraphs etc.. from dita.
	 *
	 * @param ditaContent         the dita content
	 * @param vitalStatsReportRow the vital stats report row
	 */
	private void extractDetailsFromDita(InputStream ditaContent, VitalStatsReportRow vitalStatsReportRow) {
		final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		saxParserFactory.setValidating(false);
		try {
			saxParserFactory.setFeature(DITAConstants.EXTERNAL_GENERAL_ENTITIES, false);
			saxParserFactory.setFeature(DITAConstants.EXTERNAL_PARAMETER_ENTITIES, false);
			saxParserFactory.setFeature(DITAConstants.EXTERNAL_DTD_PATH, false);
			saxParserFactory.setXIncludeAware(false);
			final SAXParser saxParser = saxParserFactory.newSAXParser();
			final DefaultHandler handler = new SAXHandler();
			saxParser.parse(ditaContent, handler);
			SAXHandler saxhandler = (SAXHandler) handler;
            if (saxhandler.getAnchorIds() != null) {
                String anchorIds = saxhandler.getAnchorIds().toString().replace("[", "").replace("]", "").replace(", ",
                        ":");
                vitalStatsReportRow.setAnchors(anchorIds);
            }
			vitalStatsReportRow.setLinks(saxhandler.getLinksCount());
			vitalStatsReportRow.setParagraphs(saxhandler.getParagraphCount());
			vitalStatsReportRow.setTables(saxhandler.getTableCount());
			vitalStatsReportRow.setWords(saxhandler.getWordsCount());
		} catch (ParserConfigurationException | SAXException | IOException e) {
			LOG.error(ERROR_PARSING_DITA, e);
		} finally {
            if(null != ditaContent) {
                try {
                    ditaContent.close();
                } catch(IOException io) {
                    LOG.error("Failed to close the stream");   
                }
            }
        }

	}

	/**
	 * Gets the dita content as a stream.
	 *
	 * @param currentAsset the current asset
	 * @return the dita content
	 */
	private InputStream getDitaContent(Asset currentAsset) {
		final InputStream inputStream = currentAsset.getRendition(DamConstants.ORIGINAL_FILE).getStream();
		final StringWriter writer = new StringWriter();
		try {
			IOUtils.copy(inputStream, writer, Charset.forName(MadisonConstants.UTF_8));
		} catch (IOException e) {
			LOG.error(ERROR_PARSING_DITA, e);
		}
		final String destAssetContent = writer.toString();
		return IOUtils.toInputStream(destAssetContent, Charset.forName(MadisonConstants.UTF_8));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.pwc.madison.core.services.VitalStatsReportService#getVitalStatsCsvReport(
	 * java.lang.String, org.apache.sling.api.resource.ResourceResolver,
	 * com.day.text.csv.Csv, java.io.Writer)
	 */
	@Override
	public Csv getVitalStatsCsvReport(String path, ResourceResolver resourceResolver, Csv csv, Writer writer) {
		List<VitalStatsReportRow> vitalStatsReport = getVitalStatsReport(path, resourceResolver);
		try {
			csv.writeRow("Asset Path", "Words", "Paragraphs", "Tables", "Links", "Anchors", "DAM SHA1");
			writer.flush();
		} catch (IOException e1) {
			LOG.error("An error while writing csv", e1);
		}
		Iterator<VitalStatsReportRow> vitalStatsReportIterator = vitalStatsReport.iterator();
		while (vitalStatsReportIterator.hasNext()) {
			VitalStatsReportRow vitalStatsReportRow = vitalStatsReportIterator.next();
			try {
				csv.writeRow(vitalStatsReportRow.getAssetPath(), String.valueOf(vitalStatsReportRow.getWords()),
						String.valueOf(vitalStatsReportRow.getParagraphs()),
						String.valueOf(vitalStatsReportRow.getTables()), String.valueOf(vitalStatsReportRow.getLinks()),
                        vitalStatsReportRow.getAnchors(), vitalStatsReportRow.getDamSha1());
				writer.flush();
			} catch (IOException e) {
				LOG.error("An error while writing csv", e);
			}
		}
		return csv;
	}

	/**
	 * The Class SAXHandler which parses the dita content xml and extracts the
	 * details of dita content like words count, links count, paragraphs count etc..
	 */
	class SAXHandler extends DefaultHandler {

		private List<String> anchorIds = new ArrayList<>();

		private int tableCount = 0;

		private int linksCount = 0;

		private int paragraphCount = 0;

		private StringBuilder builder;

		int wordsCount = 0;

		private String currentStartTag = "";

		private String previousElement = "";

		private String parentElement = "";

        	private boolean isRootElement = true;

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
		 * java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(final String uri, final String localName, final String qName,
				final Attributes attributes) {
            if (isRootElement) {
                isRootElement = false;
                return;
            }
            final int attributeLength = attributes.getLength();
            for (int i = 0; i < attributeLength; i++) {
                final String attrName = attributes.getQName(i);
                if (DITAConstants.ATTRIBUTE_ID.equals(attrName)) {
                    anchorIds.add(attributes.getValue(i));
                }
            }

			switch (qName) {
			case DITAConstants.DITA_TAG_TABLE: {
				tableCount++;
				break;
			}
			case DITAConstants.DITA_TAG_XREF: {
				linksCount++;
				break;
			}
			case DITAConstants.DITA_TAG_PWC_XREF: {
				linksCount++;
				break;
			}
			case DITAConstants.DITA_TAG_LIST_TAG: {
				paragraphCount++;
				break;
			}
			case DITAConstants.DITA_TAG_PARAGRAPH: {
				if (!currentStartTag.equals(DITAConstants.DITA_TAG_LIST_TAG) && !previousElement.equals("START")) {
					paragraphCount++;
				}
				break;
			}
			default:
				break;
			}
			currentStartTag = qName;
			parentElement = previousElement;
			previousElement = "START";
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
		 */
		@Override
		public void characters(char[] ch, int start, int length) {
			if (currentStartTag.equals(DITAConstants.DITA_TAG_SUPERSCRIPT) && parentElement.equals(CHARACTERS)
					&& !Character.isWhitespace(ch[start])) {
				currentStartTag = "";
				previousElement = CHARACTERS;
				return;
			}
			if (currentStartTag.equals(DITAConstants.DITA_TAG_ALT_TEXT)) {
				currentStartTag = "";
				previousElement = CHARACTERS;
				return;
			}
			builder = new StringBuilder();
			builder.append(ch, start, length);
			String completeString = builder.toString();
			completeString = StringUtils.trim(completeString);
			if (!StringUtils.isEmpty(completeString) && !completeString.matches("\\u00A0")) {
				char[] chars = new char[completeString.length()];
				for (int charIndex = 0; charIndex < completeString.length(); charIndex++) {
					chars[charIndex] = completeString.charAt(charIndex);
					if (((charIndex > 0) && (chars[charIndex] != ' ') && (chars[charIndex - 1] == ' '))
							|| ((chars[0] != ' ') && (chars[0] != ',') && (chars[0] != ';') && (chars[0] != '.')
									&& (charIndex == 0)))
						wordsCount++;
				}
			}
			previousElement = CHARACTERS;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
		 * java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			previousElement = "END";
		}

		public List<String> getAnchorIds() {
			return anchorIds;
		}

		public int getTableCount() {
			return tableCount;
		}

		public int getLinksCount() {
			return linksCount;
		}

		public int getParagraphCount() {
			return paragraphCount;
		}

		public StringBuilder getBuilder() {
			return builder;
		}

		public int getWordsCount() {
			return wordsCount;
		}

	}

    @Override
    public void getVitalStatsReportWithPagination(String path, ResourceResolver resourceResolver, int limit,
            int offset, List<Object> results) throws RepositoryException {
        
        if(null != resourceResolver && null != results) {
            
            final Session session = resourceResolver.adaptTo(Session.class);
            final Map<String, String> predicateMap = new HashMap<>();
            predicateMap.put("path", path);
            predicateMap.put("type", "dam:Asset");
            predicateMap.put("p.guessTotal", "true");
    
            final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap), session);
            query.setStart(offset);
            query.setHitsPerPage(limit);
    
            // execute the query
            final SearchResult result = query.getResult();
    
            for (final Hit hit : result.getHits()) {
                final Resource resource = resourceResolver.getResource(hit.getPath());
                final VitalStatsReportRow vitalStatsReportRow = new VitalStatsReportRow();
                if (null != resource) {
                    final Asset currentAsset = resource.adaptTo(Asset.class);
                    String damSha1 = StringUtils.EMPTY;
                    if (null != currentAsset.getMetadata(DamConstants.PN_SHA1)) {
                        damSha1 = currentAsset.getMetadata(DamConstants.PN_SHA1).toString();
                    }
                    if (DITAConstants.APPLICATION_XML.equals(currentAsset.getMimeType())
                            && (currentAsset.getName().endsWith(DITAConstants.DITA_EXTENSION)
                                    || currentAsset.getName().endsWith(DITAConstants.DITAMAP_EXT))) {
                        final InputStream ditaContent = getDitaContent(currentAsset);
                        extractDetailsFromDita(ditaContent, vitalStatsReportRow);
                    }
                    vitalStatsReportRow.setAssetPath(currentAsset.getPath());
                    vitalStatsReportRow.setDamSha1(damSha1);
                    results.add(vitalStatsReportRow);
                }
            }
        }
        
    }

}
