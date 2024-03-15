package com.pwc.madison.core.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.models.CitationPattern;
import com.pwc.madison.core.models.CitationTextFileRefrenceModel;
import com.pwc.madison.core.services.FetchCitationPatternService;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Service implementation to fetch <autolink> elements from dita topics
 */
@Component(service = { FetchCitationPatternService.class }, immediate = true)
public class FetchCitationPatternServiceImpl implements FetchCitationPatternService {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceResolverFactory resolverFactory;
    
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;
    
    private static final String DITA_EXTENSION = ".dita";
    private static final String AUTOLINK = "autolink";

    @Override
    public Map<String, CitationTextFileRefrenceModel> CollectCitationPattern(final CitationPattern citationPattern,
            final String searchPath) {
    	
    	final String regex = citationPattern.getRegex();

        if (StringUtils.isBlank(regex)) {
        	LOGGER.debug("Regex is blank for citation pattern {}", citationPattern.getId());
            return null;
        }
        
        ResourceResolver resolver = null;
        Map<String, CitationTextFileRefrenceModel> citationMap = new HashMap<String, CitationTextFileRefrenceModel>();

        try {
        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        	Document document;
        	NodeList nList;
        	factory.setFeature(DITAConstants.EXTERNAL_GENERAL_ENTITIES, false);
            factory.setFeature(DITAConstants.EXTERNAL_PARAMETER_ENTITIES, false);
            factory.setFeature(DITAConstants.EXTERNAL_DTD_PATH, false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
        	DocumentBuilder builder = factory.newDocumentBuilder();

            resolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());

            if (null == resolver) {
                LOGGER.error(" ResourceResolver null in CollectCitationPattern for user {}", madisonSystemUserNameProviderService.getFmditaServiceUsername());
                return null;
            }

			if (searchPath.endsWith(DITA_EXTENSION)) {
				final Asset asset = null != resolver.getResource(searchPath) ? resolver.getResource(searchPath).adaptTo(Asset.class) : null;
				if (asset != null) {
					final InputStream inStream = asset.getRendition(DamConstants.ORIGINAL_FILE).getStream();
					LOGGER.debug("Citation Text Asset Node {}", asset.getPath());
					document = builder.parse(inStream);
					nList = document.getElementsByTagName(AUTOLINK);
					if (null != nList) {
						for (int temp = 0; temp < nList.getLength(); temp++) {
							Element element = (Element) nList.item(temp);
							final String citationValue = element.getTextContent();
							LOGGER.debug("Citation found: {}", citationValue);
							if (Pattern.compile(regex).matcher(citationValue).matches()) {
								LOGGER.debug("Citation Text: {} matches Regex: {}", citationValue, regex);
								final CitationTextFileRefrenceModel citationTextFileRefrenceModel = new CitationTextFileRefrenceModel();
								citationTextFileRefrenceModel.setCitationText(citationValue);
								final Set<String> filePaths = new HashSet<>();
								filePaths.add(asset.getPath());
								citationTextFileRefrenceModel.setFilePaths(filePaths);
								citationMap.put(citationValue, citationTextFileRefrenceModel);
							}

						}
					}
					if (null != inStream) {
						inStream.close();
	                }
				}
			}
            
            else {
            	
            	final String query = "SELECT * FROM [dam:Asset] AS nodes WHERE ISDESCENDANTNODE([" + searchPath
                        + "]) AND NAME() LIKE '%.dita' AND nodes.[jcr:content/ditameta/autolink/text] IS NOT NULL";
            	
                final Iterator<Resource> results = resolver.findResources(query, javax.jcr.query.Query.JCR_SQL2);

                if (null == results) {
                	LOGGER.debug("Query resulted in null");
                    return null;
                }
                
				while (results.hasNext()) {
					final Resource ditaTopic = results.next();
					final Asset asset = null != resolver.getResource(ditaTopic.getPath()) ? resolver.getResource(ditaTopic.getPath()).adaptTo(Asset.class) : null;
					if (asset != null) {
						final InputStream inStream = asset.getRendition(DamConstants.ORIGINAL_FILE).getStream();
						LOGGER.debug("Citation Text Asset Node {}", ditaTopic.getPath());
						document = builder.parse(inStream);
						nList = document.getElementsByTagName(AUTOLINK);
						if (null != nList) {
							for (int temp = 0; temp < nList.getLength(); temp++) {
								Element element = (Element) nList.item(temp);
								final String citationValue = element.getTextContent();
								if (Pattern.compile(regex).matcher(citationValue).matches()) {
									LOGGER.debug("Citation Text: {} matches Regex: {}", citationValue, regex);
									final CitationTextFileRefrenceModel citationTextFileRefrenceModel;
									if (citationMap.containsKey(citationValue)) {
										citationTextFileRefrenceModel = citationMap.get(citationValue);
										final Set<String> filePaths = citationTextFileRefrenceModel.getFilePaths();
										filePaths.add(ditaTopic.getPath());
										citationTextFileRefrenceModel.setFilePaths(filePaths);
										citationMap.put(citationValue, citationTextFileRefrenceModel);
									} else {
										citationTextFileRefrenceModel = new CitationTextFileRefrenceModel();
										citationTextFileRefrenceModel.setCitationText(citationValue);
										final Set<String> filePaths = new HashSet<>();
										filePaths.add(ditaTopic.getPath());
										citationTextFileRefrenceModel.setFilePaths(filePaths);
										citationMap.put(citationValue, citationTextFileRefrenceModel);
									}
								}
							}
						}
						if (null != inStream) {
							inStream.close();
		                }
						
					}
				}
            }

            LOGGER.debug("citationMap: {}",citationMap.toString());
            return citationMap;

        } catch (final SAXException e) {
            LOGGER.error("SAXException occurred in the CollectCitationPattern {}", e);
            return citationMap;
        } catch (final IOException e) {
            LOGGER.error("IOException occurred in the CollectCitationPattern {}", e);
            return citationMap;
        } catch (final ParserConfigurationException e) {
            LOGGER.error("ParserConfigurationException occurred in the CollectCitationPattern {}", e);
            return citationMap;
        } finally {
            if (null != resolver && resolver.isLive()) {
                resolver.close();
            }
        }

    }

}
