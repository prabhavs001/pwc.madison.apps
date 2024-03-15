package com.pwc.madison.core.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.adobe.granite.asset.api.AssetException;
import com.adobe.granite.asset.api.AssetManager;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.SyndicationResponse;
import com.pwc.madison.core.services.SyndicationService;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.SyndicationUtil;
import com.pwc.madison.core.util.XMLFormatter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * The Class SyndicationServiceImpl.
 */
@Component(service = { SyndicationService.class }, immediate = true)
public class SyndicationServiceImpl implements SyndicationService {

    /** The Constant DITAROOT. */
    private static final String DITAROOT = "ditaroot";
    
    /** The Constant DAM_PATH. */
    private static final String DAM_PATH = "dam/";
    
    /** The Constant PN_FMDITA_TARGET_PATH. */
    private static final String PN_FMDITA_TARGET_PATH = "fmdita-targetPath";
    
    /** The Constant DITA_ASSET_NAMEDOUTPUTS_PRESETS. */
    private static final List<String> DITA_ASSET_NAMEDOUTPUTS_PRESETS = Collections
            .unmodifiableList(new ArrayList<String>() {
                {
                    add("jcr:content/metadata/namedoutputs/aemsite");
                    add("jcr:content/metadata/namedoutputs/previewsite");
                    add("jcr:content/metadata/namedoutputs/workflowtopicregeneration");
                }
            });
    public static final String TOPICREF = "topicref";
    public static final String HREF = "href";
    public static final String MAP_TAG_NAME = "map";
    public static final String SELECTOR = ">";
    public static final String MAPREF = "mapref";
    public static final String OUTPUTCLASS = "outputclass";
    public static final String DESTINATION_ONLY_TOPIC = "destinationOnlyTopic";

    private final String AEM_SITE_NODE_PATH = "/jcr:content/metadata/namedoutputs/aemsite";
    
