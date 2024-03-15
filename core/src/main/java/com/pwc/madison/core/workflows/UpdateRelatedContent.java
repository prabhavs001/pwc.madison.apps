package com.pwc.madison.core.workflows;

import java.util.*;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import com.pwc.madison.core.services.DownloadPDFConfigurationService;
import com.pwc.madison.core.services.FootNoteService;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.beans.PostProcessing;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MediaWrapperUpdateService;
import com.pwc.madison.core.services.ReplicateReferecedAssetsService;
import com.pwc.madison.core.services.SeeAlsoService;
import com.pwc.madison.core.services.TocContentService;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;

import static com.pwc.madison.core.constants.DITAConstants.*;

/**
 *
 */
@Component(service = WorkflowProcess.class, property = { "process.label= Madision - Update Related Content" })
public class UpdateRelatedContent implements WorkflowProcess {

    private static final String EQUALITY_SEPARATOR = "=";
    private static final String PROPERTY_SHOW_TOC = "showToc";
    private static final String PROPERTY_ID = "id";
    private static final String PN_TYPE = "type";
    private static final String PN_ITEM_TYPE_RELATED_CONTENT_PAGE = "relatedContentPage";
    private static final String PN_ITEM_TYPE_TEMPLATE_PAGES = "templatePages";
    private static final String PN_ITEM_TYPE = "itemType";
    private static final String GENERATED_PATH = "generatedPath";
    private static final String IS_SUCCESS = "isSuccess";
    private static final String HTML_EXTENSION = ".html";
    private static final String DITA_EXTENSION = ".dita";
    private static final String PN_PAGES = "pages";
    private static final String STATIC = "static";
    private static final String SME_RESOURCE_PATH = "pwc-madison/components/content/sme-list";
    private static final String RELATED_PAGE_LIST_RESOURCE_PATH = "pwc-madison/components/content/related-page-list";
    private static final String PN_LIST_FROM = "listFrom";
    private static final String PN_SLING_RESOURCE_TYPE = "sling:resourceType";
    private static final String JCR_CONTENT_METADATA = "jcr:content/metadata";
    private static final String OUTPUT_NAME = "outputName";
    private static final String OUTPUT_SETTING = "outputSetting";
    private static final String PREVIEW_SITE = "Preview Site";
    private static final String GENERATE_OUTPUT = "GENERATEOUTPUT";
    private static final String YES = "yes";
    private static final String EFFECTIVE_SOURCE_PATH = "effectiveSourcePath";
    private static final String MAP_PARENT = "mapParent";
    private static final String BASE_PATH = "basePath";
    private static final String TOC_NODE = "toc";
    private static final String PROPERTY_SITE_TITLE = "siteTitle";
    private static final String REPLICATION_STATUS = "cq:lastReplicationAction";
    private static final String ACTIVATE = "Activate";
    private static final String METADATA_ERROR = "Workflow metadata null. Cannot proceed with Update.";
    private static final String RESOLVER_ERROR = "Workflow ResourceResolver null. Cannot proceed with Update.";
    private static final String SITE_GENERATION_TYPE = "aemSiteGenType";

    // This Array will get added with additional Related Content Property in coming Sprint/Release.
    private static final String[] ASSET_PN_LIST = new String[] { DITAConstants.PN_SME,
            DITAConstants.PN_TEMPLATE_AND_TOOLS, DITAConstants.META_CONTENT_ID, META_CONTENT_TYPE,
            DITAConstants.META_PUBLICATION_DATE, DITAConstants.META_REVISION_DATE, DITAConstants.PN_RELATED_CONTENT,
            DITAConstants.META_DESCRIPTION, DITAConstants.META_KEYWORDS, DITAConstants.META_LANGUAGE,
            DITAConstants.META_SECONDARY_LANGUAGE, DITAConstants.META_LOCALE, DITAConstants.META_TAGS,
            DITAConstants.META_CREATED_DATE, DITAConstants.META_ORIGINAL_RELEASE_DATE,
            DITAConstants.META_SCHEDULED_PUBLISHING_DATE, DITAConstants.META_EXPIRY_DATE,
            DITAConstants.META_EFFECTIVE_AS_OF_DATE, DITAConstants.META_AUDIENCE, DITAConstants.META_PRIVATE_GROUP,
            DITAConstants.META_ACCESS_LEVEL, DITAConstants.META_LICENSE, DITAConstants.META_RELATED_EXAMPLE_LINKS,
            DITAConstants.META_RELATED_INSIGHTS, DITAConstants.META_STANDARD_SETTERS, DITAConstants.META_COPYRIGHT,
            DITAConstants.META_FAQ, DITAConstants.META_HIDE_PAGE_FROM_SITE, DITAConstants.META_ROBOTS,
            DITAConstants.META_TOPIC_IMAGE, DITAConstants.META_GUIDANCE_TERMS, DITAConstants.META_SUGGESTED_GUIDANCE,
            DITAConstants.META_DISABLE_PDF_DWNLD, DITAConstants.META_DISABLE_FEATURE_SUMMARY, DITAConstants.META_SORT_ORDER,
            DITAConstants.META_DOC_CONTEXT_SEARCH_IDENTIFIER, DITAConstants.META_SUGGESTED_GUIDANCE_ORDER, DITAConstants.META_HIDE_PUBLICATION_DATE,
            DITAConstants.META_HIDE_SEARCH_WITH_IN_DOC, DITAConstants.PN_IS_PUBLISHING_POINTS, DITAConstants.META_SHOW_STATIC_TOC,DITAConstants.META_LOAD_LITE_TOC,
            DITAConstants.META_HIDE_RELATED_CONTENT, DITAConstants.META_JOINED_SECTION_TOC, DITAConstants.META_JOINED_SECTION_LEVEL, DITAConstants.META_OVERRIDE_GLOBAL_JOIN_SETTINGS,
            DITAConstants.META_PWC_SEE_ALSO_ENABLED, DITAConstants.META_PWC_SEE_ALSO_MAX_DISPLAY_COUNT, DITAConstants.META_PWC_SEE_ALSO_OVERRIDE_SEE_ALSO, DITAConstants.META_PWC_SEE_ALSO_USED_IN,
            META_PWC_SEE_ALSO_DEFAULT_USED_IN_REFS, META_PWC_SEE_ALSO_CONTENT_TYPE, META_PWC_SEE_ALSO_TOPIC_CONTENT_TYPE
    };

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Reference
    protected SlingRepository repository;
    @Reference
    private WorkflowService workflowService;
    @Reference
    private ResourceResolverFactory resolverFactory;
    @Reference
    private QueryBuilder queryBuilder;
    @Reference
    private MediaWrapperUpdateService mediaWrapperUpdateService;
    @Reference
    private ReplicateReferecedAssetsService replicateReferecedAssetsService;
    @Reference
    Replicator replicator;
    @Reference
    private DownloadPDFConfigurationService downloadPDFConfigurationService;
    @Reference
    private FootNoteService footNoteService;
    
