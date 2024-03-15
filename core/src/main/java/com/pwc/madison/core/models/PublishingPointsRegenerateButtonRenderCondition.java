package com.pwc.madison.core.models;

import javax.annotation.PostConstruct;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.constants.WorkflowConstants;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Render condition to determine whether the "Generate / Regenerate" button should render in publishing console of fullcycle and simple workflows
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class PublishingPointsRegenerateButtonRenderCondition {

    public static final Logger LOGGER = LoggerFactory.getLogger(PublishingPointsRegenerateButtonRenderCondition.class);

    @Self
    private SlingHttpServletRequest request;

    private ResourceResolver requestResolver;

    private ResourceResolver madisonServiceUserResolver;

    private boolean isRendered = true;

    @OSGiService
    private ResourceResolverFactory resolverFactory;

    @OSGiService
    private WorkflowService workflowService;

    @PostConstruct
    protected void init() {
        String workId = StringUtils.EMPTY;

        // get the service resource resolver for reading the users under /home etc
        madisonServiceUserResolver = MadisonUtil
                .getResourceResolver(resolverFactory, MadisonConstants.MADISON_GENERIC_SUB_SERVICE);

        Session session = madisonServiceUserResolver.adaptTo(Session.class);

        RequestParameterMap requestParameterMap = request.getRequestParameterMap();

        if (requestParameterMap.containsKey(WorkflowConstants.WORKFLOW_ID)) {
            workId = request.getRequestParameter(WorkflowConstants.WORKFLOW_ID).getString();
        }

        WorkflowSession wfSession = workflowService.getWorkflowSession(session);
        try {
            Workflow workflow = wfSession.getWorkflow(workId);
            if (null != workflow) {
                MetaDataMap workflowData = workflow.getWorkflowData().getMetaDataMap();
                String strChangesDitamaps = workflowData.get(DITAConstants.REVIEW_DITAMAPS,String.class);
                if(null!=strChangesDitamaps && !strChangesDitamaps.isEmpty()){
                    String[] ditamapArray = strChangesDitamaps.split("|");
                    if(ditamapArray.length>0){
                        isRendered = false;
                    }
                }
            }
        } catch (WorkflowException e) {
            LOGGER.error("WorkflowException in init of PublishingPointsRegenerateButtonRenderCondition {}",e);
        } finally {
            // close the service user resolver
            if (madisonServiceUserResolver != null && madisonServiceUserResolver.isLive()) {
                madisonServiceUserResolver.close();
            }
        }
        // set the render condition appropriately
        request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(isRendered));
    }
}
