package com.pwc.madison.core.workflows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.notifications.InboxNotificationSender;
import com.adobe.granite.taskmanagement.TaskManagerException;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.commons.Externalizer;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MailSenderService;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.SyndicationUtil;

/**
 * The Class WorkflowCompletionNotification is a Process to notify the assignees of the workflow about the completion of
 * Workflow.
 */
@Component(service = WorkflowProcess.class, property = { "process.label=Madison - Workflow Completion Notification" })
public class WorkflowCompletionNotification implements WorkflowProcess {


    private static final Logger LOG = LoggerFactory.getLogger(WorkflowCompletionNotification.class);
    private static final String URL_PARAM_PAGE_READ_ONLY = "?wcmmode=disabled";

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Reference
    Externalizer externalizer;

    @Reference
    private InboxNotificationSender inboxNotificationSender;

    @Reference
    private MailSenderService mailSenderService;

    /*
     * (non-Javadoc)
     * 
     * @see com.adobe.granite.workflow.exec.WorkflowProcess#execute(com.adobe.granite.workflow.exec.WorkItem,
     * com.adobe.granite.workflow.WorkflowSession, com.adobe.granite.workflow.metadata.MetaDataMap) This method notifies
     * the assignees via workflow inbox as well as email.
     */
    @Override
    public void execute(final WorkItem workItem, final WorkflowSession workflowSession, final MetaDataMap metaDataMap)
            throws WorkflowException {

        final ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                MadisonConstants.SYNDICATION_SERVICE_USER);

        final MetaDataMap metadataMap = workItem.getWorkflowData().getMetaDataMap();

        if(workItem.getWorkflowData().getPayload() == null){
            LOG.error("Payload is null");
            return;
        }
        
        String payload = (String) workItem.getWorkflowData().getPayload();

        String workflowTitle = metadataMap.get(MadisonConstants.WORKFLOW_TITLE, String.class);

        String assignee = metadataMap.get(DITAConstants.ASSIGNEE, String.class);
        
        String initiator = metadataMap.get(DITAConstants.INITIATOR, String.class);

        List<String> userGroupsToBeNotified = new ArrayList<>();

        if (assignee.contains(MadisonConstants.COMMA_SEPARATOR)) {
            userGroupsToBeNotified = new ArrayList<>(Arrays.asList(assignee.split(MadisonConstants.COMMA_SEPARATOR)));
        } else {
            userGroupsToBeNotified.add(assignee);
        }
        
        userGroupsToBeNotified.add(initiator);

        Set<Authorizable> authorizables = getAuthorizables(resourceResolver, userGroupsToBeNotified);

