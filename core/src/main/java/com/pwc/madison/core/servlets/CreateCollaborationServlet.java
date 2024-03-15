package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.rmi.ServerException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.model.WorkflowModel;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * A Servlet that invokes an AEM Workflow
 */
@Component(service = Servlet.class,
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = { Constants.SERVICE_DESCRIPTION + "=Servlet to invoke Create Collaboration Workflow",
                   "sling.servlet.methods=" + "POST", "sling.servlet.paths=" + "/bin/pwc-madison/createcollaboration" })
public class CreateCollaborationServlet extends SlingAllMethodsServlet {
    private static final long serialVersionUID = 1L;

    protected final Logger log = LoggerFactory.getLogger(CreateCollaborationServlet.class);
    @Reference
    protected SlingRepository repository;

    @Reference
    private ResourceResolverFactory resolverFactory;
    @Reference
    private WorkflowService workflowService;

    private Session session = null;

    @Override
    protected void doPost(final SlingHttpServletRequest req, final SlingHttpServletResponse resp)
            throws ServletException, IOException {
        log.debug("INSIDE doGet of CreateCollaborationServlet");
        handleOperationCreateReview(req, resp);
    }

    private void handleOperationCreateReview(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServerException, IOException {
        ResourceResolver resourceResolver = request.getResourceResolver();
        session = resourceResolver.adaptTo(Session.class);

        // Start review workflow
        ResourceBundle resourceBundle = request.getResourceBundle(request.getLocale());
        String payloadJson = request.getParameter("contentPath");
        String versionJson = request.getParameter("versions");
        String assignTo = request.getParameter("collaborators");
        String taskDueDate = request.getParameter("taskDueDate");
        String title = request.getParameter("title");
        String allowAllReviewers = request.getParameter("allowAllReviewers");
        String description = request.getParameter("description");
        String reviewVersion = request.getParameter(DITAConstants.REVIEW_VERSION);
        String ditamapHierarchy = request.getParameter(DITAConstants.DITAMAP_HIERARCHY);
        if (description == null) {
            description = "";
        }
        String sendEmailNotification = request.getParameter("sendEmailNotification");
        /**
         * projectResource.getPath() is used to set as value of the drop down
         * fields in create approval wizard; projectResource extends from
         * resource
         */
        String projectResourcePath = request.getParameter("projectPath");

        if (payloadJson == null || assignTo == null || taskDueDate == null) {
            response.sendError(500, resourceBundle.getString("Invalid Input Parameters to Start Collaboration."));
            return;
        }
        if (payloadJson.length() == 0 || assignTo.length() == 0 || taskDueDate.length() == 0) {
            response.sendError(500, resourceBundle.getString("Invalid Input Parameters to Start Collaboration."));
            return;
        }

        JSONObject reqObj;
        try {
            reqObj = new JSONObject(payloadJson);
        } catch (JSONException ex) {
            response.sendError(500, resourceBundle.getString("Invalid Content Path(s) JSON"));
            return;
        }

        try {
            WorkflowSession wfSession = workflowService.getWorkflowSession(session);
            JSONArray assetArray = reqObj.getJSONArray(DITAConstants.INPUT_PAYLOAD_ASSET);
            String asset;
            asset = assetArray.get(0).toString();
            WorkflowData wfData = wfSession
                    .newWorkflowData("JCR_PATH", asset);

            WorkflowModel wfModel = wfSession.getModel(DITAConstants.WF_MODEL_COLLABORATION);
            wfData.getMetaDataMap().put(DITAConstants.OPERATION, DITAConstants.REVIEW_OPERATION);
            wfData.getMetaDataMap().put("versionJson", versionJson);
            wfData.getMetaDataMap().put(DITAConstants.REVIEW_VERSION, reviewVersion);
            wfData.getMetaDataMap().put(DITAConstants.DITAMAP_HIERARCHY, ditamapHierarchy);
            wfData.getMetaDataMap().put(DITAConstants.INPUT_PAYLOAD, payloadJson);
            wfData.getMetaDataMap().put(MadisonConstants.ALLOW_ALL_REVIEWERS, allowAllReviewers);

            wfData.getMetaDataMap().put(DITAConstants.DEADLINE, taskDueDate);
            Date deadline = MadisonUtil.FormatDeadline(taskDueDate);
            wfData.getMetaDataMap().put(DITAConstants.ABSOLUTE_TIME, deadline.getTime());

            wfData.getMetaDataMap().put(DITAConstants.COLLABORATORS, assignTo);
            wfData.getMetaDataMap()
                    .put(DITAConstants.ASSIGNEE, assignTo); //this is needed for the Collaboration parent task console
            wfData.getMetaDataMap().put(DITAConstants.TITLE, title);
            wfData.getMetaDataMap().put(DITAConstants.DESCRIPTION, description);
            wfData.getMetaDataMap().put(DITAConstants.NOTIFY_EMAIL, sendEmailNotification);
            wfData.getMetaDataMap().put(DITAConstants.STATUS_PROP_NAME, DITAConstants.IN_PROGRESS);
            wfData.getMetaDataMap().put(DITAConstants.PROJECT_PATH, projectResourcePath);
            wfData.getMetaDataMap().put(DITAConstants.START_TIME, System.currentTimeMillis());

            Boolean isDitamap = asset.endsWith(DITAConstants.DITAMAP_EXT);
            wfData.getMetaDataMap().put(DITAConstants.IS_DITAMAP, isDitamap);
            if (isDitamap) {
                wfData.getMetaDataMap().put(DITAConstants.DITAMAP, asset);
            }

            String currentUser = request.getResourceResolver().getUserID();
            wfData.getMetaDataMap().put(DITAConstants.INITIATOR, currentUser);
            String orgTopics = "";
            if (isDitamap) {
                String topicsSelected = request.getParameter("review-topics");
                if (null != topicsSelected) {
                    orgTopics = topicsSelected;
                }
            } else {
                for (int i = 0; i < assetArray.length(); i++) {
                    String path = assetArray.get(i).toString();
                    orgTopics = orgTopics + path + DITAConstants.ITEM_SEPARATOR;
                }
            }
            wfData.getMetaDataMap().put(DITAConstants.ORIGINAL_TOPICS, orgTopics);
            wfSession.startWorkflow(wfModel, wfData);

            session.save();
        } catch (Exception e) {
            log.error(e.toString());
            response.sendError(500, resourceBundle.getString("Failed to Start Collaboration."));
        }
    }
}