    /** The resource resolver factory. */
    @Reference
    ResourceResolverFactory resourceResolverFactory;
    
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SyndicationServiceImpl.class);

    /* (non-Javadoc)
     * @see com.pwc.madison.core.services.SyndicationService#processSyndication(org.apache.sling.api.resource.Resource, org.apache.sling.api.resource.Resource)
     */
    @Override
    public String processSyndication(final Resource sourceResource, final Resource destinationResource) {
        final SyndicationResponse response = new SyndicationResponse();
        ResourceResolver resourceResolver = null;
        final Gson gson = new GsonBuilder().create();
        try {
            resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                    MadisonConstants.SYNDICATION_SERVICE_USER);

            if (null == resourceResolver) {
                response.setErrorMsg("Not able process syndication for user : "
                        + MadisonConstants.SYNDICATION_SERVICE_USER + " as there is no permission");
                return gson.toJson(response);
            }

            final String sourceBasePath = sourceResource.getPath();
            final String destinationBasePath = destinationResource.getPath();
            response.setDestinationPath(destinationBasePath);

            final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setFeature(DITAConstants.EXTERNAL_GENERAL_ENTITIES, false);
            saxParserFactory.setFeature(DITAConstants.EXTERNAL_PARAMETER_ENTITIES, false);
            saxParserFactory.setFeature(DITAConstants.EXTERNAL_DTD_PATH, false);
            saxParserFactory.setXIncludeAware(false);
            saxParserFactory.setValidating(false);
            final SAXParser saxParser = saxParserFactory.newSAXParser();

            final AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);

            if (null == assetManager) {
                throw new NullPointerException("AssetManager is Empty");
            }
            LOGGER.debug("Syndication from sourcePath :: {} ::: to destPath::: {}", sourceBasePath,
                    destinationBasePath);

            final Iterator<Asset> assets = DamUtil.getAssets(sourceResource);
            while (assets.hasNext()) {
                final Asset srcAsset = assets.next();
                final String assetPath = srcAsset.getPath();

                if (DITAConstants.APPLICATION_XML.equals(srcAsset.getMimeType())
                        && (srcAsset.getName().endsWith(DITAConstants.DITA_EXTENSION)
                                || srcAsset.getName().endsWith(DITAConstants.DITAMAP_EXT))) {
                    LOGGER.debug("assets sourcepath :: {}  ", assetPath);
                    syndicateAsset(resourceResolver, assetManager, assetPath, sourceBasePath, destinationBasePath,
                            response, saxParser);
                    setSyndicatedAssetState(srcAsset,resourceResolver);
                }
            }
            setSyndicatedAssetState(sourceResource);
            if (resourceResolver.hasChanges()) {
                resourceResolver.refresh();
                resourceResolver.commit();
            }
        } catch (final Exception e) {
            LOGGER.error("processSyndication error {}", e);
            response.setErrorMsg(e.getMessage());
        } finally {
            if (null != resourceResolver && resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }
        return gson.toJson(response);
    }

    /**
     * Sets the syndicated asset state.
     *
     * @param asset the new syndicated asset state
     * @throws RepositoryException the repository exception
     */
	private void setSyndicatedAssetState(final Asset asset, ResourceResolver resourceResolver) {
		final Node srcAssetNode = asset.adaptTo(Node.class);
		if (null != srcAssetNode) {
			try {
				srcAssetNode.getNode(JcrConstants.JCR_CONTENT + DITAConstants.FORWARD_SLASH + DITAConstants.METADATA_NAME)
                .setProperty(DITAConstants.PN_IS_SYDICATED, true);
				LOGGER.debug("set isSyndicated true at source metadata");
			} catch (RepositoryException e) {
				LOGGER.error("setSyndicatedAssetState Exception {}", e);
			} 
		}
	}

    /**
     * Sets the syndicated asset state.
     *
     * @param resource the new syndicated asset state
     * @throws RepositoryException the repository exception
     */
    private void setSyndicatedAssetState(final Resource resource) throws RepositoryException {
        final Node srcAssetNode = resource.adaptTo(Node.class);
        if (null != srcAssetNode) {
            srcAssetNode.getNode(JcrConstants.JCR_CONTENT).setProperty(DITAConstants.PN_IS_SYDICATED, true);
        }
    }

    /**
     * Syndicate asset.
     *
     * @param resourceResolver the resource resolver
     * @param assetManager the asset manager
     * @param assetPath the asset path
     * @param sourceBasePath the source base path
     * @param destinationBasePath the destination base path
     * @param response the response
     * @param saxParser the sax parser
     */
    private void syndicateAsset(final ResourceResolver resourceResolver, final AssetManager assetManager,
            final String assetPath, final String sourceBasePath, final String destinationBasePath,
            final SyndicationResponse response, final SAXParser saxParser) {
        final String destAssetPath = assetPath.replace(sourceBasePath, destinationBasePath);
        try {
            while (!checkAssetParentPath(resourceResolver, destAssetPath, sourceBasePath, destinationBasePath)) {
                checkAssetParentPath(resourceResolver, destAssetPath, sourceBasePath, destinationBasePath);
            }
            String destFmditaLastPublishedPath = null;
			if (destAssetPath.endsWith(DITAConstants.DITAMAP_EXT)) {
				Resource existingAemSiteNode = resourceResolver.getResource(destAssetPath + AEM_SITE_NODE_PATH);
				if (existingAemSiteNode != null) {
					destFmditaLastPublishedPath = existingAemSiteNode.getValueMap().get(DITAConstants.PN_LAST_PUBLISHED_PATH,String.class);
				}
			}
            // Handle merging of source and destination ditamaps as we don't want to override the destination changes
			final Resource sourceAssetRes = resourceResolver.getResource(assetPath);
			if(null != sourceAssetRes && sourceAssetRes.getName().endsWith(DITAConstants.DITAMAP_EXT)) {
				copyOrMergeSourceDestinationDitaMap(sourceAssetRes, resourceResolver, saxParser, assetManager, destAssetPath);
			} else {
                LOGGER.debug("Coping this asset to destination ::> {}",assetPath);
                assetManager.copyAsset(assetPath, destAssetPath);
            }
            
            final Resource assetRes = resourceResolver.getResource(destAssetPath);
            if (null != assetRes) {
            	// giving a timeout so that any running workflow can be completed
            	TimeUnit.SECONDS.sleep(5);

                final Asset syndicatedAsset = assetRes.adaptTo(Asset.class);
                resetStatusProperty(resourceResolver, assetRes.getPath());
                if (null != syndicatedAsset && syndicatedAsset.getName().endsWith(DITAConstants.DITA_EXTENSION)) {
                    updateSyndicatedAsset(syndicatedAsset, assetPath, saxParser);

                    // set the doc state to Draft
                    final Resource metadataResource = assetRes.getChild(MadisonConstants.METADATA_RELATIVE_PATH);
                    final Node metadataResNode = metadataResource.adaptTo(Node.class);
					if (null != metadataResNode) {
						LOGGER.info("Setting docstate(draft) and isSyndicated(null) at metadata for the topic");
						metadataResNode.setProperty(DITAConstants.PN_METADATA_DOCSTATE,
								DITAConstants.DITA_DOCUMENTSTATE_DRAFT);
						if (metadataResNode.hasProperty(DITAConstants.PN_IS_SYDICATED)) {
							metadataResNode.setProperty(DITAConstants.PN_IS_SYDICATED, (Value) null);
						}
						LOGGER.info("docstate and isSyndicated(null) has been set");
					}
				} else if (assetRes.getName().endsWith(DITAConstants.DITAMAP_EXT)) {
					LOGGER.debug("Setting map metadata properties");
					for (final String outputPreset : DITA_ASSET_NAMEDOUTPUTS_PRESETS) {
						if (null != assetRes.getChild(outputPreset)) {
							final Node presetNode = assetRes.getChild(outputPreset).adaptTo(Node.class);
							String presetPath = destinationBasePath.replace(DAM_PATH, StringUtils.EMPTY);
							if ("jcr:content/metadata/namedoutputs/previewsite".equals(outputPreset)) {
								presetPath = presetPath.replace(MadisonConstants.PWC_MADISON_CONTENT_BASEPATH + DITAROOT+ DITAConstants.FORWARD_SLASH,MadisonConstants.PWC_MADISON_PREVIEW_BASEPATH);
							}
							presetNode.setProperty(PN_FMDITA_TARGET_PATH, presetPath);
							if (destFmditaLastPublishedPath != null && "jcr:content/metadata/namedoutputs/aemsite".equals(outputPreset)) {
								presetNode.setProperty(DITAConstants.PN_LAST_PUBLISHED_PATH,destFmditaLastPublishedPath);
							}
						}
					}
					LOGGER.debug("Map meta properties has beeen set");
				}
            }
        } 
		catch (AssetException | SAXException | IOException | RepositoryException e) {
			final List<String> errorFiles = response.getErrorFiles();
			errorFiles.add(destAssetPath);
			response.setErrorFiles(errorFiles);
			LOGGER.error("syndicateAsset Error: {}", e);
		}
        catch (Exception  e) {
            final List<String> errorFiles = response.getErrorFiles();
            errorFiles.add(destAssetPath);
            response.setErrorFiles(errorFiles);
            LOGGER.error("syndicateAsset Error: {}", e);
        }

}

    /**
     * @param sourceAssetRes
     * @param resourceResolver
     * @param saxParser
     * @param assetManager
     * @param destAssetPath
     */
    private void copyOrMergeSourceDestinationDitaMap(Resource sourceAssetRes, ResourceResolver resourceResolver,
			SAXParser saxParser, AssetManager assetManager, String destAssetPath) {
    	Resource destinationAssetRes = resourceResolver.getResource(destAssetPath);
        LOGGER.debug("copyOrMerging {}", sourceAssetRes.getPath());
    	if(null != resourceResolver && (destinationAssetRes != null ? !ResourceUtil.isNonExistingResource(destinationAssetRes) : false)) {
    		try {
				List<Element> sourceTopicRefs = getTopicRefs(sourceAssetRes, resourceResolver);
                List<Element> destTopicRefs = getTopicRefs(destinationAssetRes, resourceResolver);
                List<Element> destinationOnlyTopic = destTopicRefs.stream().filter(topic-> topic.attr(OUTPUTCLASS) != null && topic.attr(OUTPUTCLASS).equals(DESTINATION_ONLY_TOPIC)).collect(Collectors.toList());
                LOGGER.debug("Number of local topics in map {} are {}", sourceAssetRes.getName(), destinationOnlyTopic.size());
                final Asset srcAsset = sourceAssetRes.adaptTo(Asset.class);
                final InputStream srcInputStream = srcAsset.getRendition(DamConstants.ORIGINAL_FILE).getStream();
//                Document srcDocument = Jsoup.parse(srcInputStream, null, "", Parser.xmlParser());

                final Asset destAsset = destinationAssetRes.adaptTo(Asset.class);
                final InputStream destInputStream = destAsset.getRendition(DamConstants.ORIGINAL_FILE).getStream();
                Document destDocument = Jsoup.parse(destInputStream, null, "", Parser.xmlParser());

                Document tempDestDocument = Jsoup.parse(srcInputStream, null, "", Parser.xmlParser());

                destinationOnlyTopic.forEach(element -> {
                    int index = element.elementSiblingIndex()-1;
                    Element parentElement;
                    int elementLevel = getElementLevel(element);
                    LOGGER.debug(String.format("Handling Local Topic :: %s, at level %d, and elementSiblingIndex %d", element.attr(HREF), elementLevel, element.elementSiblingIndex()));
                    if (elementLevel <= 1) {
                        if (index == 0) {
                            // Handling local topic placed at first position in the hierarchy
                            parentElement = tempDestDocument.getElementsByTag(MAP_TAG_NAME).get(0);
                            Element titleElement = parentElement.selectFirst(TOPICREF);
                            titleElement.before(element.outerHtml());
                        } else if (destDocument.getElementsByTag(MAP_TAG_NAME).get(0).children().size() - 1 == element.elementSiblingIndex()) {
                            // Handling local topic placed at last position in the hierarchy
                            parentElement = tempDestDocument.getElementsByTag(MAP_TAG_NAME).get(0);
                            parentElement.appendChild(element);
                        } else {
                            // Handling local topic placed elsewhere in the hierarchy
                            parentElement = tempDestDocument.root();
                            Element newPrevSibling = parentElement.getElementsByIndexEquals(index+1).stream().filter(e->e.nodeName().equals(TOPICREF) || e.nodeName().equals(MAPREF)).collect(Collectors.toCollection(Elements::new)).get(0);
                            newPrevSibling.before(element.outerHtml());
                        }
                    }else{
                        // local topic element which has atleast one parent
                        Elements parentElementNewDest = tempDestDocument.getElementsByAttributeValue(HREF, element.parent().attr(HREF));
                        parentElementNewDest.forEach(parentElementOfSource -> {
                            if(parentElementOfSource.select(SELECTOR +TOPICREF).size() == 0) {
                                parentElementOfSource.insertChildren(0, element);
                            } else {
                                if(element.parent().selectFirst(SELECTOR+TOPICREF) == element) {
                                    // Handling local topic placed at first position in the hierarchy
                                    parentElementOfSource.selectFirst(SELECTOR+TOPICREF).before(element.outerHtml());
                                } else if (parentElementOfSource.children().size() == element.elementSiblingIndex()){
                                    // Handling local topic placed at last position in the hierarchy
                                    parentElementOfSource.appendChild(element);
                                } else {
                                    // Handling local topic placed elsewhere in the parent hierarchy
                                    Element siblingElement = parentElementOfSource.select(SELECTOR + TOPICREF).get(element.elementSiblingIndex()-1);
                                    siblingElement.after(element.outerHtml());
                                }
                            }
                        });
                    }

                });

                // Get destination asset again
                Resource destinationAssetResource = resourceResolver.getResource(destAssetPath);
                Asset destinationAsset = destinationAssetResource.adaptTo(Asset.class);

                //updates Desitnation file with source topic + destination only topics
                XMLFormatter formatter = new XMLFormatter();
                String modifiedAssetContent = formatter.format(tempDestDocument.toString());
                destinationAsset.addRendition("original", IOUtils.toInputStream(modifiedAssetContent.replace("&nbsp;",""), StandardCharsets.UTF_8), destinationAsset.getMimeType());
                LOGGER.debug("Successfully copied over to ::> {} ",destAssetPath);
                resourceResolver.commit();
			} catch (Exception e) {
                LOGGER.error("Error during syndication :: copyOrMergeSourceDestinationDitaMap "+e);
			}
    	} else {
    		String sourceAssetPath = sourceAssetRes.getPath();
    		LOGGER.debug("Coping this asset to destination ::> {}", sourceAssetPath);
            assetManager.copyAsset(sourceAssetPath, destAssetPath);
    	}
    	final Asset sourceAsset = sourceAssetRes.adaptTo(Asset.class);
    	final InputStream inputStream = sourceAsset.getRendition(DamConstants.ORIGINAL_FILE).getStream();
        final StringWriter writer = new StringWriter();
        try {
			IOUtils.copy(inputStream, writer, Charset.forName(MadisonConstants.UTF_8));
			final String destAssetContent = writer.toString();
			final DefaultHandler handler = new SAXHandler();
			saxParser.parse(IOUtils.toInputStream(destAssetContent, Charset.forName(MadisonConstants.UTF_8)), handler);
		} catch (IOException | SAXException e) {
			e.printStackTrace();
		}
	}

    private static int getElementLevel(Element element) {
        int level = 0;
        Elements parents = element.parents();
        // Count the number of parents to determine the level
        for (Element parent : parents) {
            level++;
        }
        return level;
    }
    private List<Element> getTopicRefs(Resource assetResource, ResourceResolver resourceResolver) throws IOException {
        final Asset asset = assetResource.adaptTo(Asset.class);
        final InputStream destInputStream = asset.getRendition(DamConstants.ORIGINAL_FILE).getStream();
        Document document = Jsoup.parse(destInputStream, null, "", Parser.xmlParser());

        List<Element> topicRefs= new ArrayList<>();

        // Find all topicref elements
        for (Element topicRefElement : document.select(TOPICREF)) {
            topicRefs.add(topicRefElement);
        }
        return topicRefs;
    }

    /**
     * Reset status property.
     *
     * @param resourceResolver            resourceResolver
     * @param assetPath            assetPath
     * @throws RepositoryException             RepositoryException
     */
    private void resetStatusProperty(final ResourceResolver resourceResolver, final String assetPath)
	{
		try {
			final Resource resource = resourceResolver.getResource(assetPath + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
			if (null == resource) {
				LOGGER.debug("resource is empty {}", resource);
				return;
			}
			final Node node = resource.adaptTo(Node.class);
			if (null != node && node.hasProperty(DITAConstants.STATUS_PROP_NAME)) {
				node.getProperty(DITAConstants.STATUS_PROP_NAME).remove();
				LOGGER.debug(DITAConstants.STATUS_PROP_NAME+ " property has been removed from destination asset jcr content node");
			}
		} catch (RepositoryException e) {
			LOGGER.error("Exception while reseting status property at dest jcr node {} ",e);
		}
	}

    

    /**
     * Check asset parent path.
     *
     * @param resourceResolver the resource resolver
     * @param destAssetPath the dest asset path
     * @param sourceBasePath the source base path
     * @param destinationBasePath the destination base path
     * @return true, if successful
     * @throws PersistenceException the persistence exception
     * @throws RepositoryException the repository exception
     */
    private boolean checkAssetParentPath(final ResourceResolver resourceResolver, final String destAssetPath,
            final String sourceBasePath, final String destinationBasePath)
            throws PersistenceException, RepositoryException {

        boolean isParExists = false;

        if (StringUtils.isBlank(destAssetPath)) {
            return isParExists;
        }

        final String destAssetParPath = destAssetPath.substring(0,
                destAssetPath.lastIndexOf(DITAConstants.FORWARD_SLASH));
        final Session session = resourceResolver.adaptTo(Session.class);
        if (null != session && !session.itemExists(destAssetParPath)) {
            final String resSourcePath = destAssetParPath.replace(destinationBasePath, sourceBasePath);
            final Resource sourceRes = resourceResolver.getResource(resSourcePath);
            final String destAssetParFolderPath = destAssetParPath.substring(0,
                    destAssetParPath.lastIndexOf(DITAConstants.FORWARD_SLASH));
            final Resource destAssetParFolderRes = resourceResolver.getResource(destAssetParFolderPath);
            if (null == destAssetParFolderRes) {
                checkAssetParentPath(resourceResolver, destAssetParPath, sourceBasePath, destinationBasePath);
            }

            if (null != destAssetParFolderRes && null != sourceRes) {
                final ModifiableValueMap valueMap = sourceRes.adaptTo(ModifiableValueMap.class);
                valueMap.put(DITAConstants.PN_IS_SYDICATED, true);
                resourceResolver.create(destAssetParFolderRes, sourceRes.getName(), valueMap);
                final Resource parResource = resourceResolver.getResource(destAssetParPath);
                if (null != parResource) {
                    final Resource childRes = sourceRes.getChild(JcrConstants.JCR_CONTENT);
                    resourceResolver.create(parResource, JcrConstants.JCR_CONTENT, childRes.getValueMap());
                    isParExists = true;
                }
            }
        } else {
            isParExists = true;
        }

        return isParExists;

    }

    /**
     * Update syndicated asset.
     *
     * @param asset the asset
     * @param srcAssetPath the src asset path
     * @param saxParser the sax parser
     * @throws SAXException the SAX exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void updateSyndicatedAsset(final Asset asset, final String srcAssetPath, final SAXParser saxParser)
            throws SAXException, IOException {
        final InputStream inputStream = asset.getRendition(DamConstants.ORIGINAL_FILE).getStream();
        final StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer, Charset.forName(MadisonConstants.UTF_8));
        final String destAssetContent = writer.toString();
        final DefaultHandler handler = new SAXHandler();
        saxParser.parse(IOUtils.toInputStream(destAssetContent, Charset.forName(MadisonConstants.UTF_8)), handler);

        final String bodyDivId = ((SAXHandler) handler).getBodyDivId();
        final String bodyDivType = ((SAXHandler) handler).getBodyDivType();
        if (StringUtils.isBlank(bodyDivId)) {
            LOGGER.error("asset for the path is not having the bodydiv with id :: {} ", asset.getPath());
            throw new NullPointerException("Dita is Not having BodyDiv id");
        }
        final String conrefValue = SyndicationUtil.getRelativePath(srcAssetPath, asset.getPath())
                + DITAConstants.HASH_STR + bodyDivId;
        String bodyDivConrefElement = StringUtils.EMPTY;
        String bodyDivTagEnd = StringUtils.EMPTY;
        String bodyDivRegex = StringUtils.EMPTY;
        String bodyDivEmptyRegex = StringUtils.EMPTY;
        if (bodyDivType.equals(DITAConstants.STATIC_STR_TOPIC)) {
            bodyDivConrefElement = DITAConstants.DITA_BODYDIV_CONREF_ELEMENT;
            bodyDivTagEnd = DITAConstants.DITA_BODYDIV_TAG_END;
            bodyDivRegex = DITAConstants.BODYDIV_REGEX;
            bodyDivEmptyRegex = DITAConstants.BODYDIV_EMPTY_REGEX;
        } else if (bodyDivType.equals(DITAConstants.STATIC_STR_FAQ)) {
            bodyDivConrefElement = DITAConstants.FAQ_DITA_BODYDIV_CONREF_ELEMENT;
            bodyDivTagEnd = DITAConstants.FAQ_DITA_BODYDIV_TAG_END;
            bodyDivRegex = DITAConstants.FAQ_BODYDIV_REGEX;
            bodyDivEmptyRegex = DITAConstants.FAQ_BODYDIV_EMPTY_REGEX;
        } else if (bodyDivType.equals(DITAConstants.STATIC_STR_EXAMPLE)) {
            bodyDivConrefElement = DITAConstants.EXAMPLE_DITA_BODYDIV_CONREF_ELEMENT;
            bodyDivTagEnd = DITAConstants.EXAMPLE_DITA_BODYDIV_TAG_END;
            bodyDivRegex = DITAConstants.EXAMPLE_BODYDIV_REGEX;
            bodyDivEmptyRegex = DITAConstants.EXAMPLE_BODYDIV_EMPTY_REGEX;
        }
        final String bodyDivWithConref = bodyDivConrefElement + conrefValue + bodyDivTagEnd;
        String processedDita = destAssetContent.replaceAll(DITAConstants.NEWLINE_REGEX, StringUtils.EMPTY)
                .replaceAll(bodyDivRegex, bodyDivWithConref).replaceAll(bodyDivEmptyRegex, bodyDivWithConref);
        final XMLFormatter formatter = new XMLFormatter();
        processedDita = formatter.format(processedDita);

        asset.addRendition(DamConstants.ORIGINAL_FILE,
                IOUtils.toInputStream(processedDita, Charset.forName(MadisonConstants.UTF_8)), asset.getMimeType());
        LOGGER.debug("Conref element has been added and renditions created successfully ");

    }

    /* (non-Javadoc)
     * @see com.pwc.madison.core.services.SyndicationService#processSyndication(java.util.List, org.apache.sling.api.resource.Resource, org.apache.sling.api.resource.Resource)
     */
    @Override
    public String processSyndication(final Set<String> resources, final Resource sourceResource,
            final Resource destinationResource) {
        final SyndicationResponse response = new SyndicationResponse();
        ResourceResolver resourceResolver = null;
        final Gson gson = new GsonBuilder().create();
        try {
            resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                    MadisonConstants.SYNDICATION_SERVICE_USER);

            if (null == resourceResolver) {
                response.setErrorMsg("Not able process syndication for user : "
                        + MadisonConstants.SYNDICATION_SERVICE_USER + " as there is no permission");
                return gson.toJson(response);
            }

            final String sourceBasePath = sourceResource.getPath();
            final String destinationBasePath = destinationResource.getPath();
            response.setDestinationPath(destinationBasePath);

            final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setFeature(DITAConstants.EXTERNAL_GENERAL_ENTITIES, false);
            saxParserFactory.setFeature(DITAConstants.EXTERNAL_PARAMETER_ENTITIES, false);
            saxParserFactory.setFeature(DITAConstants.EXTERNAL_DTD_PATH, false);
            saxParserFactory.setXIncludeAware(false);
            saxParserFactory.setValidating(false);
            final SAXParser saxParser = saxParserFactory.newSAXParser();

            final AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);

            if (null == assetManager) {
                throw new NullPointerException("AssetManager is Empty");
            }
            LOGGER.debug("Syndication from sourcePath :: {} ::: to destPath::: {}", sourceBasePath, destinationBasePath);
            for (final String resource : resources) {
                final Resource resSrc = resourceResolver.getResource(resource);
                final Asset srcAsset = resSrc != null ? resSrc.adaptTo(Asset.class) : null;
                if (null != srcAsset) {
                    final String assetPath = srcAsset.getPath();

                    if (DITAConstants.APPLICATION_XML.equals(srcAsset.getMimeType())
                            && (srcAsset.getName().endsWith(DITAConstants.DITA_EXTENSION)
                                    || srcAsset.getName().endsWith(DITAConstants.DITAMAP_EXT))) {
                        syndicateAsset(resourceResolver, assetManager, assetPath, sourceBasePath, destinationBasePath,
                                response, saxParser);
                        setSyndicatedAssetState(srcAsset,resourceResolver);
                    }
                }
            }
            LOGGER.debug("Setting isSyndication at sourceFolder");
            setSyndicatedAssetState(sourceResource);
            if (resourceResolver.hasChanges()) {
                resourceResolver.refresh();
                resourceResolver.commit();
            }
            LOGGER.debug("after isSyndication set at source folder");
        } catch (final Exception e) {
            LOGGER.error("processSyndication error {}", e);
            response.setErrorMsg(e.getMessage());
        } finally {
            if (null != resourceResolver && resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }
        return gson.toJson(response);
    }

    /* (non-Javadoc)
     * @see com.pwc.madison.core.services.SyndicationService#copyNonEditableMetadata(java.util.List, org.apache.sling.api.resource.Resource, org.apache.sling.api.resource.Resource, org.apache.sling.api.resource.ResourceResolver)
     */
    @Override
    public void copyNonEditableMetadata(final Set<Resource> toBeUpdatedAssets, final Resource syndicationSource,
            final Resource territory, final ResourceResolver resourceResolver) {

        if (null == resourceResolver || null == syndicationSource || null == territory || toBeUpdatedAssets.isEmpty()) {
            return;
        }

        final List<String> editableProperties = getEditablePropertiesFromConfig();
        List<String> inclusionProperties = getGenericInclusionPropertiesFromConfig();

        final String sourcePath = syndicationSource.getPath();
        final String destinationPath = territory.getPath();
        for (final Resource sourceResource : toBeUpdatedAssets) {
            final String destinationResourcePath = sourceResource.getPath().replace(sourcePath, destinationPath);

            final Resource destinationResource = resourceResolver.getResource(destinationResourcePath);

            if (null == destinationResource) {
                continue;
            }

            final Resource sourceMetadata = sourceResource.getChild(MadisonConstants.METADATA_RELATIVE_PATH);
            final Resource destinationMetadata = destinationResource.getChild(MadisonConstants.METADATA_RELATIVE_PATH);

            if (null == sourceMetadata || null == destinationMetadata) {
                continue;
            }

            final ValueMap sourceValueMap = sourceMetadata.getValueMap();
            final Map<String, Object> metadataTobeCopied = getMetadataToBeCopied(sourceValueMap, editableProperties,inclusionProperties);

            final ModifiableValueMap destinationValueMap = destinationMetadata.adaptTo(ModifiableValueMap.class);
            destinationValueMap.putAll(metadataTobeCopied);

            try {
                destinationResource.getResourceResolver().commit();
            } catch (final PersistenceException e) {
                LOGGER.error("Unable to save metadata properties for {} ", destinationPath, e);
            }
        }
    }

    /**
     * Gets the metadata to be copied.
     *
     * @param sourceValueMap the source value map
     * @param editableProperties the editable properties
     * @param inclusionProperties 
     * @return the metadata to be copied
     */
    // exclude properties that are editable and not pwc related
    private Map<String, Object> getMetadataToBeCopied(final ValueMap sourceValueMap,
            final List<String> editableProperties, List<String> inclusionProperties) {

        Map<String, Object> metadataToBeCopied = new HashMap<String, Object>();

        for (final String key : sourceValueMap.keySet()) {
            getValidKey(sourceValueMap, editableProperties, inclusionProperties, key,metadataToBeCopied);
        }

        // set the docstate to Draft
        metadataToBeCopied.put(DITAConstants.PN_METADATA_DOCSTATE, DITAConstants.DITA_DOCUMENTSTATE_DRAFT);

        return metadataToBeCopied;
    }

    
    /**
     * Checks if is valid key.
     *
     * @param sourceValueMap the source value map
     * @param editableProperties the editable properties
     * @param inclusionProperties 
     * @param metadataToBeCopied the metadata to be copied
     * @param key the key
     */
    private void getValidKey(final ValueMap sourceValueMap, final List<String> editableProperties,
            List<String> inclusionProperties, final String key,Map<String, Object> metadataToBeCopied) {
        if (!editableProperties.contains(key) && key.startsWith("pwc") || inclusionProperties.contains(key)) {
            metadataToBeCopied.put(key, sourceValueMap.get(key));
        }
    }

    /**
     * Gets the editable properties from config.
     *
     * @return the editable properties from config
     */
    // read syndication editable properties from ref data
    private List<String> getEditablePropertiesFromConfig() {
        final List<String> editableProperties = new ArrayList<>();

        final ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                MadisonConstants.MADISON_READ_SUB_SERVICE);

        if (null == resourceResolver) {
            return editableProperties;
        }

        final Resource editablePropertiesRefData = resourceResolver
                .getResource(MadisonConstants.SYNDICATION_EDITABLE_PROPERTIES_REF_DATA_PATH);

        if (null == editablePropertiesRefData) {
            return editableProperties;
        }

        final Iterator<Resource> items = editablePropertiesRefData.listChildren();

        while (items.hasNext()) {
            final Resource item = items.next();
            final ValueMap valueMap = item.getValueMap();

            if (null == valueMap) {
                continue;
            }

            final String propertyName = valueMap.get("name", "");

            if (StringUtils.isNotBlank(propertyName)) {
                editableProperties.add(propertyName);
            }
        }

        if (resourceResolver.isLive()) {
            resourceResolver.close();
        }

        return editableProperties;
    }
    
    
    /**
     * Gets the generic inclusion properties from config.
     *
     * @return the generic inclusion properties from config
     */
    private List<String> getGenericInclusionPropertiesFromConfig() {
        final List<String> editableProperties = new ArrayList<>();

        final ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                MadisonConstants.MADISON_READ_SUB_SERVICE);

        if (null == resourceResolver) {
            return editableProperties;
        }

        final Resource editablePropertiesRefData = resourceResolver
                .getResource(MadisonConstants.GENERIC_INCLUSION_PROPERTIES_REF_DATA_PATH);

        if (null == editablePropertiesRefData) {
            return editableProperties;
        }

        final Iterator<Resource> items = editablePropertiesRefData.listChildren();

        while (items.hasNext()) {
            final Resource item = items.next();
            final ValueMap valueMap = item.getValueMap();

            if (null == valueMap) {
                continue;
            }

            final String propertyName = valueMap.get("name", "");

            if (StringUtils.isNotBlank(propertyName)) {
                editableProperties.add(propertyName);
            }
        }

        if (resourceResolver.isLive()) {
            resourceResolver.close();
        }

        return editableProperties;
    }
}



