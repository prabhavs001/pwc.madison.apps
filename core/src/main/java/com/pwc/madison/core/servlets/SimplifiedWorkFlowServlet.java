package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.rmi.ServerException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.jcr.RepositoryException;
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

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.model.WorkflowModel;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;

/**
 * A Servlet that invokes an AEM Workflow
 */
@Component(service = Servlet.class,
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = { Constants.SERVICE_DESCRIPTION + "=Servlet to invoke PwC DITA Simplified Workflow",
                   "sling.servlet.methods=" + "POST", "sling.servlet.paths=" + "/bin/pwc-madison/simplereviewtask" })
public class SimplifiedWorkFlowServlet extends SlingAllMethodsServlet {
    private static final long serialVersionUID = 1L;
    private static final String VERSION_JSON = "versionJson";

    protected final Logger log = LoggerFactory.getLogger(SimplifiedWorkFlowServlet.class);
    @Reference
    protected SlingRepository repository;

    @Reference
    private ResourceResolverFactory resolverFactory;
    @Reference
    private WorkflowService workflowService;

    private Session session = null;

    protected void handleOperationCreateReview(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServerException, IOException {

        ResourceResolver resolver = null;
        ResourceBundle resourceBundle = request.getResourceBundle(request.getLocale());
        try {
            resolver = request.getResourceResolver();
            session = resolver.adaptTo(Session.class);
            // Start review workflow

            String payloadJson = request.getParameter("contentPath");
            String approvers = request.getParameter("approvers");
            String publishers = request.getParameter("publishers");
            String title = request.getParameter("title");
            String taskDueDate = request.getParameter("taskDueDate");
            String description = request.getParameter("comment");
            String versionJson = request.getParameter("versions");
            String reviewVersion = request.getParameter(DITAConstants.REVIEW_VERSION);
            String ditamapHierarchy = request.getParameter(DITAConstants.DITAMAP_HIERARCHY);
            if (description == null) {
                description = "";
            }
            String sendEmailNotification = request.getParameter("sendEmailNotification");
            boolean bExcludeTopics = true;
            String excludeTopics = request.getParameter("excludetopics");
            if (excludeTopics == null || !excludeTopics.equalsIgnoreCase("on"))
                bExcludeTopics = false;
            /**
             * projectResource.getPath() is used to set as value of the drop down
             * fields in create approval wizard; projectResource extends from
             * resource
             */
            String projectResourcePath = "/content/projects/pwc-madison-global";
            JSONObject reqObj;
            reqObj = new JSONObject(payloadJson);
            WorkflowSession wfSession = workflowService.getWorkflowSession(session);
            JSONArray assetArray = reqObj.getJSONArray(DITAConstants.INPUT_PAYLOAD_ASSET);
            String asset;
            asset = assetArray.get(0).toString();
            WorkflowData wfData = wfSession.newWorkflowData("JCR_PATH", asset);

            WorkflowModel wfModel = wfSession.getModel(DITAConstants.WF_MODEL_APPROVAL_DITAMAP_KEY);
            wfData.getMetaDataMap().put(DITAConstants.OPERATION, DITAConstants.DITA_APPROVE_OPERATION);
            wfData.getMetaDataMap().put(DITAConstants.INPUT_PAYLOAD, payloadJson);
            wfData.getMetaDataMap().put(DITAConstants.DEADLINE, taskDueDate);
            String currentUser = request.getResourceResolver().getUserID();
            wfData.getMetaDataMap().put(DITAConstants.ASSIGNEE, currentUser);
            wfData.getMetaDataMap().put(DITAConstants.REVIEWER, currentUser);
            wfData.getMetaDataMap().put(DITAConstants.APPROVER, approvers);
            wfData.getMetaDataMap().put(DITAConstants.PUBLISHER, publishers);
            wfData.getMetaDataMap().put(DITAConstants.TITLE, title);
            wfData.getMetaDataMap().put(DITAConstants.DESCRIPTION, description);
            wfData.getMetaDataMap().put(DITAConstants.NOTIFY_EMAIL, sendEmailNotification);
            wfData.getMetaDataMap().put(DITAConstants.STATUS_PROP_NAME, DITAConstants.IN_PROGRESS);
            wfData.getMetaDataMap().put(DITAConstants.PROJECT_PATH, projectResourcePath);
            wfData.getMetaDataMap().put(DITAConstants.START_TIME, System.currentTimeMillis());

            Boolean isDitamap = asset.endsWith("." + DITAConstants.DITAMAP);
            wfData.getMetaDataMap().put(DITAConstants.IS_DITAMAP, isDitamap);
            if (isDitamap) {
                wfData.getMetaDataMap().put(DITAConstants.DITAMAP, asset);
            }
            wfData.getMetaDataMap().put(DITAConstants.DITAMAP_INCLUDE_TOPICS, !bExcludeTopics && isDitamap);

            wfData.getMetaDataMap().put(DITAConstants.INITIATOR, currentUser);
            String orgTopics = "";
            if (isDitamap) {
                String topicsSelected = request.getParameter("review-topics");
                if(null != topicsSelected){
                    orgTopics = topicsSelected;
                }
            } else {
                for (int i = 0; i < assetArray.length(); i++) {
                    String path = assetArray.get(i).toString();
                    orgTopics = orgTopics + path + DITAConstants.ITEM_SEPARATOR;
                }
            }
            wfData.getMetaDataMap().put(DITAConstants.ORIGINAL_TOPICS, orgTopics);
            wfData.getMetaDataMap().put(DITAConstants.SELECTED_TOPICS, request.getParameter(DITAConstants.SELECTED_TOPICS));
            String referencedMaps = request.getParameter(DITAConstants.REVIEW_DITAMAPS);
            if (null != referencedMaps) {
                if (!referencedMaps.isEmpty()) {
                    wfData.getMetaDataMap().put(DITAConstants.REVIEW_DITAMAPS, referencedMaps);
                }
            }
            Map<String, Object> metaDataMap = new HashMap<String, Object>();
            metaDataMap.put(MadisonConstants.WORKFLOW_TITLE, title);
            wfData.getMetaDataMap().put(VERSION_JSON,versionJson);
            wfData.getMetaDataMap().put(DITAConstants.REVIEW_VERSION, reviewVersion);
            wfData.getMetaDataMap().put(DITAConstants.DITAMAP_HIERARCHY, ditamapHierarchy);
            wfSession.startWorkflow(wfModel, wfData, metaDataMap);
            session.save();
        } catch (RepositoryException | JSONException | WorkflowException e) {
            log.error("Following occurred in handleOperationCreateReview : {}", e);
        } finally {
            if (session != null)
                session.logout();
        }
    }


    @Override
    protected void doPost(final SlingHttpServletRequest req, final SlingHttpServletResponse resp)
            throws ServletException, IOException {
        log.debug("INSIDE doGet of SimplifiedWorkFlowServlet");
        handleOperationCreateReview(req, resp);
    }
}
