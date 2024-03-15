package com.pwc.madison.core.models.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.constants.WorkflowConstants;
import com.pwc.madison.core.models.DitaMapDetails;
import com.pwc.madison.core.models.PublishingPointsModel;
import com.pwc.madison.core.services.VerifyChunkService;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = PublishingPointsModel.class)
public class PublishingPointsModelImpl implements PublishingPointsModel {
    private static final Logger LOG = LoggerFactory.getLogger(PublishingPointsModelImpl.class);
    private Set<String> visibleTopicList = Collections.emptySet();
    private List<Resource> topicList = Collections.emptyList();
    private Map<String, String> pubPointsMap = Collections.EMPTY_MAP;

    Map<String, Set<String>> ditaMapList = Collections.EMPTY_MAP;
    @OSGiService
    private ResourceResolverFactory resolverFactory;

    @OSGiService
    private VerifyChunkService verifyChunkService;

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private WorkflowService workflowService;

    @OSGiService
    private QueryBuilder queryBuilder;

    @OSGiService
    private XSSAPI xssAPI;

    @Override
    public List<Resource> getTopicList() {
        return topicList;
    }

    @Override
    public Map<String, String> getPublishingPoints() {
        return pubPointsMap;
    }

    /**
     * init Method of Model.
     */
    @PostConstruct
    protected void init() {
        String workId = StringUtils.EMPTY;
        visibleTopicList = new HashSet<>();
        topicList = new ArrayList<>();
        pubPointsMap = new HashMap<String, String>();
        String ditamapPath = StringUtils.EMPTY;
        ResourceResolver resolver = MadisonUtil
                .getResourceResolver(resolverFactory, MadisonConstants.MADISON_GENERIC_SUB_SERVICE);

        Session session = resolver.adaptTo(Session.class);
        RequestParameterMap requestParameterMap = request.getRequestParameterMap();
        if (requestParameterMap.containsKey(DITAConstants.DITAMAP)) {
            ditamapPath = requestParameterMap.getValue(DITAConstants.DITAMAP).getString();
        }

        if (!ditamapPath.isEmpty()) {
            pubPointsMap = fetchPublishingPoints(ditamapPath, resolver);
        }

        if (requestParameterMap.containsKey(WorkflowConstants.WORKFLOW_ID)) {
            workId = request.getRequestParameter(WorkflowConstants.WORKFLOW_ID).getString();
        }

        WorkflowSession wfSession = workflowService.getWorkflowSession(session);
        try {
            Workflow workflow = wfSession.getWorkflow(workId);
            boolean isFullSiteGeneration = false;
            if (null != workflow) {
                MetaDataMap workflowData = workflow.getWorkflowData().getMetaDataMap();
                String strChangesDitamaps = workflowData.get(DITAConstants.REVIEW_DITAMAPS,String.class);
                if(null!=strChangesDitamaps && !strChangesDitamaps.isEmpty()){
                    String[] ditamapArray = strChangesDitamaps.split("|");
                    if(ditamapArray.length>0){
                        isFullSiteGeneration = true;
                    }
                }
                String topics = workflowData.get(DITAConstants.SELECTED_TOPICS, String.class);;
                if(isFullSiteGeneration){
                    topics = workflowData.get(DITAConstants.ORIGINAL_TOPICS, String.class);
                }

                if (null != topics && !topics.isEmpty()) {
                    String[] selectedTopicsList = topics.split("\\|");
                    getTopics(selectedTopicsList, resolver);
                    visibleTopicList = verifyChunkService.getChunkedRoot(selectedTopicsList, request, resolver, ditamapPath);
                    LOG.debug("visibleTopicList size: {}", visibleTopicList.size());
                    ditaMapList = getFinalMaps(visibleTopicList, pubPointsMap, resolver);
                }
            }
        } catch (WorkflowException e) {
            LOG.error(e.getMessage());
        } finally {
            if (null != resolver && resolver.isLive()) {
                resolver.close();
            }
        }
        LOG.debug("TopicOutputGenerationModelImpl: workId is {}", xssAPI.encodeForHTML(workId));    
    }

    /**
     * Fetches all the publishing points having reference to the given ditamap
     *
     * @param ditaMapPath
     * @return
     */
    private Map<String, String> fetchPublishingPoints(String ditaMapPath, ResourceResolver resourceResolver) {
        Map<String, String> pubPointsMap = Collections.EMPTY_MAP;
        Resource ditamapRes = resourceResolver.getResource(ditaMapPath);
        if (null != ditamapRes) {
            pubPointsMap = new HashMap<String, String>();
            populatePublishingPointsMap(ditamapRes, resourceResolver, pubPointsMap);
            Map<String, Object> map = MadisonUtil.createDitaMapQuery(ditaMapPath);
            final Query query = queryBuilder
                    .createQuery(PredicateGroup.create(map), resourceResolver.adaptTo(Session.class));
            final SearchResult ditaMapPaths = query.getResult();
            if (null != ditaMapPaths) {
                final Iterator<Resource> pathResources = ditaMapPaths.getResources();
                while (pathResources.hasNext()) {
                    Resource resource = pathResources.next();
                    populatePublishingPointsMap(resource, resourceResolver, pubPointsMap);
                }
            }
        }
        return pubPointsMap;
    }