class SAXHandler extends DefaultHandler {
    private String bodyDivId = StringUtils.EMPTY;
    private String bodyDivType = StringUtils.EMPTY;

    @Override
    public void startElement(final String uri, final String localName, final String qName,
            final Attributes attributes) {
        if (DITAConstants.DITA_TAG_BODY_DIV.equals(qName)) {
            bodyDivType = DITAConstants.STATIC_STR_TOPIC;
            setBodyDivId(attributes);
        } else if (DITAConstants.FAQ_DITA_TAG_BODY_DIV.equals(qName)) {
            bodyDivType = DITAConstants.STATIC_STR_FAQ;
            setBodyDivId(attributes);
        } else if (DITAConstants.EXAMPLE_DITA_TAG_BODY_DIV.equals(qName)) {
            bodyDivType = DITAConstants.STATIC_STR_EXAMPLE;
            setBodyDivId(attributes);
        }
    }

    private void setBodyDivId(final Attributes attributes) {
        final int attributeLength = attributes.getLength();
        for (int i = 0; i < attributeLength; i++) {
            final String attrName = attributes.getQName(i);
            if (DITAConstants.ATTRIBUTE_ID.equals(attrName)) {
                bodyDivId = attributes.getValue(i);
            }
        }

    }

    public String getBodyDivId() {
        return bodyDivId;
    }

    public String getBodyDivType() {
        return bodyDivType;
    }
}
