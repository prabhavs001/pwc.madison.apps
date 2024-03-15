package com.pwc.madison.core.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.SAXParser;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
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
import com.day.text.csv.Csv;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.SyndicationDataPojo;
import com.pwc.madison.core.services.SyndicationFailureReportService;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.SyndicationUtil;

@Component(service = { SyndicationFailureReportService.class }, immediate = true)
public class SyndicationFailureReportServiceImpl implements SyndicationFailureReportService {

	@Reference
	transient ResourceResolverFactory resourceResolverFactory;
	private ResourceResolver syndicationResourceResolver;
	private final String SEPARATOR = ",";
	private final String PUBLISH_DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";
	private final String NOT_PUBLISHED = "Not Published";
	private final String UK = "uk";
	private final String PUBLISH_DATE_NOT_PRESENT = "Publish date not present";
	private final String NOT_APPLICABLE = "N/A";
	
	private static final Logger LOG = LoggerFactory.getLogger(SyndicationFailureReportServiceImpl.class);

	@Override
	public void writeFailureReport(String sourcePath, Csv csv, Writer writer) {
		syndicationResourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
				MadisonConstants.SYNDICATION_SERVICE_USER);
		try {

			/* Get Subscribing territories for given folder */
			Map<String, List<String>> syndicationSubscribersMap = SyndicationUtil.getSyndicatedSubscribersMap(sourcePath, syndicationResourceResolver);
			if (syndicationSubscribersMap.size() > 0) {
				int toalCount = 0;
				int failedCount = 0;
				List<SyndicationDataPojo> syndicationDataList = new ArrayList<>();

				for (Entry<String, List<String>> entry : syndicationSubscribersMap.entrySet()) {

					String sourceFolderPath = entry.getKey();
					Resource sourceFolder = syndicationResourceResolver.getResource(sourceFolderPath);
					
					if (sourceFolder != null) {
						List<String> syndicationSubscribersList = entry.getValue();
						String ukFolderPath = null, caFolderPath = null, frFolderPath = null;

						for (String subscriberPath : syndicationSubscribersList) {
							if (subscriberPath.contains("uk/en")) {
								ukFolderPath = subscriberPath;
							} else if (subscriberPath.contains("ca/en")) {
								caFolderPath = subscriberPath;
							} else if (subscriberPath.contains("ca/fr")) {
								frFolderPath = subscriberPath;
							}
						}
						LOG.debug("Checking synidcation status for folder : {} ", sourceFolderPath);

						Iterator<Asset> assetIterator = DamUtil.getAssets(sourceFolder);
						while (assetIterator.hasNext()) {

							String assetPath = assetIterator.next().getPath();
							if (null != assetPath && (assetPath.endsWith(DITAConstants.DITA_EXTENSION)
									|| assetPath.endsWith(DITAConstants.DITAMAP_EXT))) {

								Resource currentResource = syndicationResourceResolver.getResource(assetPath);
								if (null != currentResource) {
									toalCount++;

									SyndicationDataPojo syndicationDataPojo = new SyndicationDataPojo();
									syndicationDataPojo.setSrcPath(assetPath);
									syndicationDataPojo.setSourceSyndicationStatus(true);
									List<String> syndicatedContentStatusList = new ArrayList<>(3);

									checkDestTeritorySyndicationStatus(assetPath, sourceFolderPath, ukFolderPath,
											syndicatedContentStatusList, syndicationDataPojo);
									checkDestTeritorySyndicationStatus(assetPath, sourceFolderPath, caFolderPath,
											syndicatedContentStatusList, syndicationDataPojo);
									checkDestTeritorySyndicationStatus(assetPath, sourceFolderPath, frFolderPath,
											syndicatedContentStatusList, syndicationDataPojo);

									if (!syndicationDataPojo.isSourceSyndicationStatus()) {
										failedCount++;
									}
									syndicationDataPojo.setSyndicatedContentStatusList(syndicatedContentStatusList);
									syndicationDataList.add(syndicationDataPojo);
								}
							}
						}
					}
				}
				writeDataInCsv(sourcePath, csv, toalCount, failedCount, syndicationDataList, writer);
			}
		} catch (IOException e) {
			LOG.error("An error while genrating syndication failure report", e);
		} finally {
			if (null != syndicationResourceResolver && syndicationResourceResolver.isLive()) {
				syndicationResourceResolver.close();
			}
		}
	}

	private void checkDestTeritorySyndicationStatus(String srcAssetPath, String srcFolderPath,
			String destFolderPath, List<String> syndicatedContentStatusList,
			SyndicationDataPojo syndicationDataPojo) {

		if (destFolderPath != null) {
			String destAssetPath = srcAssetPath.replace(srcFolderPath, destFolderPath);
			Resource destAsset = syndicationResourceResolver.getResource(destAssetPath);

			if (null != destAsset) {
				// Dita file is present at destination territory
				syndicatedContentStatusList.add("Yes");
			} else {
				/* Asset is not present as destination territory */
				syndicatedContentStatusList.add("No");
				syndicationDataPojo.setSourceSyndicationStatus(false);
			}
		} else {
			syndicatedContentStatusList.add(NOT_APPLICABLE);
		}
	}
	
	private void checkDitaTopicForSync(Resource destResource, SAXParser saxParser,
			List<String> syndicatedContentStatusList, SyndicationDataPojo syndicationDataPojo)
			throws IOException, SAXException {
		try {
			Asset syndicatedAsset = destResource.adaptTo(Asset.class);
			if (syndicatedAsset != null && syndicatedAsset.getRendition(DamConstants.ORIGINAL_FILE) != null) {

				InputStream inputStream = syndicatedAsset.getRendition(DamConstants.ORIGINAL_FILE).getStream();

				StringWriter stringWriter = new StringWriter();

				IOUtils.copy(inputStream, stringWriter, Charset.forName(MadisonConstants.UTF_8));

				String destAssetContent = stringWriter.toString();
				DefaultHandler handler = new SyndicationSAXHandler();
				saxParser.parse(IOUtils.toInputStream(destAssetContent, Charset.forName(MadisonConstants.UTF_8)),
						handler);

				boolean isSyndicated = ((SyndicationSAXHandler) handler).isSyndicated();
				if (isSyndicated) {
					syndicatedContentStatusList.add("Yes");
				} else {
					syndicatedContentStatusList.add("No/Not in sync");
					syndicationDataPojo.setSourceSyndicationStatus(false);
				}
			} else {
				/* original rendition does not exist for the topic */
				syndicatedContentStatusList.add("No/Not in sync");
				syndicationDataPojo.setSourceSyndicationStatus(false);
			}
		} catch (SAXException e) {
			LOG.error("SAXException occured for {} ditatopic {}", destResource.getPath(), e);
		} catch (IOException e) {
			LOG.error("IOException occured for {} ditatopic", destResource.getPath());
			throw e;
		}
	}

	private void writeDataInCsv(String sourcePath, Csv csv, int toalCount, int failedCount,
			List<SyndicationDataPojo> syndicationDataList, Writer writer)
			throws IOException {
		csv.writeRow("Path to the source ditamap", String.join(SEPARATOR, sourcePath));
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		LocalDateTime now = LocalDateTime.now();
		csv.writeRow("Report date", String.join(SEPARATOR, dtf.format(now)));
		csv.writeRow("Total File Count", String.join(SEPARATOR, String.valueOf(toalCount)));
		csv.writeRow("Failed File Count", String.join(SEPARATOR, String.valueOf(failedCount)));
		csv.writeRow();

		List<String> columns = new ArrayList<>(4);
		columns.add("Global Topic");
		columns.add("Syndicated to UK?");
		columns.add("Syndicated to CA EN?");
		columns.add("Syndicated to CA FR?");

		csv.writeRow(columns.toArray(new String[columns.size()]));

		for (SyndicationDataPojo syndicationData : syndicationDataList) {
			List<String> csvFileds = new ArrayList<String>();
			csvFileds.add(syndicationData.getSrcPath());
			List<String> syndicatedContentStatusList = syndicationData.getSyndicatedContentStatusList();
			for (String syndicatedContentStatus : syndicatedContentStatusList) {
				csvFileds.add(syndicatedContentStatus);
			}
			csv.writeRow(csvFileds.toArray(new String[csvFileds.size()]));
		}
		writer.flush();
	}

	@Override
	public void writePublishReport(String sourcePath, Csv csv, Writer writer) {
		syndicationResourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
				MadisonConstants.SYNDICATION_SERVICE_USER);
		try {
			writeStaticFiledsinPublishCsvReport(sourcePath,csv);

			Map<String, List<String>> syndicationSubscribersMap = SyndicationUtil.getSyndicatedSubscribersMap(sourcePath, syndicationResourceResolver);
			if (syndicationSubscribersMap.size() > 0) {
				for (Entry<String, List<String>> entry : syndicationSubscribersMap.entrySet()) {

					String sourceFolderPath = entry.getKey();
					Resource sourceFolder = syndicationResourceResolver.getResource(sourceFolderPath);

					if (sourceFolder != null) {
						List<String> syndicationSubscribersList = entry.getValue();
						String ukFolderPath = null, caFolderPath = null, frFolderPath = null;

						for (String subscriberPath : syndicationSubscribersList) {
							if (subscriberPath.contains("uk/en")) {
								ukFolderPath = subscriberPath;
							} else if (subscriberPath.contains("ca/en")) {
								caFolderPath = subscriberPath;
							} else if (subscriberPath.contains("ca/fr")) {
								frFolderPath = subscriberPath;
							}
						}
						String query = "SELECT parent.* FROM [dam:Asset] AS parent INNER JOIN [nt:unstructured] AS child ON ISDESCENDANTNODE(child,parent) WHERE ISDESCENDANTNODE(parent, ["
								+ sourceFolderPath+ "]) AND parent.[jcr:content/metadata/pwc:isPublishingPoint] = 'yes' and name(child) = 'aemsite'";

						final Iterator<Resource> results = syndicationResourceResolver.findResources(query,javax.jcr.query.Query.JCR_SQL2);
						while (results.hasNext()) {
							String srcPublishingPointPath = results.next().getPath();
							LOG.debug("Checking publishing details for PP {} ", srcPublishingPointPath);
							List<String> csvFileds = new ArrayList<String>();
							csvFileds.add(srcPublishingPointPath);

							Date srcPublishDate = null;
							if (syndicationResourceResolver.getResource(srcPublishingPointPath) != null) {
								Resource aemSiteRes = getSite(srcPublishingPointPath);
								if (aemSiteRes != null) {
									writeDynamicFiledsinPublishReport(aemSiteRes, csvFileds, srcPublishDate,
											srcPublishingPointPath, sourceFolderPath, ukFolderPath, caFolderPath, frFolderPath);
								}
								else {
									Resource metadataRes = syndicationResourceResolver.getResource(srcPublishingPointPath + MadisonConstants.METADATA_PATH);
									csvFileds.add(metadataRes.getValueMap().get(DITAConstants.PROPERTY_TITLE, String.class));

									csvFileds.add(NOT_PUBLISHED); // Gx not published
									
									StringBuilder republishingRequire = new StringBuilder();
									checkPublshingStatus(ukFolderPath, "UK", csvFileds, republishingRequire); // check publishing status for uk
									checkPublshingStatus(caFolderPath, "CA EN", csvFileds, republishingRequire); // check publishing status for ca en
									checkPublshingStatus(frFolderPath, "CA FR", csvFileds, republishingRequire); // check publishing status for ca fr
									
									csvFileds.add(StringUtils.substringBeforeLast(republishingRequire.toString(),SEPARATOR));
								}
								csv.writeRow(csvFileds.toArray(new String[csvFileds.size()]));
								writer.flush();
							} 
						}
					}
				}
			}
		} catch (IOException e) {
			LOG.error("An error while genrating syndication publish date report", e);
		} finally {
			if (null != syndicationResourceResolver && syndicationResourceResolver.isLive()) {
				syndicationResourceResolver.close();
			}
		}
	}
	
	private void checkPublshingStatus(String folderPath, String folderName, List<String> csvFileds, StringBuilder republishingRequire) {
		if (folderPath != null) {
			csvFileds.add(NOT_PUBLISHED); 
			republishingRequire.append(folderName);
			republishingRequire.append(SEPARATOR);
		} else {
			csvFileds.add(NOT_APPLICABLE);
		}
	}
	/**
	 * Get jcr node of the generated site of a given publishing point
	 * @param publishingPointPath
	 * @return
	 */
	private Resource getSite(String publishingPointPath) {

		Resource aemSiteRes = syndicationResourceResolver.getResource(publishingPointPath + DITAConstants.AEMSITE_PRESETS_NODE);
		if (aemSiteRes != null) {
			String rootPagePath = aemSiteRes.getValueMap().get(DITAConstants.PN_LAST_PUBLISHED_PATH, String.class);

			// Check if fmdita-lastPublishedPath property present in aemSite node
			if (StringUtils.isNotBlank(rootPagePath)) {
				rootPagePath = rootPagePath.substring(0, rootPagePath.indexOf(MadisonConstants.HTML_EXTN));
				final Resource rootPageResource = syndicationResourceResolver.getResource(rootPagePath + DITAConstants.JCR_CONTENT);
				return rootPageResource;
			}
		}
		return null;
	}
	private void setDestTeritoriesValues(String srcPublishingPointPath, String srcFolderPath, String destFolderPath,
			List<String> csvFileds, Date srcPublishDate, List<String> republishingRequiredList) {

		if (destFolderPath != null) {
			String destPublishingPointPath = srcPublishingPointPath.replace(srcFolderPath, destFolderPath);
			Resource destPublishingPoint = syndicationResourceResolver.getResource(destPublishingPointPath);

			String republishingRequiredListString = getRepublishingRequiredListData(destFolderPath);
			republishingRequiredList.add(republishingRequiredListString);

			// Check if map is present over destination territory or not
			if (destPublishingPoint != null) {

				final Resource destSite = getSite(destPublishingPointPath);

				// Check whether site is generated or not for this publishing point
				if (destSite != null) {

					Date destPublishDate = destSite.getValueMap().get(DITAConstants.META_LAST_REPLICATED_DATE,
							Date.class);

					if (destPublishDate != null) {
						csvFileds.add(MadisonUtil.getDate(destPublishDate, PUBLISH_DATE_FORMAT));
						if (srcPublishDate != null && destPublishDate.after(srcPublishDate)) {
							republishingRequiredList.remove(republishingRequiredListString);
						}
					} else {
						csvFileds.add(PUBLISH_DATE_NOT_PRESENT);
						republishingRequiredList.remove(republishingRequiredListString);
					}
				} else {
					csvFileds.add(NOT_PUBLISHED);
				}
			} else {
				csvFileds.add("Not Available");
			}
		} else {
			csvFileds.add(NOT_APPLICABLE);
		}
	}
	
	private String getRepublishingRequiredListData(String destFolderPath) {
		if (destFolderPath.contains(UK)) {
			return "UK";
		} else if (destFolderPath.contains("ca/en")) {
			return "CA EN";
		} else if (destFolderPath.contains("ca/fr")) {
			return "CA FR";
		}
		return "";
	}
	
	private void writeStaticFiledsinPublishCsvReport(String sourcePath, Csv csv) throws IOException {
		csv.writeRow("Path to the source", String.join(SEPARATOR, sourcePath));
		csv.writeRow("Report date", String.join(SEPARATOR, MadisonUtil.getDate(new Date(), PUBLISH_DATE_FORMAT)));
		csv.writeRow();

		
		List<String> columns = new ArrayList<>(7);
		columns.add("Source publish point path (ditamap)");
		columns.add("Source publish point title");
		columns.add("Last Publish (GX)");
		columns.add("Last Publish (UK)");
		columns.add("Last Publish (CA EN)");
		columns.add("Last Publish (CA FR)");
		columns.add("Republishing Required ?");

		csv.writeRow(columns.toArray(new String[columns.size()]));
	}

	private void writeDynamicFiledsinPublishReport(Resource aemSiteRes, List<String> csvFileds, Date srcPublishDate,
			String srcPublishingPointPath, String sourceFolderPath, String ukFolderPath, String caFolderPath,
			String frFolderPath) {

		ValueMap valueMap = aemSiteRes.adaptTo(ValueMap.class);
		String mapTitle = valueMap.get("jcr:title", String.class);
		csvFileds.add(mapTitle);

		srcPublishDate = valueMap.get(DITAConstants.META_LAST_REPLICATED_DATE, Date.class);
		if (srcPublishDate != null) {
			csvFileds.add(MadisonUtil.getDate(srcPublishDate, PUBLISH_DATE_FORMAT));
		} else {
			csvFileds.add(PUBLISH_DATE_NOT_PRESENT);
		}

		List<String> republishingRequiredList = new ArrayList<>(3);
		setDestTeritoriesValues(srcPublishingPointPath, sourceFolderPath, ukFolderPath, csvFileds, srcPublishDate,
				republishingRequiredList);
		setDestTeritoriesValues(srcPublishingPointPath, sourceFolderPath, caFolderPath, csvFileds, srcPublishDate,
				republishingRequiredList);
		setDestTeritoriesValues(srcPublishingPointPath, sourceFolderPath, frFolderPath, csvFileds, srcPublishDate,
				republishingRequiredList);

		if (republishingRequiredList.size() != 0) {
			StringBuilder sb = new StringBuilder();
			for (String republishRequired : republishingRequiredList) {
				sb.append(republishRequired);
				sb.append(SEPARATOR);
			}
			csvFileds.add(StringUtils.substringBeforeLast(sb.toString(), SEPARATOR));
		} else {
			csvFileds.add("No");
		}

	}
}

class SyndicationSAXHandler extends DefaultHandler {
	private boolean isSyndicated = false;

	@Override
	public void startElement(final String uri, final String localName, final String qName,
			final Attributes attributes) {
		if (DITAConstants.DITA_TAG_BODY_DIV.equals(qName) || DITAConstants.FAQ_DITA_TAG_BODY_DIV.equals(qName)
				|| DITAConstants.EXAMPLE_DITA_TAG_BODY_DIV.equals(qName)) {

			final int attributeLength = attributes.getLength();
			for (int i = 0; i < attributeLength; i++) {
				final String attrName = attributes.getQName(i);
				if (DITAConstants.ATTRIBUTE_CONREF.equals(attrName)) {
					isSyndicated = true;
					break;
				}
			}
		}
	}

	public boolean isSyndicated() {
		return isSyndicated;
	}
}