        String publishedPage = getLastPublishedPath(payload, resourceResolver);
        if (publishedPage != null) {
            publishedPage = publishedPage.replace(MadisonConstants.HTML_EXTN + URL_PARAM_PAGE_READ_ONLY, "");
        }
        sendNotifications(resourceResolver, publishedPage, authorizables, workflowTitle);

    }

    /**
     * Gets the authorizables.
     *
     * @param resourceResolver
     *            the resource resolver
     * @param userGroupsToBeNotified
     *            the user groups to be notified
     * @return the authorizables
     */
    private Set<Authorizable> getAuthorizables(ResourceResolver resourceResolver, List<String> userGroupsToBeNotified) {
        Set<Authorizable> authorizables = new HashSet<>();
        final UserManager userManager = resourceResolver.adaptTo(UserManager.class);
        if (null != userManager) {
            for (String userGroup : userGroupsToBeNotified) {
                try {
                    final Authorizable authorizable = userManager.getAuthorizable(userGroup);
                    if (null != authorizable) {
                        authorizables.add(authorizable);
                    }
                } catch (RepositoryException e) {
                    LOG.error("An error ocurred while fetching users/groups {}", e);
                }
            }
        }
        return authorizables;
    }

    /**
     * Gets the last published path.
     *
     * @param payload
     *            the payload
     * @param resourceResolver
     *            the resource resolver
     * @return the last published path
     */
    private String getLastPublishedPath(String payload, ResourceResolver resourceResolver) {
        Resource resource = resourceResolver.getResource(payload);
        if (resource != null) {
            Resource metadataRes = resourceResolver.getResource(resource.getPath() + MadisonConstants.METADATA_PATH);
            Resource presetRes = resourceResolver
                    .getResource(resource.getPath() + DITAConstants.AEMSITE_PRESETS_NODE);
            return getPublishedPagePath(metadataRes, presetRes);

        }

        return StringUtils.EMPTY;
    }

    /**
     * Gets the published page path.
     *
     * @param metadataRes
     *            the metadata res
     * @param presetRes
     *            the preset res
     * @return the published page path
     */
    private String getPublishedPagePath(Resource metadataRes, Resource presetRes) {
        if (metadataRes != null) {
            ValueMap valueMap = metadataRes.getValueMap();
            if (presetRes != null && valueMap.containsKey(DITAConstants.PN_IS_PUBLISHING_POINTS)) {
                String isPubPoint = valueMap.get(DITAConstants.PN_IS_PUBLISHING_POINTS, String.class);
                if (!StringUtils.isEmpty(isPubPoint) && isPubPoint.equals("yes")) {
                    ValueMap presetValueMap = presetRes.getValueMap();
                    if (presetValueMap.containsKey(DITAConstants.PN_LAST_PUBLISHED_PATH)) {
                        return presetValueMap.get(DITAConstants.PN_LAST_PUBLISHED_PATH, String.class);
                    }
                }
            }
        }
        return StringUtils.EMPTY;

    }

    /**
     * Send notifications.
     *
     * @param resourceResolver
     *            the resource resolver
     * @param publishedPagePath
     *            the published page path
     * @param authorizables
     *            the authorizables
     * @param workflowTitle
     *            the workflow title
     */
    private void sendNotifications(final ResourceResolver resourceResolver, String publishedPagePath,
            final Set<Authorizable> authorizables, String workflowTitle) {

        try {
            Resource publishedPageResource = resourceResolver.getResource(publishedPagePath);
            final Map<String, String> emailParams = new HashMap<>();
            emailParams.put("workflow_title", workflowTitle);
            emailParams.put("published_page",
                    externalizer.authorLink(resourceResolver, publishedPagePath + MadisonConstants.HTML_EXTN));
            final String subject = String.format("Workflow %s has been completed.", workflowTitle);
            Set<Authorizable> emailList = new HashSet<>();
            for (final Authorizable authorizable : authorizables) {
                SyndicationUtil.sendInboxNotification(inboxNotificationSender, resourceResolver, publishedPageResource,
                        subject, subject, authorizable.getID());
                if (authorizable.isGroup()) {
                    addAllMembers((Group) authorizable, emailList);
                } else {
                    emailList.add(authorizable);
                }
            }
            /*SyndicationUtil.sendSyndicationEmailNotification(MadisonConstants.WORKFLOW_COMPLETION_TEMPLATE, subject,
                    emailParams, mailSenderService, emailList);*/

        } catch (final RepositoryException | TaskManagerException e) {
            LOG.error("Workflow Completion Notification Error  ", e);
        }

    }
    
    /**
     * Adds all members.
     *
     * @param parentGroup
     *            the parent group
     * @param memberGroups
     *            the member groups
     */
    private static void addAllMembers(Group parentGroup, Set<Authorizable> memberGroups) {
        try {
            Iterator<Authorizable> allMembers = parentGroup.getMembers();
            while (allMembers.hasNext()) {
                Authorizable member = allMembers.next();
                if (null != memberGroups) {
                    memberGroups.add(member);
                }
            }
        } catch (RepositoryException e) {
            LOG.error(e.getMessage(), e);
        }

    }

}
