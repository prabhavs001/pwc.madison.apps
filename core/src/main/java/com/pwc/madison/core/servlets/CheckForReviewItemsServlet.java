package com.pwc.madison.core.servlets;

import com.google.gson.Gson;
import com.pwc.madison.core.models.ReviewButtonModel;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Servlet that is used by a client-side render condition of the "Review Map Revisions"
 */
@Component(service = Servlet.class,
           property = { Constants.SERVICE_DESCRIPTION
                   + "=This servlet is called in workitem console for checking whether to load the 'Review Map Revisions' button.",
                   "sling.servlet.methods=" + "GET", "sling.servlet.paths=" + "/bin/pwc/checkforreviewitems",
                   "sling.servlet.extensions=" + "json" })
public class CheckForReviewItemsServlet extends SlingSafeMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(CheckForReviewItemsServlet.class);
    private static final long serialVersionUid = 1L;

    @Reference
    ResourceResolverFactory resolverFactory;

    @Reference
    WorkflowService workflowService;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {
        ReviewButtonModel reviewButton = new ReviewButtonModel();
        Boolean isRender = false;
        String workflowId = StringUtils.EMPTY;
        String reviewPage = StringUtils.EMPTY;
        String reviewButtonJson = StringUtils.EMPTY;
        List<String> reviewItemsList = Collections.EMPTY_LIST;
        ResourceResolver resolver = MadisonUtil
                .getResourceResolver(resolverFactory, MadisonConstants.MADISON_GENERIC_SUB_SERVICE);
        Session session = resolver.adaptTo(Session.class);
        WorkflowSession wfSession = workflowService.getWorkflowSession(session);
        try {
            RequestParameterMap requestParameterMap = request.getRequestParameterMap();
            if (requestParameterMap.containsKey(WorkflowConstants.WORKFLOW_ID)) {
                workflowId = requestParameterMap.getValue(WorkflowConstants.WORKFLOW_ID).getString();
            }
            if (!workflowId.isEmpty()) {
                Workflow workflow = wfSession.getWorkflow(workflowId);
                if (null != workflow) {
                    MetaDataMap workflowData = workflow.getWorkflowData().getMetaDataMap();
                    if(workflowData.containsKey(DITAConstants.PROP_REVIEW_PAGE)){
                        reviewPage = workflowData.get(DITAConstants.PROP_REVIEW_PAGE, String.class);
                    }
                    if (requestParameterMap.containsKey(DITAConstants.PN_IS_INLINE)) {
                        String topicsSentForReview = workflowData.get(DITAConstants.ORIGINAL_TOPICS, String.class);
                        reviewItemsList = checkForReviewItems(topicsSentForReview);
                    } else {
                        String refDitaMaps = workflowData.get(DITAConstants.REVIEW_DITAMAPS, String.class);
                        reviewItemsList = checkForReviewItems(refDitaMaps);
                    }
                }
            }
            if (!reviewItemsList.isEmpty()) {
                isRender = true;
            }
            reviewButton.setReviewPage(reviewPage);
            reviewButton.setRender(isRender);
            final Gson gson = new Gson();
            reviewButtonJson = gson.toJson(reviewButton);
            response.getWriter().print(reviewButtonJson);
        } catch (WorkflowException e) {
            LOG.error("WorkflowException in CheckForReviewMapsServlet {}", e);
        } finally {
            // close the service user resolver
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
    }

    private List<String> checkForReviewItems(String reviewItemString) {
        List<String> reviewItemsList = Collections.EMPTY_LIST;
        if (null != reviewItemString && !reviewItemString.isEmpty()) {
            String[] reviewItemsArray = reviewItemString.split("\\|");
            reviewItemsList = Arrays.asList(reviewItemsArray);
        }
        return reviewItemsList;
    }
}
