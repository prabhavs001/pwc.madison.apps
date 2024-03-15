package com.pwc.madison.core.workflows;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Query;
import org.apache.jackrabbit.api.security.user.QueryBuilder;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.taskmanagement.Task;
import com.adobe.granite.taskmanagement.TaskAction;
import com.adobe.granite.taskmanagement.TaskManager;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.NodeHolder;
import com.pwc.madison.core.util.State;

//Sling Imports

//This is a component so it can provide or consume services
@Component(service = WorkflowProcess.class,
           property = { "process.label= Madision - Assign review task" })
public class AssignReviewTasks implements WorkflowProcess {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Reference
    protected SlingRepository repository;
    
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    Session session = null;
    @Reference
    private WorkflowService workflowService;
    @Reference
    private ResourceResolverFactory resolverFactory;

    private static TaskManager getProjectTaskManager(ResourceResolver resolver, Resource projectResource)
            throws PersistenceException {
        String strTasksNode = "tasks";
        Resource projectContent = projectResource.getChild(JcrConstants.JCR_CONTENT);
        Resource tasksNode = projectContent.getChild(strTasksNode);

        if (tasksNode == null) {
            // create
            tasksNode = resolver.create(projectContent, strTasksNode, null);
        }
        return tasksNode.adaptTo(TaskManager.class);
    }

    @Override
    public void execute(WorkItem item, WorkflowSession wfsession, MetaDataMap args) throws WorkflowException {
        ResourceResolver resolver = null;
        try {
            MetaDataMap meta = item.getWorkflowData().getMetaDataMap();
            if (meta == null) {
                log.error("Workflow metadata null. Cannot proceed with review.");
                return;
            }
            resolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
            Session session = resolver.adaptTo(Session.class);
            assignTasks(item, resolver, meta, session);
            session.save();
        } catch (Exception e) {
            log.error("Failed to assign review tasks ", e);
        } finally {
            if (resolver != null)
                resolver.close();
        }

    }