    private String getLastPublishedPath(Resource presetRes) {
        String path = StringUtils.EMPTY;
        if (null != presetRes) {
            ValueMap presetValueMap = presetRes.getValueMap();
            if (presetValueMap.containsKey(DITAConstants.PN_LAST_PUBLISHED_PATH)) {
                path = presetValueMap.get(DITAConstants.PN_LAST_PUBLISHED_PATH, String.class);
            }
        }
        return path;
    }

    private void populatePublishingPointsMap(Resource resource, ResourceResolver resourceResolver,
            Map<String, String> pubPointsMap) {
        Resource metadataRes = resourceResolver.getResource(resource.getPath() + MadisonConstants.METADATA_PATH);
        Resource presetRes = resourceResolver.getResource(resource.getPath() + DITAConstants.AEMSITE_PRESETS_NODE);
        ValueMap valueMap = metadataRes.getValueMap();
        if (valueMap.containsKey(DITAConstants.PN_IS_PUBLISHING_POINTS)) {
            String isPubPoint = valueMap.get(DITAConstants.PN_IS_PUBLISHING_POINTS, String.class);
            if (isPubPoint.equals("yes")) {
                String lastPublishedPath = getLastPublishedPath(presetRes);
                pubPointsMap.put(resource.getPath(), lastPublishedPath);
            }
        }
    }

    /**
     * Iterates through publishing points and populate mapList for each
     * @param topics
     * @param pubPointsMap
     * @param resolver
     * @return mapList
     */
    private Map<String, Set<String>> getFinalMaps(Set<String> topics, Map<String, String> pubPointsMap, ResourceResolver resolver){
        LOG.debug("Inside getFinalMaps");
        final long startTime = System.currentTimeMillis();
        Map<String, Set<String>> mapList = new HashMap<>();
        if(null == topics || null == pubPointsMap || null == resolver){
            return  mapList;
        }
        for (Map.Entry entry : pubPointsMap.entrySet()) {
            Set<String> subMapList = new HashSet<>();
            String publishingPoint = entry.getKey().toString();
            List<DitaMapDetails> mapReferences = DITAUtils.getDitaMapsRefs(publishingPoint, resolver,null);
            LOG.debug("Map reference list size for map: {} is:: {}", publishingPoint, mapReferences.size());
            for (String topic: topics) {
                getMaps(topic, mapReferences, publishingPoint, resolver, subMapList, topics);
            }
            mapList.put(publishingPoint, subMapList);
        }
        final long endTime = System.currentTimeMillis();
        LOG.debug("getFinalMaps took {} seconds to complete the process", (endTime - startTime) / 1000);
        return mapList;
    }

    /**
     * Iterates till publishing point and returns all the ditamaps between publishing-point and topic
     * @param path
     * @param subMapList
     * @param publishingPointMap
     * @param resolver
     * @param intermediateMapSet
     * @return mapSet
     */
    private String getMaps(String path, List<DitaMapDetails> subMapList, String publishingPointMap, ResourceResolver resolver, Set<String>intermediateMapSet, Set<String> topics){
        LOG.debug("inside getMaps");
        if(null == intermediateMapSet){
            intermediateMapSet = new HashSet<>();
        }
        List<String> refs = DITAUtils.getMapReference(path, resolver);
        LOG.debug("Map reference list size for topic: {} is:: {}", path, refs.size());
        for (String reference : refs) {
            if (reference.equals(publishingPointMap)){
                return  "";
            }
            for (DitaMapDetails mapDetail : subMapList) {
                if (reference.equals(mapDetail.getDitaMapPath()) && !topics.contains(reference)) {
                    intermediateMapSet.add(reference);
                }
            }
            getMaps(reference, subMapList, publishingPointMap, resolver, intermediateMapSet, topics);
        }
        return "";
    }

    /**
     * Fetches the list of topics from workflow with required info
     * @param topicsArray
     * @param resourceResolver
     */
    private void getTopics(String[] topicsArray, ResourceResolver resourceResolver) {
        topicList = new ArrayList<Resource>();
        if (topicsArray.length > 0) {
            for (String topic : topicsArray) {
                Resource resource = resourceResolver.getResource(topic);
                if (null != resource) {
                    topicList.add(resource);
                }
            }
        }
    }

    public Map<String, Set<String>> getDitaMapList() {
        return ditaMapList;
    }

    public Set<String> getVisibleTopicList() {
        return visibleTopicList;
    }
}

