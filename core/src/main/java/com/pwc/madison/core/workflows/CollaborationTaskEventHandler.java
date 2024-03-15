package com.pwc.madison.core.workflows;

import com.pwc.madison.core.util.NodeHolder;
import com.pwc.madison.core.util.State;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.taskmanagement.Task;
import com.adobe.granite.taskmanagement.TaskAction;
import com.adobe.granite.taskmanagement.TaskEvent;
import com.adobe.granite.taskmanagement.TaskManager;
import com.adobe.granite.taskmanagement.TaskManagerException;
import com.adobe.granite.workflow.exec.Status;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.DITAUtils;

@Component(
        immediate = true,
        service = EventHandler.class,
        property = { EventConstants.EVENT_FILTER + "=" + "(" + TaskEvent.TASK_EVENT_TYPE_STRING + "=TASK_COMPLETED)",
                EventConstants.EVENT_TOPIC + "=" + TaskEvent.TOPIC, })
public class CollaborationTaskEventHandler implements EventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CollaborationTaskEventHandler.class);
    @Reference
    private ResourceResolverFactory resolverFactory;
    @Reference
    private WorkflowService workflowService;
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;


    @Override
    public void handleEvent(Event event) {
        TaskEvent taskEvent = (TaskEvent) event;
        if (DITAConstants.REVIEW_TASK_TYPE.equals(taskEvent.getTaskType())) {
            String taskId = taskEvent.getTaskId();

            ResourceResolver resolver = null;
            try {
                Map<String, Object> serviceUserParams = Collections.unmodifiableMap(new HashMap<String, Object>() {
                    private static final long serialVersionUID = 1L;

                    {
                        put(ResourceResolverFactory.SUBSERVICE, madisonSystemUserNameProviderService.getFmditaServiceUsername());
                    }
                });
                resolver = resolverFactory.getServiceResourceResolver(serviceUserParams);
                TaskManager tM = resolver.adaptTo(TaskManager.class);
                Task t = tM.getTask(taskId);
                TaskAction selectedAction = t.getSelectedAction();
                if (selectedAction != null
                        && MadisonConstants.COLLABORATION_TASK_ACTION_COMPLETE.equals(selectedAction.getActionID()))
                    closeReviewTask(resolver, taskId);
            } catch (LoginException | TaskManagerException e) {
                LOG.error("Error in closing the review ", e);
            } finally {
                if (resolver != null)
                    resolver.close();
            }

        }
    }

    private boolean closeReviewTask(ResourceResolver resourceResolver, String taskId) {
        Session session = null;
        try {
            TaskManager tM = resourceResolver.adaptTo(TaskManager.class);
            session = resourceResolver.adaptTo(Session.class);
            Task projectTaskObj = tM.getTask(taskId);
            String taskIds = (String) projectTaskObj.getProperty(DITAConstants.TASK_IDS);

            WorkflowSession wfSession = workflowService.getWorkflowSession(session);
            Workflow workflow = GetWorkflow(resourceResolver, taskId);// wfSession.getWorkflow(workflowId);
            MetaDataMap meta = workflow.getWorkflowData().getMetaDataMap();
            String[] taskIdArr;
            if (taskIds != null) {

                taskIdArr = taskIds.split("\\|");
                for (String curTaskId : taskIdArr) {
                    try {
                        Task task = tM.getTask(curTaskId);
                        if (task.getStatus() != Status.COMPLETE) {
                            tM.completeTask(task.getId(), null);
                        }
                    } catch (Exception e) {
                        LOG.error("Review Task completion Failed.", e);
                    }
                }
            }

            Object orgNode = meta.get(DITAConstants.ORIGINAL_TOPICS);
            String reviewID = meta.get(DITAConstants.REVIEW_ID, String.class);
            String reviewNodePath = DITAConstants.REVIEW_STORE_DATA + MadisonConstants.FORWARD_SLASH + reviewID;
            State state = new State(session);
            NodeHolder reviewNode = new NodeHolder(reviewNodePath, state);
            String[] orgTopics = {};
            if (orgNode != null) {
                orgTopics = orgNode.toString().split("\\" + DITAConstants.ITEM_SEPARATOR);
            }
            Boolean isDitamap = meta.get(DITAConstants.IS_DITAMAP, Boolean.class);
            if (isDitamap == null) {
                isDitamap = false;
            }
            String ditamap = null;
            if (isDitamap) {
                ditamap = meta.get(DITAConstants.DITAMAP, String.class);
            }

            List<String> reviewedTopics = new ArrayList<>();
            if (isDitamap) {
                DITAUtils.setDocStates(new String[] { ditamap }, DITAConstants.DITA_DOCUMENTSTATE_DRAFT, session, true,
                        true, DITAConstants.COMPLETE_STATUS);
            }
            // Setting Document state
            DITAUtils.setDocStates(orgTopics, DITAConstants.DITA_DOCUMENTSTATE_DRAFT, session, true, true,
                    DITAConstants.COMPLETE_STATUS);

            meta.put(DITAConstants.STATUS_PROP_NAME, DITAConstants.COMPLETE);
            reviewNode.setProperty(DITAConstants.STATUS_PROP_NAME, DITAConstants.COMPLETE);
            // createCloseReviewTask(resourceResolver, workflow);
            session.refresh(true);
            wfSession.updateWorkflowData(workflow, workflow.getWorkflowData());
            session.save();
        } catch (Exception e) {
            LOG.error("Failed to close review task", e);
            return false;
        } finally {
            if (session != null) {
                session.logout();
            }
        }
        return true;
    }

    private Workflow GetWorkflow(ResourceResolver resourceResolver, String taskId) {

        Workflow workflow = null;
        try {

            String workflowId = null;
            Session session = resourceResolver.adaptTo(Session.class);
            if (taskId != null) {
                TaskManager tM = resourceResolver.adaptTo(TaskManager.class);
                Task task = tM.getTask(taskId);
                if (task != null) {
                    workflowId = (String) task.getProperty(DITAConstants.WORKFLOW_ID);
                }
            }
            WorkflowSession wfSession = workflowService.getWorkflowSession(session);
            if (workflowId != null && !workflowId.isEmpty()) {

                workflow = wfSession.getWorkflow(workflowId);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return workflow;
    }

}