    @Reference
    private TocContentService tocContentService;
    
    @Reference
    private SeeAlsoService seeAlsoService;

    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    private boolean isMediaWrapperPage = false;

    private boolean isOverride = false;

    @Override
    public void execute(final WorkItem item, final WorkflowSession wfsession, final MetaDataMap args)
            throws WorkflowException {
        long startTime = System.currentTimeMillis();
        PostProcessing postProcessing = new PostProcessing();
        postProcessing.setFailure(false);
        postProcessing.appendString(System.lineSeparator());
        postProcessing.appendString("POST PROCESSING WORKFLOW: " + item.getWorkflowData().getPayload().toString());
        postProcessing.appendString(System.lineSeparator());
        postProcessing.appendString("------------------------------");
        postProcessing.appendString(System.lineSeparator());
        ResourceResolver resolver = null;
        Session session = null;
        String outputHistoryPath = StringUtils.EMPTY;
        final MetaDataMap meta = item.getWorkflowData().getMetaDataMap();
        isOverride = false;
        try {
            if (null == meta) {
                log.error(METADATA_ERROR);
                postProcessing.setFailure(true);
                postProcessing.appendException(METADATA_ERROR);
                return;
            }

            outputHistoryPath = meta.get(DITAConstants.OUTPUT_HISTORY_PATH, String.class);
            String outputSettings = meta.get("outputSetting", String.class);
            final String ditaOTGenerationSuccess = meta.get(IS_SUCCESS, String.class);
            final String outputPath = meta.get(GENERATED_PATH, String.class);
            final String outputName = meta.get(OUTPUT_NAME, String.class);
            final String siteGenerationType = meta.get(SITE_GENERATION_TYPE, String.class);


            log.debug("Update Page Properties Workflow ditaOTGenerationSuccess ::: {} ", ditaOTGenerationSuccess);
            log.debug("Update Page Properties Workflow outputPath ::: {} ", outputPath);
            log.debug("Update Page Properties Workflow outputName ::: {} ", outputName);

            resolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
            session = resolver.adaptTo(Session.class);

            if (StringUtils.isBlank(ditaOTGenerationSuccess) || ditaOTGenerationSuccess.equals("false") || StringUtils.isBlank(outputPath)) {
                log.error(MadisonConstants.EXCEPTION_OUTPUT_TYPE_NULL);
                postProcessing.setFailure(true);
                postProcessing.appendException(MadisonConstants.EXCEPTION_OUTPUT_TYPE_NULL);
                return;
            }

            if (!outputPath.contains(HTML_EXTENSION)) {
                log.error("This is not AEMSITE generation, so not executing postprocessing workflow {} ", outputPath);
                postProcessing.setFailure(true);
                postProcessing.appendException("This is not AEMSITE generation, so not executing postprocessing workflow: "+ outputPath);
                return;
            }
            final String sourcePath = outputPath.substring(0, outputPath.indexOf(HTML_EXTENSION));
            if (null == resolver) {
                log.error(RESOLVER_ERROR);
                postProcessing.setFailure(true);
                postProcessing.appendException(RESOLVER_ERROR);
                return;
            }


            if (!sourcePath.startsWith(MadisonConstants.PWC_MADISON_PREVIEW_BASEPATH)
                    && "AEMSITE".equalsIgnoreCase(outputName)) {
                // update the redirect target for intermediary pages to the territory homepage as they don't have a
                // template
                updateIntermediaryPageProperties(sourcePath, resolver, postProcessing);
            }
            isOverrideSet(resolver.getResource(sourcePath), resolver);
            Resource baseContentResource = resolver.getResource(sourcePath + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
            updateRelatedPathsToPage(resolver.getResource(sourcePath), resolver,baseContentResource, postProcessing);

            final Map<String, Object> predicateMap = getPredicateMap(sourcePath);
            final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap), session);
            final SearchResult searchResult = query.getResult();

            final Iterator<Resource> resources = searchResult.getResources();
            ArrayList<String> pathList = new ArrayList<>();
            while (resources.hasNext()) {
                final Resource resource = resources.next();
                pathList.add(resource.getPath());
                Resource contentResource = resolver.getResource(resource.getPath().concat(DITAConstants.JCR_CONTENT));
                if(null != contentResource){
                    ValueMap properties = contentResource.getValueMap();
                    if(properties.containsKey(DITAConstants.SOURCE_PATH)){
                        updateRelatedPathsToPage(resource, resolver, baseContentResource, postProcessing);
                        updatePDFDownloadProperties(resource, resolver, sourcePath);
                        updateMediaWrapperProperties(resource, resolver);
                        replicateReferecedAssetsService.replicateReferencedAssets(resource);
                    }
                }
            }
            //update Footnote's HTML
            footNoteService.updateFootnoteHTMLString(sourcePath);
            
            // Joined section
            int joinedLevel = 0;
            boolean isJoinedEnabledAtPublishingPoint = false;
            Map<Integer, String> overrideJoinMap = new HashMap<>();
            if (null != baseContentResource && !ResourceUtil.isNonExistingResource(baseContentResource)) {
                ValueMap valueMap = baseContentResource.adaptTo(ValueMap.class);
                joinedLevel = valueMap.containsKey(DITAConstants.META_JOINED_SECTION_LEVEL) ? valueMap.get(DITAConstants.META_JOINED_SECTION_LEVEL, Integer.class) : 0;
                if (YES.equals(valueMap.get(DITAConstants.META_JOINED_SECTION_TOC, String.class))) {
                    isJoinedEnabledAtPublishingPoint = true;
                    getOverrideJoinViewSettings(overrideJoinMap, valueMap);
                }
                // Handle See Also section
                boolean seeAlsoEnable = valueMap.containsKey(DITAConstants.META_PWC_SEE_ALSO_ENABLED) && "yes".equalsIgnoreCase(valueMap.get(DITAConstants.META_PWC_SEE_ALSO_ENABLED, String.class)) ? true : false;
                if(seeAlsoEnable) {
                    seeAlsoService.addSeeAlsoSection(resolver, sourcePath);
                }
            }
            if (isJoinedEnabledAtPublishingPoint) {
                tocContentService.createAndUpdateJoinedSectionPage(resolver, sourcePath, joinedLevel, overrideJoinMap, isJoinedEnabledAtPublishingPoint);
            }
            if(siteGenerationType.equals(GENERATE_OUTPUT)){
                Collections.sort(pathList);
                MadisonUtil.setLevel(pathList, resolver);
            }

            if (resolver.hasChanges()) {
                resolver.commit();
            }
        } catch (final Exception e) {
            postProcessing.setFailure(true);
            postProcessing.appendLog(e);
            log.error("Failed to update pages  ", e);
        } finally {
            meta.put(MadisonConstants.FAILURE, postProcessing.isFailure());
            meta.put(MadisonConstants.EXCEPTIONS, postProcessing.getException().toString());
            if(null == resolver){
                return;
            }
            boolean isPreviewSite = isPreview(outputHistoryPath, resolver);
            if(isPreviewSite && postProcessing.isFailure() && (null != postProcessing.getException())){
                MadisonUtil.setFailure(true, outputHistoryPath, session);
                MadisonUtil.storeErrorLog(resolver, postProcessing, outputHistoryPath, session);
            }
            if (resolver != null) {
                resolver.close();
            }
        }
        long endTime = System.currentTimeMillis();
        log.debug("Madision - Update Related Content took {} seconds to complete the process", (endTime - startTime) / 1000);
    }

    private void getOverrideJoinViewSettings(Map<Integer, String> overrideJoinMap, ValueMap valueMap) {
        if(valueMap.containsKey(DITAConstants.META_OVERRIDE_GLOBAL_JOIN_SETTINGS)){
            String[] overrideGlobalJoinSettings = valueMap.get(DITAConstants.META_OVERRIDE_GLOBAL_JOIN_SETTINGS, String[].class);
            for(String setting : overrideGlobalJoinSettings) {
                String[] settingArray = setting.trim().split(EQUALITY_SEPARATOR);
                try {
                    if(settingArray.length == 2) {
                        int key = Integer.parseInt(settingArray[0]);
                        overrideJoinMap.put(key, settingArray[1]);
                    } else {
                        log.warn("Specific format is now followed for overriding Global Join View setting");
                    }
                } catch (NumberFormatException e) {
                    log.error("For chapter wrong number provided in string format, cannot convert from String to Integer  {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Update the redirect target for the intermediary pages after site generation to the territory home page. Adding
     * redirectTarget and redirect resource type to the page properties.
     */
    private void updateIntermediaryPageProperties(final String sourcePath, final ResourceResolver resolver, PostProcessing postProcessing) {
        if (null == resolver || StringUtils.isBlank(sourcePath)) {
            return;
        }
        log.debug("updateIntermediaryPageProperties for the path: {}", sourcePath);
        final Session session = resolver.adaptTo(Session.class);
        final Resource ditaMapResource = resolver.getResource(sourcePath);

        if (null == ditaMapResource) {
            return;
        }

        final String laguageCode = MadisonUtil.getLanguageCodeForPath(sourcePath);
        final String territoryCode = MadisonUtil.getTerritoryCodeForPath(sourcePath);
        final String redirectTarget = MadisonConstants.PWC_MADISON_CONTENT_BASEPATH + territoryCode + "/" + laguageCode;
        final String basePathVal = StringUtils.substring(MadisonConstants.PWC_MADISON_CONTENT_BASEPATH, 0,
                MadisonConstants.PWC_MADISON_CONTENT_BASEPATH.length() - 1);
        Resource parentResource = ditaMapResource.getParent();
        final List<String> pagePaths = new ArrayList<>();
        Resource contentResource = null;
        while (null != parentResource && !parentResource.getName().equals(laguageCode)
                && !parentResource.getPath().equals(basePathVal)) {
            try {

                log.debug("updating page properties for path: {}", parentResource.getPath());
                 contentResource = parentResource.getChild(JcrConstants.JCR_CONTENT);

                if (null == contentResource) {
                    postProcessing.setFailure(true);
                    postProcessing.appendException("JCR:Content node missing for the resource: "+ parentResource.getPath());
                    log.error("JCR:Content node missing for the resource: {}", parentResource.getPath());
                    break;
                }

                final ModifiableValueMap valueMap = contentResource.adaptTo(ModifiableValueMap.class);
                if (null != valueMap) {
                    valueMap.put("redirectTarget", redirectTarget);
                    valueMap.put("sling:resourceType", "foundation/components/redirect");
                    // Replicate intermediate pages
                    if (!(valueMap.containsKey(REPLICATION_STATUS)
                            && valueMap.get(REPLICATION_STATUS).toString().equals(ACTIVATE))) {
                        pagePaths.add(parentResource.getPath());
                        log.debug("size of page path: {}", pagePaths.size());
                    }
                }

                parentResource = parentResource.getParent();
            } catch (final Exception e) {
                postProcessing.setFailure(true);
                postProcessing.appendLog(e);
                log.error("Failed while updating parent pages: {}", e);
            }
        }

        try {
            if (resolver.hasChanges()) {
                resolver.commit();
            }

            // Replicate intermediate pages
            for (final String pagePath : pagePaths) {
                replicator.replicate(session, ReplicationActionType.ACTIVATE, pagePath);
            }

        } catch (final PersistenceException | ReplicationException e) {
            postProcessing.setFailure(true);
            postProcessing.appendException("Unable to set redirect target for intermediary page");
            postProcessing.appendLog(e);
            log.error("Unable to set redirect target for intermediary page ", e);
        }
    }

    /**
     * Update media wrapper properties.
     *
     * @param resource
     *            the resource
     * @param resolver
     *            the resolver
     */
    private void updateMediaWrapperProperties(final Resource resource, final ResourceResolver resolver) {
        final Node pageNode = resource.adaptTo(Node.class);
        if (pageNode != null) {
            isMediaWrapperPage = false;
            checkForMediaWrapper(pageNode);
            if (isMediaWrapperPage) {
                mediaWrapperUpdateService.updateMediaWrapperProperties(pageNode, resolver);
            }
        }
    }

    /**
     * Check for media wrapper.
     *
     * @param pageNode
     *            the page node
     */
    private void checkForMediaWrapper(final Node pageNode) {
        try {
            if (isMediaWrapperPage) {
                return;
            }
            if (pageNode.getName().startsWith("object") && pageNode.hasProperty(PN_SLING_RESOURCE_TYPE)
                    && pageNode.getProperty(PN_SLING_RESOURCE_TYPE).getString()
                            .equals(MadisonConstants.MEDIA_WRAPPER_RESOURCE_TYPE)) {
                isMediaWrapperPage = true;
                return;
            }
            if (pageNode.hasNodes()) {
                final NodeIterator childNodes = pageNode.getNodes();
                while (childNodes.hasNext()) {
                    final Node childNode = childNodes.nextNode();
                    checkForMediaWrapper(childNode);
                }
            }
        } catch (final RepositoryException e) {
            log.error("An error ocurred while finding media wrapper node ", e);
        }
    }

    /**
     * This method Updates the Related Path to Respective Pages
     *
     * @param resource            resource
     * @param resolver            ResourceResolver
     * @param baseContentResource
     */
    private void updateRelatedPathsToPage(final Resource resource, final ResourceResolver resolver, Resource baseContentResource, PostProcessing postProcessing) {

        try {
            if (null == resource) {
                return;
            }
            final Page page = resource.adaptTo(Page.class);
            final Node pageNode = resource.adaptTo(Node.class);

            if (null == page) {
                return;
            }

            final ValueMap valueMap = page.getProperties();
            final String ditaPath = valueMap.containsKey(EFFECTIVE_SOURCE_PATH) ? valueMap.get(EFFECTIVE_SOURCE_PATH, String.class)
                    : StringUtils.EMPTY;

            if (StringUtils.isBlank(ditaPath) || !ditaPath.contains(DITA_EXTENSION)) {
                return;
            }

            final Resource ditaAssetRes = resolver.getResource(ditaPath);
            Node ditaAsset = null;

            if (null != ditaAssetRes) {
                ditaAsset = ditaAssetRes.adaptTo(Node.class);
            }

            updatePageMeta(pageNode, ditaAsset, resolver,baseContentResource);
            setDocSearchIdentifier(pageNode, ditaAsset);

        } catch (final RepositoryException e) {
            postProcessing.setFailure(true);
            postProcessing.appendException("Error while copying metadata to page: "+ resource.getPath() );
            postProcessing.appendLog(e);
            log.error("Error while copying metadata to page: {}", resource.getPath());
        }
    }

    private void updatePageMeta(final Node pageNode, Node ditaAsset, ResourceResolver resolver, Resource baseContentResource) throws RepositoryException {
        if (null == pageNode || !pageNode.hasNode(NameConstants.NN_CONTENT) || null == ditaAsset
                || !ditaAsset.hasNode(JCR_CONTENT_METADATA)) {
            return;

        }
        for (final String propertyName : ASSET_PN_LIST) {
            updatePageProperty(pageNode, ditaAsset, propertyName, resolver, baseContentResource);
        }
    }

    /**
     * Set Identifier for document search
     * @param pageNode
     * @param ditaAsset
     * @throws RepositoryException
     */
    private void setDocSearchIdentifier(final Node pageNode, Node ditaAsset) throws RepositoryException {
        if (null == pageNode || !pageNode.hasNode(NameConstants.NN_CONTENT) || null == ditaAsset
                || !ditaAsset.hasNode(JCR_CONTENT_METADATA)) {
            return;
        }
        if(ditaAsset.getNode(JCR_CONTENT_METADATA).hasProperty(DITAConstants.META_IS_DOC_CONTEXT_SEARCH)){
            if(MadisonConstants.YES.equals(ditaAsset.getNode(JCR_CONTENT_METADATA).getProperty(DITAConstants.META_IS_DOC_CONTEXT_SEARCH).getString())){
                if(!pageNode.getNode(NameConstants.NN_CONTENT).hasProperty(DITAConstants.META_DOC_CONTEXT_SEARCH_IDENTIFIER) && !ditaAsset.getNode(JCR_CONTENT_METADATA).hasProperty(DITAConstants.META_DOC_CONTEXT_SEARCH_IDENTIFIER)){
                    pageNode.getNode(NameConstants.NN_CONTENT).setProperty(DITAConstants.META_DOC_CONTEXT_SEARCH_IDENTIFIER, MadisonUtil.getRandomUUID());
                }
            }else if(!ditaAsset.getNode(JCR_CONTENT_METADATA).hasProperty(DITAConstants.META_DOC_CONTEXT_SEARCH_IDENTIFIER)){
                pageNode.getNode(NameConstants.NN_CONTENT).setProperty(DITAConstants.META_DOC_CONTEXT_SEARCH_IDENTIFIER, (Value) null);
            }
        }
    }


    
    /**
     * Fetch preset setting
     * @param resource
     * @param resolver
     */
    private void isOverrideSet(Resource resource,ResourceResolver resolver) {
        final Page page = resource.adaptTo(Page.class);
        if (null == page) {
            return;
        }
        final ValueMap valueMap = page.getProperties();
        final String ditaPath = valueMap.containsKey(EFFECTIVE_SOURCE_PATH) ? valueMap.get(EFFECTIVE_SOURCE_PATH, String.class)
                : StringUtils.EMPTY;
        log.debug("Fetching preset setting for {}",ditaPath);

        if (StringUtils.isBlank(ditaPath) || !StringUtils.endsWith(ditaPath, DITAConstants.DITAMAP_EXT)) {
            log.debug("Asset is not a ditamap {}",ditaPath);
            return;
        }
        final Resource ditaAssetRes = resolver.getResource(ditaPath);
        Node ditaAsset = null;
        if (null != ditaAssetRes) {
            ditaAsset = ditaAssetRes.adaptTo(Node.class);
        }
        String aemSiteOutputName = StringUtils.stripStart(DITAConstants.AEMSITE_PRESETS_NODE, "/");
        try {
            if(ditaAsset.hasNode(aemSiteOutputName)) {
                isOverride = ditaAsset.getNode(aemSiteOutputName).hasProperty("fmdita-overwriteFiles") ? StringUtils.equalsIgnoreCase("ReuseExisting", ditaAsset.getNode(aemSiteOutputName).getProperty("fmdita-overwriteFiles").getString()) :false;
            }
            log.debug("preset setting set for isOverride  {}",isOverride);
        } catch (RepositoryException e) {
            log.error("Error Processing the resouce for path {}",resource.getPath());
        }
    }

    /**
     * Method to update PDF download properties in for a given resource
     *
     * @param resource
     * @param resolver
     * @param generatedPath
     */
    private void updatePDFDownloadProperties(final Resource resource, final ResourceResolver resolver,
            final String generatedPath) {
        final Page page = resource.adaptTo(Page.class);
        final Node pageNode = resource.adaptTo(Node.class);
        if (page == null) {
            return;
        }
        final ValueMap valueMap = page.getProperties();
        if (!valueMap.containsKey(BASE_PATH)) {
            return;
        }
        String mapParent = valueMap.get(MAP_PARENT, String.class);
        if (null != mapParent) {
            mapParent = mapParent.replace(";", StringUtils.EMPTY);
        }
        final String basePath = valueMap.get(BASE_PATH, String.class);
        final Resource baseResource = resolver
                .getResource(basePath + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
        if (baseResource == null) {
            return;
        }
        final Node baseNode = baseResource.adaptTo(Node.class);
        String sourcePath;
        try {
            sourcePath = baseNode.hasProperty(EFFECTIVE_SOURCE_PATH) ? baseNode.getProperty(EFFECTIVE_SOURCE_PATH).getString()
                    : StringUtils.EMPTY;

            savePDFDownloadProperties(sourcePath, mapParent, baseNode, pageNode, generatedPath, resolver);
        } catch (final RepositoryException e) {
            log.error("Error updating pdf properties for the resouce {} ", resource.getPath(), e);
        }

    }

    /**
     * Method to retrieve and save the PDF download page path and PDF title in the page
     *
     * @param sourcePath
     * @param mapParent
     * @param baseNode
     * @param pageNode
     * @param generatedPath
     * @param resolver
     * @throws RepositoryException
     */
    private void savePDFDownloadProperties(final String sourcePath, final String mapParent, final Node baseNode,
            final Node pageNode, final String generatedPath, final ResourceResolver resolver)
            throws RepositoryException {
        final Node jcrNode = pageNode.getNode(NameConstants.NN_CONTENT);
        String downloadParentPath = StringUtils.EMPTY;
        String title = jcrNode.hasProperty(PROPERTY_SITE_TITLE) ? jcrNode.getProperty(PROPERTY_SITE_TITLE).getString()
                : StringUtils.EMPTY;
       //Only fetch entry node if map is book map or downaload is enabled
        if (DITAUtils.isBookMap(sourcePath, resolver) || DITAUtils.isDownloadEnabled(sourcePath, resolver)) {
            downloadParentPath = getDownloadPathFromToc(sourcePath, pageNode, baseNode, resolver);
        }
        if (DITAUtils.isBookMap(sourcePath, resolver)) {
            title = getPDFTitleForBookMap(downloadParentPath, title, resolver);
        }
        if (!DITAUtils.isBookMap(sourcePath, resolver) || isFullGuidePath(sourcePath)) {
            downloadParentPath = baseNode.getParent().getPath();
        }
        log.debug("downloadParentPath {}",downloadParentPath);
        if (StringUtils.isNotBlank(downloadParentPath)) {
            jcrNode.setProperty(DITAConstants.PROPERTY_DOWNLOAD_PDF_PATH, StringUtils.substringBefore(downloadParentPath, "#"));
        }
        if (StringUtils.isNotBlank(title)) {
            jcrNode.setProperty(DITAConstants.PROPERTY_DOWNLOAD_PDF_TITLE, title);
        }
    }

    /**
     * Method to return if the map is enabled for full guide download irrespective of map type
     * @param sourcePath
     * @return boolean
     */
    private boolean isFullGuidePath(String sourcePath){
        String[] fullGuidePaths = downloadPDFConfigurationService.getFullGuidePaths();
        for(String fullGuidePath : fullGuidePaths){
            if(sourcePath.contains(fullGuidePath)){
                return true;
            }
        }
        return false;
    }
    

    /**
     * Method to form the PDF title for a bookmap by including the concatinating the topic title and chapter title
     *
     * @param chapterPath
     * @param title
     * @param resolver
     * @return
     */
    private String getPDFTitleForBookMap(final String chapterPath, final String title,
            final ResourceResolver resolver) {
        final Resource chapterResource = resolver
                .getResource(chapterPath + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
        String updatedTitle = title;
        if (chapterResource != null) {
            final Node chapterNode = chapterResource.adaptTo(Node.class);
            try {
                updatedTitle = title.trim() + " - "
                        + (chapterNode.hasProperty(DITAConstants.PN_PAGE_TITLE)
                        ? chapterNode.getProperty(DITAConstants.PN_PAGE_TITLE).getString()
                        : chapterNode.getName());
            } catch (final RepositoryException e) {
                log.error("Error in forming the PDF title for a bookmap", e);
            }
        }
        return updatedTitle;
    }

    /**
     * Method to get the chapter path from the toc structure
     * @param sourcePath 
     *
     * @param pageNode
     * @param baseNode
     * @param resolver
     * @return String
     * @throws RepositoryException
     */
    private String getDownloadPathFromToc(String sourcePath, final Node pageNode, final Node baseNode, final ResourceResolver resolver)
            throws RepositoryException {
        String downloadPath = StringUtils.EMPTY;
        final long startTime = System.currentTimeMillis();
        if (!baseNode.hasNode(TOC_NODE)) {
            return downloadPath;
        }
        final Node tocNode = baseNode.getNode(TOC_NODE);
        final List<String> entryList = new ArrayList<>();
        final NodeIterator entries = tocNode.getNodes();
        while (entries.hasNext()) {
            final Node entry = entries.nextNode();
            entryList.add(entry.getPath());
        }
        final String query = getQueryStatement(resolver, pageNode, tocNode);
        if(StringUtils.isEmpty(query)) {
            log.debug("Empty query statement");
            return downloadPath;
        }
        final Iterator<Resource> result = resolver.findResources(query, javax.jcr.query.Query.JCR_SQL2);
        if (result.hasNext()) {
            final Node entryNode = result.next().adaptTo(Node.class);
            stampHideToc(sourcePath,pageNode,entryNode,resolver);
            final long endTime = System.currentTimeMillis();
            log.debug("stampHideToc took {} seconds to complete the process", (endTime - startTime) / 1000);
            //Only for bookmap
            if(DITAUtils.isBookMap(sourcePath, resolver) && null != entryNode) {
                downloadPath = getPathFromParentToc(entryList, entryNode).replace(MadisonConstants.HTML_EXTN,
                        StringUtils.EMPTY);
            }
        }

        return downloadPath;
    }
    
    /**
     * Stamp hide toc.
     *
     * @param pageNode the page node
     * @param entryNode the entry node
     * @throws RepositoryException the repository exception
     */
    private void stampHideToc(String sourcePath,Node pageNode,Node entryNode,ResourceResolver resolver) throws RepositoryException {
        if(DITAUtils.isDownloadEnabled(sourcePath, resolver)) {
            boolean isTocHidden = DITAUtils.isTOCHidden(entryNode);
            log.debug("TOC hidden {}",isTocHidden);
            if(isTocHidden && null != pageNode.getNode(NameConstants.NN_CONTENT)) {
                log.debug("Stamping showToc as no for pageNode {}",pageNode.getPath());
                pageNode.getNode(NameConstants.NN_CONTENT).setProperty(PROPERTY_SHOW_TOC, "no");
            }
        }
        else {
            log.debug("Download disabled for {}",sourcePath);
        }
    }
    
    /**
     * Gets the query statement.
     *
     * @param sourcePath the source path
     * @param resolver the resolver
     * @param pageNode the page node
     * @param tocNode the toc node
     * @return the query statement
     * @throws RepositoryException the repository exception
     */
    private String getQueryStatement(ResourceResolver resolver,Node pageNode,Node tocNode) throws RepositoryException {
        String  query = getQueryString(pageNode, tocNode,resolver);
        log.debug("query statement for fetching toc entry {}",query);
        return query;
    }

    /**
     * Gets the query string.
     *
     * @param pageNode the page node
     * @param tocNode the toc node
     * @return the query string
     * @throws RepositoryException the repository exception
     */
    private String getQueryString(Node pageNode, Node tocNode,ResourceResolver resolver) throws RepositoryException {
        String query;
        if(null != pageNode && !DITAUtils.isChunked(pageNode.getNode(NameConstants.NN_CONTENT))) {
            query = "SELECT * FROM [nt:base] AS entry WHERE ISDESCENDANTNODE(entry , '" + tocNode.getPath()
            + "') AND entry.[link] = '" + pageNode.getPath() + MadisonConstants.HTML_EXTN + "'";
        }
        else {
            String path = getChunkedPath(pageNode,resolver);
            log.debug("Chunked link path {}",path);
            query = StringUtils.isNotEmpty(path) ? "SELECT * FROM [nt:base] AS entry WHERE ISDESCENDANTNODE(entry , '" + tocNode.getPath()
            + "') AND entry.[link] = '" + path+ "'" : StringUtils.EMPTY;
        }
        return query;
    }
    
    /**
     * Gets the chunked path.
     *
     * @param pageNode the page node
     * @param resolver the resolver
     * @return the chunked path
     * @throws RepositoryException the repository exception
     */
    private String getChunkedPath(Node pageNode,ResourceResolver resolver) throws RepositoryException {
        if(null != pageNode && null != pageNode.getNode(NameConstants.NN_CONTENT) && DITAUtils.isChunked(pageNode.getNode(NameConstants.NN_CONTENT))) {
            Node jcrNode = pageNode.getNode(NameConstants.NN_CONTENT);
            Node ditaNode = jcrNode.getNode("root/maincontainer/readerrow/bodycontainer/docreader/contentbody/topicbody");
            if(null != ditaNode) {
                final Node topicNode = DITAUtils.getTopicNode(ditaNode, resolver);
                    if(null != topicNode && topicNode.hasProperty(PROPERTY_ID)) {
                        String idVal = StringUtils.contains(topicNode.getProperty(PROPERTY_ID).getString(), "#") ? topicNode.getProperty(PROPERTY_ID).getString() : "#"+topicNode.getProperty(PROPERTY_ID).getString();
                        log.debug("Found topic node {} with id {}",topicNode.getPath(),idVal);
                        return pageNode.getPath() + MadisonConstants.HTML_EXTN +idVal;
                }
            }
        }
        return StringUtils.EMPTY;
    }

    /**
     * Method to recursively iterate the toc structure to find the chapter path
     *
     * @param entryList
     * @param entryNode
     * @return String
     * @throws RepositoryException
     */
    private String getPathFromParentToc(final List<String> entryList, final Node entryNode) throws RepositoryException {
        String linkPath;
        if (entryList.contains(entryNode.getPath())) {
            linkPath = entryNode.hasProperty(MadisonConstants.LINK)
                    ? entryNode.getProperty(MadisonConstants.LINK).getString()
                    : StringUtils.EMPTY;
        } else {
            linkPath = getPathFromParentToc(entryList, entryNode.getParent());
        }
        return linkPath;
    }

    /**
     * Update Each property from Dita Asset and Update on Page Property.
     *
     * @param pageNode            pageNode
     * @param ditaAsset           ditaAsset
     * @param propertyName        propertyName
     * @param resolver
     * @param baseContentResource
     * @throws RepositoryException RepositoryException
     */
    private void updatePageProperty(final Node pageNode, final Node ditaAsset, final String propertyName, ResourceResolver resolver, Resource baseContentResource)
            throws RepositoryException {
        if(isOverride && pageNode.getNode(NameConstants.NN_CONTENT).hasProperty(propertyName)) {
            log.debug("Removed {} for page/node: {}",propertyName,
                    pageNode.getNode(NameConstants.NN_CONTENT).getPath());
            pageNode.getNode(NameConstants.NN_CONTENT).setProperty(propertyName, (Value)null);
        }
        if (propertyName.equals(DITAConstants.META_COPYRIGHT)) {
            updateCopyRightText(pageNode, ditaAsset);
            return;
        }
        if (ditaAsset.getNode(JCR_CONTENT_METADATA).hasProperty(propertyName)) {
                if (ditaAsset.getNode(JCR_CONTENT_METADATA).getProperty(propertyName).isMultiple()) {
                    if(META_PWC_SEE_ALSO_DEFAULT_USED_IN_REFS.equals(propertyName)){
                        setSeeAlsoReferencesForAutoSetReference(pageNode, ditaAsset, propertyName, resolver, baseContentResource);
                    } else {
                        setPagePropertyForMultiValueProp(pageNode, ditaAsset, propertyName);
                    }
                } else {
                    final Value value = ditaAsset.getNode(JCR_CONTENT_METADATA).getProperty(propertyName).getValue();
                    pageNode.getNode(NameConstants.NN_CONTENT).setProperty(propertyName, value);
                }

                // The below execution will done only for SME property update for the SME-List component on the Page.
                if (DITAConstants.PN_SME.equals(propertyName)) {
                    updateSMEComponentOnPage(pageNode, ditaAsset);
                }

                // The below execution will done only for Templates property update for the Templates and tools list
                // component on the Page.
                if ((DITAConstants.PN_TEMPLATE_AND_TOOLS.equals(propertyName)
                        || DITAConstants.PN_RELATED_CONTENT.equals(propertyName))
                        && pageNode.hasNode(DITAConstants.RIGHT_RAIL_NODE_PATH_V1)) {
                    final Node relatedNode = pageNode.getNode(DITAConstants.RIGHT_RAIL_NODE_PATH_V1);
                    updateRelatedPageComponentOnPage(relatedNode, ditaAsset);
                }
                log.debug("Update Related Content Properties for page/node: {}",
                        pageNode.getNode(NameConstants.NN_CONTENT).getPath());

        }
    }

    /**
     * @param pageNode
     * @param ditaAsset
     * @param propertyName
     * @param resolver
     * @param baseContentResource
     * @throws RepositoryException
     */
    private static void setSeeAlsoReferencesForAutoSetReference(final Node pageNode, final Node ditaAsset, final String propertyName, ResourceResolver resolver, Resource baseContentResource) throws RepositoryException {
        final String[] allowedContentTypes = baseContentResource.adaptTo(ValueMap.class).get(META_PWC_SEE_ALSO_CONTENT_TYPE, String[].class);
        final Value[] references = ditaAsset.getNode(JCR_CONTENT_METADATA).getProperty(META_PWC_SEE_ALSO_DEFAULT_USED_IN_REFS).getValues();
        List<Value> finalReferences = new ArrayList<>();
        if (allowedContentTypes == null || allowedContentTypes.length < 1) {
            setPagePropertyForMultiValueProp(pageNode, ditaAsset, propertyName);
            return;
        }
        for (Value reference : references) {
            Resource resource = resolver.getResource(reference.getString());
            if (resource != null) {
                Node node = resource.adaptTo(Node.class);
                if (node.hasNode(JCR_CONTENT_METADATA)) {
                    Node metadataNode = node.getNode(JCR_CONTENT_METADATA);
                    if (metadataNode.hasProperty(META_CONTENT_TYPE)) {
                        String contentType = metadataNode.getProperty(META_CONTENT_TYPE).getValue().toString();
                        if (ArrayUtils.contains(allowedContentTypes, contentType)) {
                            finalReferences.add(reference);
                        }
                    }
                }
            }
        }
        pageNode.getNode(NameConstants.NN_CONTENT).setProperty(propertyName, finalReferences.toArray(new Value[0]));
    }

    /**
     * @param pageNode
     * @param ditaAsset
     * @param propertyName
     * @throws RepositoryException
     */
    private static void setPagePropertyForMultiValueProp(final Node pageNode, final Node ditaAsset, final String propertyName) throws RepositoryException {
        final Value[] values = ditaAsset.getNode(JCR_CONTENT_METADATA).getProperty(propertyName).getValues();
        pageNode.getNode(NameConstants.NN_CONTENT).setProperty(propertyName, values);
    }

    /**
     * Update copy right text.
     *
     * @param pageNode
     *            the page node
     * @param ditaAsset
     *            the dita asset
     * @throws RepositoryException
     *             the repository exception
     */
    private void updateCopyRightText(final Node pageNode, final Node ditaAsset) throws RepositoryException {
            Value[] copyrightValues = null;
            if (ditaAsset.getNode(JCR_CONTENT_METADATA).hasProperty(DITAConstants.META_COPYRIGHT)) {
                copyrightValues = ditaAsset.getNode(JCR_CONTENT_METADATA).getProperty(DITAConstants.META_COPYRIGHT)
                        .getValues();
            } else {
                copyrightValues = getCopyRightFromFolderMetadata(ditaAsset);
            }
            if (copyrightValues.length != 0) {
                pageNode.getNode(NameConstants.NN_CONTENT).setProperty(DITAConstants.META_COPYRIGHT, copyrightValues);
            }
            log.debug("Update Related Content Properties for page/node: {}",
                    pageNode.getNode(NameConstants.NN_CONTENT).getPath());

    }

    /**
     * Gets the copy right from folder metadata.
     *
     * @param assetNode
     *            the asset node
     * @return the copy right from folder metadata
     * @throws RepositoryException
     *             the repository exception
     */
    private Value[] getCopyRightFromFolderMetadata(final Node assetNode) throws RepositoryException {
        if (assetNode == null) {
            return new Value[0];
        }
        if (assetNode.getName().equals("dam")) {
            return new Value[0];
        }
        if ((assetNode.isNodeType(JcrConstants.NT_FOLDER) || assetNode.isNodeType(JcrResourceConstants.NT_SLING_FOLDER)
                || assetNode.isNodeType(JcrResourceConstants.NT_SLING_ORDERED_FOLDER))
                && assetNode.hasNode(JCR_CONTENT_METADATA)) {
            if (assetNode.getNode(JCR_CONTENT_METADATA).hasProperty(DITAConstants.META_COPYRIGHT)) {
                return assetNode.getNode(JCR_CONTENT_METADATA).getProperty(DITAConstants.META_COPYRIGHT).getValues();
            }
        }
        return getCopyRightFromFolderMetadata(assetNode.getParent());
    }

    /**
     * This Method is to Update the SME List component on page
     *
     * @param rootNode
     *            rootNode
     * @param ditaAsset
     *            ditaAsset
     * @throws RepositoryException
     *             RepositoryException
     */
    private void updateSMEComponentOnPage(final Node rootNode, final Node ditaAsset) throws RepositoryException {
        final NodeIterator linksIterator = rootNode.getNodes();
        while (linksIterator.hasNext()) {
            final Node childNode = linksIterator.nextNode();
            if (null != childNode && childNode.hasProperty(PN_SLING_RESOURCE_TYPE)
                    && SME_RESOURCE_PATH.equals(childNode.getProperty(PN_SLING_RESOURCE_TYPE).getString())) {
                childNode.setProperty(PN_LIST_FROM, STATIC);
                setRelatedContentProperty(ditaAsset, childNode, DITAConstants.PN_SME);
            }
            if (null != childNode && null != childNode.getNodes() && childNode.getNodes().getSize() > 0) {
                updateSMEComponentOnPage(childNode, ditaAsset);
            }
        }
    }

    /**
     * This Method is to Update the Related content, Template and Tools component on page
     *
     * @param rootNode
     *            rootNode
     * @param ditaAsset
     *            ditaAsset
     * @throws RepositoryException
     *             RepositoryException
     */
    private void updateRelatedPageComponentOnPage(final Node rootNode, final Node ditaAsset)
            throws RepositoryException {
        final NodeIterator linksIterator = rootNode.getNodes();
        while (linksIterator.hasNext()) {
            final Node childNode = linksIterator.nextNode();
            if (null != childNode && childNode.hasProperty(PN_SLING_RESOURCE_TYPE) && RELATED_PAGE_LIST_RESOURCE_PATH
                    .equals(childNode.getProperty(PN_SLING_RESOURCE_TYPE).getString())) {
                childNode.setProperty(PN_LIST_FROM, STATIC);

                if (childNode.hasProperty(PN_ITEM_TYPE)
                        && childNode.getProperty(PN_ITEM_TYPE).getString().equals(PN_ITEM_TYPE_TEMPLATE_PAGES)) {
                    // Set Template Pages to Related Pages List component
                    setRelatedContentProperty(ditaAsset, childNode, DITAConstants.PN_TEMPLATE_AND_TOOLS);
                } else if (childNode.hasProperty(PN_ITEM_TYPE)
                        && childNode.getProperty(PN_ITEM_TYPE).getString().equals(PN_ITEM_TYPE_RELATED_CONTENT_PAGE)) {
                    // Set Related Content Pages to Related Pages List component
                    setRelatedContentProperty(ditaAsset, childNode, DITAConstants.PN_RELATED_CONTENT);
                }
            }
            if (null != childNode && null != childNode.getNodes() && childNode.getNodes().getSize() > 0) {
                updateRelatedPageComponentOnPage(childNode, ditaAsset);
            }
        }
    }

    /**
     * This Method is to set property for related content component.
     *
     * @param ditaAsset
     *            ditaAsset
     * @param childNode
     *            childNode
     * @param relatedPropertyName
     *            relatedPropertyName
     * @throws RepositoryException
     *             RepositoryException
     */
    private void setRelatedContentProperty(final Node ditaAsset, final Node childNode, final String relatedPropertyName)
            throws RepositoryException {

        if (!ditaAsset.getNode(JCR_CONTENT_METADATA).hasProperty(relatedPropertyName)) {
            return;
        }
        if(childNode.hasProperty(PN_PAGES)){
            /*Remove the existing property before copying to avoid ValueFormatException*/
            log.debug("Removed {} for page/node: {}",PN_PAGES,childNode);
            childNode.setProperty(PN_PAGES, (Value)null);
        }
        if (ditaAsset.getNode(JCR_CONTENT_METADATA).getProperty(relatedPropertyName).isMultiple()) {
            final Value[] values = ditaAsset.getNode(JCR_CONTENT_METADATA).getProperty(relatedPropertyName).getValues();
            childNode.setProperty(PN_PAGES, values);
        } else {
            final Value value = ditaAsset.getNode(JCR_CONTENT_METADATA).getProperty(relatedPropertyName).getValue();
            childNode.setProperty(PN_PAGES, value);
        }
    }

    /**
     * Private method to return the Search predicate map
     *
     * @param outputPath
     *            pathOfOutputPage
     * @return predicateMap
     */
    private Map<String, Object> getPredicateMap(final String outputPath) {

        final Map<String, Object> predicateMap = new HashMap<>();
        predicateMap.put("p.limit", "-1");
        predicateMap.put("path", outputPath);
        predicateMap.put(PN_TYPE, "cq:Page");

        return predicateMap;
    }

    /**
     * Check if the generation is for preview-site
     * @param outputHistoryPath
     * @param resolver
     * @return
     */
    private boolean isPreview(String outputHistoryPath, ResourceResolver resolver){
        if(StringUtils.isBlank(outputHistoryPath) || null == resolver){
            return  false;
        }
        boolean preview = false;
        Resource historyNode = resolver.getResource(outputHistoryPath);
        if(null == historyNode){
            return  false;
        }
        if(historyNode.getValueMap().containsKey(OUTPUT_SETTING)){
            String outputSetting = historyNode.getValueMap().get(OUTPUT_SETTING, String.class);
            if(outputSetting.equalsIgnoreCase(PREVIEW_SITE)){
                preview = true;
            }
        }
        return preview;
    }

}
