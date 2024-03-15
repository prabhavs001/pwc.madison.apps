package com.pwc.madison.core.models.impl;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.constants.WorkflowConstants;
import com.pwc.madison.core.models.DITAGeneratedOutputBean;
import com.pwc.madison.core.models.TopicOutputGenerationModel;
import com.pwc.madison.core.services.RegenerateTopicsService;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Model(adaptables = SlingHttpServletRequest.class, adapters = TopicOutputGenerationModel.class, resourceType = BasePageModelImpl.RESOURCE_TYPE)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class TopicOutputGenerationModelImpl implements TopicOutputGenerationModel {
    private static final Logger LOG = LoggerFactory.getLogger(TopicOutputGenerationModelImpl.class);
    private static String FOLDER_NAME = "<folderName>";
    private List<Resource> topicList = Collections.emptyList();
    private List<DITAGeneratedOutputBean> pageList = Collections.EMPTY_LIST;
    private String ditaMap = StringUtils.EMPTY;
    @OSGiService private ResourceResolverFactory resolverFactory;

    @Self private SlingHttpServletRequest request;

    @OSGiService private WorkflowService workflowService;

    @OSGiService private RegenerateTopicsService regenerateTopicsService;

    @OSGiService private XSSAPI xssAPI;
    
    @Override public List<Resource> getTopicList() {
        return topicList;
    }

    @Override public List<DITAGeneratedOutputBean> getPageList() {
        return pageList;
    }

    @Override public String getDitaMap() {
        return ditaMap;
    }

    /**
     * init Method of Model.
     */
    @PostConstruct protected void init() {
        topicList = new ArrayList<Resource>();
        ResourceResolver resolver = MadisonUtil
                .getResourceResolver(resolverFactory, MadisonConstants.MADISON_GENERIC_SUB_SERVICE);

        Session session = resolver.adaptTo(Session.class);
        String workId = request.getRequestParameter(WorkflowConstants.WORKFLOW_ID).getString();
        WorkflowSession wfSession = workflowService.getWorkflowSession(session);
        try {
            Workflow workflow = wfSession.getWorkflow(workId);
            if (null != workflow) {
                MetaDataMap workflowData = workflow.getWorkflowData().getMetaDataMap();
                String orgTopics = workflowData.get(DITAConstants.ORIGINAL_TOPICS, String.class);
                if (null != orgTopics && !orgTopics.isEmpty()) {
                    String[] orgTopicsList = orgTopics.split("\\|");
                    if (null != request.getRequestParameter(MadisonConstants.AEM_PAGES)) {
                        fetchGeneratedAemPages(orgTopicsList, resolver);
                    } else {
                        getTopics(orgTopicsList, resolver);
                        retrieveRootDITAMAP(orgTopicsList[0], resolver);
                    }
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
     * Fetches the list of topics from workflow with required info
     *
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

    /**
     * Gets the root ditamap for the topic in context of which regeneration is triggered
     *
     * @param path
     * @param resourceResolver
     */
    private void retrieveRootDITAMAP(String path, ResourceResolver resourceResolver) {
        int matchScore = 0,i=0;
        Map<String, String> configValues = regenerateTopicsService.getRootDitaMaps();
        for (Map.Entry<String, String> entry : configValues.entrySet()) {
            String folderNameRegx = MadisonConstants.PWC_DAM_GENERAL_REGEX.replace(FOLDER_NAME, entry.getKey());
            if (path.matches(folderNameRegx)) {
                Pattern pattern = Pattern.compile(MadisonConstants.PWC__DAM_TERRITORY_FOLDER_REGEX.replace(FOLDER_NAME,entry.getKey()));
                Matcher matcher = pattern.matcher(path);
                while (matcher.find()) {
                    int level = getHierarchyLevel(matcher.group(),path,resourceResolver);
                    if(i==0){
                        matchScore = level;
                    }else if(level<matchScore){
                        matchScore = level;
                    }else{
                        i++;
                        continue;
                    }
                    String ditaMapPath = matcher.group() + "/" + entry.getValue();
                    Resource ditaMapResource = resourceResolver.getResource(ditaMapPath);
                    if (null != ditaMapResource) {
                        ditaMap = ditaMapPath;
                    }
                    i++;
                }
            }
        }
    }

    /**
     * Gets the hierarchy level of folder wrt topic
     * @param folderPath
     * @param topicPath
     * @param resolver
     * @return
     */
    private int getHierarchyLevel(String folderPath, String topicPath, ResourceResolver resolver){
        Resource childResource = resolver.getResource(topicPath);
        Resource parent = childResource.getParent();
        int level = 0;
        while(!parent.getPath().equals(MadisonConstants.MADISON_BASE_CONTENT_HIERARCHY) && !parent.getPath().equals(folderPath)){
            parent = parent.getParent();
            ++level;
        }
        return level;
    }

    /**
     * Fetches the aem pages generated out of the ditatopics
     *
     * @param topicsList
     * @param resourceResolver
     */
    private void fetchGeneratedAemPages(String[] topicsList, ResourceResolver resourceResolver) {
        pageList = new ArrayList<DITAGeneratedOutputBean>();
        if (topicsList.length > 0) {
            for (String topic : topicsList) {
                DITAGeneratedOutputBean generatedPage = new DITAGeneratedOutputBean();
                String pagePath = DITAUtils.getPageFromXrefDita(topic, resourceResolver, xssAPI);
                generatedPage.setPagePath(pagePath);
                Resource pageRes = resourceResolver.getResource(pagePath);
                //Reference API may return dita file path when the output generation failed
                if (pageRes.getResourceType().equals(NameConstants.NT_PAGE)) {
                    generatedPage.setCqPage(true);
                } else {
                    generatedPage.setCqPage(false);
                }
                pageList.add(generatedPage);
            }
        }
    }
}