    private void assignTasks(WorkItem item, ResourceResolver resolver, MetaDataMap meta, Session session) {

        // Get a list of all user accounts under the selected review group to assign created tasks to them.
        //Session userSession = null;
        try {
            String wfID = item.getWorkflow().getId();
            String reviewId = meta.get(DITAConstants.REVIEW_ID, String.class);
            String reviewNodePath = DITAConstants.REVIEW_STORE_DATA + MadisonConstants.FORWARD_SLASH + reviewId;
            State state = new State(session);
            NodeHolder reviewNode = new NodeHolder(reviewNodePath, state);
            String reviewPage = DITAConstants.NEW_REVIEW_UI_PAGE + reviewId + DITAConstants.WCMMODE_DISABLED;
            UserManager userManager = resolver.adaptTo(UserManager.class);
            String reviewGroup = meta.get(DITAConstants.COLLABORATORS, String.class);
            reviewGroup = reviewGroup == null ? "" : reviewGroup;
            String reviewList[] = reviewGroup.split(DITAConstants.USER_SEPARATOR);
            List<Authorizable> authorizers = new ArrayList<Authorizable>();
            for (String reviewer : reviewList) {
                Authorizable auth = userManager.getAuthorizable(reviewer);
                if (auth.isGroup()) {
                    final String rG = reviewer;
                    Iterator<Authorizable> members = userManager.findAuthorizables(new Query() {
                        @Override
                        public <T> void build(QueryBuilder<T> builder) {
                            builder.setScope(rG, false);
                        }
                    });
                    while (members.hasNext())
                        authorizers.add(members.next());
                } else {
                    authorizers.add(auth);
                }
            }

            String reviewTitle = meta.get(DITAConstants.TITLE, String.class);
            String reviewDescription = meta.get(DITAConstants.DESCRIPTION, String.class);
            String reviewDeadline = meta.get(DITAConstants.DEADLINE, String.class);
            Date reviewDueTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse(reviewDeadline);

            TaskManager tM = resolver.adaptTo(TaskManager.class);
            Task parentTask = null;
            TaskManager projectTaskManager = null;

            String projectPath = meta.get(DITAConstants.PROJECT_PATH, String.class);
            if (projectPath != null)//create a parent task to be tracked via project
            {
                Resource projectResource = resolver.getResource(projectPath);
                projectTaskManager = getProjectTaskManager(resolver, projectResource);
                parentTask = projectTaskManager
                        .createTask(projectTaskManager.getTaskManagerFactory().newTask(DITAConstants.REVIEW_TASK_TYPE));
                parentTask.setName("Close Collaboration  - " + reviewTitle);
                parentTask.setDescription(reviewDescription);
                parentTask.setProperty(DITAConstants.PROJECT_PATH, projectPath);
                parentTask.setProperty(DITAConstants.WORKFLOW_ID, wfID);
                parentTask.setContentPath(reviewPage);
                parentTask.setProperty(DITAConstants.TASK_DUE_DATE, reviewDeadline);
                parentTask.setDueTime(reviewDueTime);
                parentTask.setProperty(DITAConstants.REVIEW_ID, reviewId);
                parentTask.setCurrentAssignee(meta.get(DITAConstants.INITIATOR, String.class));

                List<TaskAction> actions = new ArrayList<TaskAction>();
                actions.add(new TaskAction() {
                    @Override
                    public String getActionID() {
                        return MadisonConstants.COLLABORATION_TASK_ACTION_COMPLETE;
                    }

                    @Override
                    public void setActionID(String actionID) {
                    }
                });
                parentTask.setActions(actions);
            }

            String taskIds = "";
            //Assign a dummy task to service user
            //Done in order to obtain permission to close reviewer tasks
            Task serviceuser = tM
                    .createTask(tM.getTaskManagerFactory().newTask(DITAConstants.REVIEW_TASK_NOTIFICATION));
            serviceuser.setName("Auto created review task");
            serviceuser.setDescription("Auto created review task");
            serviceuser.setCurrentAssignee(madisonSystemUserNameProviderService.getFmditaServiceUsername());
            tM.saveTask(serviceuser);
            Iterator<Authorizable> result = authorizers.listIterator();
            while (result.hasNext()) {
                Authorizable user = result.next();
                //Create reviewer tasks as sub task to parent fmditaservice user task
                Task t = tM.createTask(serviceuser.getId(),
                        tM.getTaskManagerFactory().newTask(DITAConstants.REVIEW_TASK_NOTIFICATION));
                t.setName(reviewTitle);
                t.setDescription(reviewDescription);
                t.setContentPath(reviewPage);
                t.setCurrentAssignee(user.getID());
                t.setDueTime(reviewDueTime);
                t.setProperty(DITAConstants.REVIEW_ID, reviewId);
                taskIds = taskIds + t.getId() + "|";
                tM.saveTask(t);
            }
            //Complete the dummy task
            tM.completeTask(serviceuser.getId(), null);
            // Save task ids into metadatamap so that tasks can be closed later.
            meta.put(DITAConstants.TASK_IDS, taskIds);
            if (parentTask != null) {
                meta.put(DITAConstants.PROJECT_TASK, parentTask.getId());
                //save task ids into parent task as well
                parentTask.setProperty(DITAConstants.TASK_IDS, taskIds);
                parentTask.setProperty(DITAConstants.REVIEW_WF, wfID);
                parentTask.setProperty(DITAConstants.REVIEW_ID, reviewId);
                projectTaskManager.saveTask(parentTask);
            }
            reviewNode.setProperty(DITAConstants.TASK_IDS, taskIds);
            reviewNode.setProperty(DITAConstants.PROJECT_TASK, parentTask.getId());
        } catch (Exception e) {
            log.error("Error assigning review task", e);
        }
    }

}	
