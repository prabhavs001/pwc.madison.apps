package com.pwc.madison.core.workflows;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
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
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.HistoryItem;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MailSenderService;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.SyndicationUtil;
import com.pwc.madison.core.util.TranslationUtil;

/**
 * Workflow process for notifying subscribing territories when simple and full cycle workflow is triggered on source
 * content
 */
@Component(
    service = WorkflowProcess.class,
    property = { "process.label = Madision - Content translation notification" })
public class NotifySubscribersTask implements WorkflowProcess {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private static final String SUBSCRIBER_BASE_PATH = MadisonConstants.PWC_MADISON_DAM_BASEPATH
            + MadisonConstants.DITAROOT_TEXT;
    private static final String INBOX_NOTIFICATION_TITLE = "Source content update";

    @Reference
    private ResourceResolverFactory resolverFactory;
    @Reference
    transient InboxNotificationSender inboxNotificationSender;
    @Reference
    private MailSenderService mailSenderService;

    @Override
    public void execute(final WorkItem item, final WorkflowSession wfSession, final MetaDataMap meta)
            throws WorkflowException {

        ResourceResolver resolver = null;
        try {
            resolver = MadisonUtil.getResourceResolver(resolverFactory, MadisonConstants.SYNDICATION_SERVICE_USER);
            final Resource payload = getResourceFromPayload(item, resolver);

            if (payload == null) {
                log.error("Empty payload");
                return;
            }
            final String sourcePath = payload.getPath();
            final String syndPath = getSyndicationSource(resolver, payload);

            // check if content has translation config. Author comment will be sent as notification to subscribers
            if (StringUtils.isNotBlank(syndPath)) {
                final String authorComments = getAuthorComments(item, wfSession);
                // Configuration resource
                final Resource confResource = resolver.getResource(syndPath);

                // check if configuration node has child nodes
                if (confResource != null && confResource.hasChildren()) {
                    final Iterator<Resource> subItr = confResource.listChildren();

                    while (subItr.hasNext()) {
                        final Resource subResource = subItr.next();

                        if (subResource.getValueMap().get(DITAConstants.PN_SUBSCRIBER) != null) {
                            final String destination = SUBSCRIBER_BASE_PATH
                                    + subResource.getValueMap().get(DITAConstants.PN_SUBSCRIBER).toString();
                            final Resource destResource = resolver.getResource(destination);
                            final String territoryCode = MadisonUtil.getTerritoryCodeForPath(destination);
                            if (StringUtils.isBlank(territoryCode)) {
                                log.error("No territory code found for destination. Send notification failed");
                                return;
                            }
                            final String territoryAuthorGroup = new StringBuilder().append(territoryCode)
                                    .append(MadisonConstants.HYPHEN).append(MadisonConstants.MADISON_PUBLISHER)
                                    .toString();

                            final Set<Authorizable> allAllowedMembers = TranslationUtil.getTerritoryGroups(resolver,
                                    destResource, territoryAuthorGroup, false);

                            if (allAllowedMembers.isEmpty()) {
                                log.error("No territory code found for destination. Send notification failed");
                                return;
                            }

                            final Iterator<Authorizable> itr = allAllowedMembers.iterator();

                            while (itr.hasNext()) {
                                final String grpName = itr.next().getID();
                                if (StringUtils.isNotBlank(grpName)) {
                                    inboxNotification(resolver, grpName, destResource, sourcePath, authorComments);

                                    final Set<Authorizable> groupMembers = TranslationUtil.getTerritoryGroups(resolver,
                                            destResource, grpName, true);
                                    /*emailNotification(payload, groupMembers);*/

                                }
                            }

                        }
                    }

                }
            } else {
                log.info("Author did not enter comments or There are no subscribers for {} " + payload.getPath());
                return;
            }

        } catch (final Exception e) {
            log.error("Failed to send notification", e);
        } finally {
            if (resolver != null) {
                resolver.close();
            }
        }
    }

    /**
     * Create and send inbox notification to authors
     */
    private void inboxNotification(final ResourceResolver resolver, final String assignee, final Resource destResource,
            final String sourcePath, final String authorComments) throws TaskManagerException {
        final String title = INBOX_NOTIFICATION_TITLE;
        final String message = StringUtils.isNoneBlank(authorComments) ? authorComments
                : "Source content is updated at " + sourcePath;
        SyndicationUtil.sendInboxNotification(inboxNotificationSender, resolver, destResource, title, message,
                assignee);
    }

    /**
     * Send email notification to Territory authors
     */
    private void emailNotification(final Resource payload, final Set<Authorizable> authors) throws RepositoryException {
        final Map<String, String> emailParams = new HashMap<>();
        final String title = payload.getParent().getName();
        emailParams.put("title", title);
        emailParams.put("source", MadisonUtil.getTerritoryCodeForPath(payload.getPath()));
        final String subject = title + " has been updated and is now available for translation";
        SyndicationUtil.sendSyndicationEmailNotification(MadisonConstants.TRANSLATION_UPDATE_EMAIL_TEMPLATE, subject,
                emailParams, mailSenderService, authors);
    }

    /**
     * Returns syndication source property if present
     */
    private String getSyndicationSource(final ResourceResolver resolver, final Resource payload) {

        String syndSource = StringUtils.EMPTY;
        if (payload.getName().equals(MadisonConstants.DITAROOT_TEXT)) {
            return syndSource;
        }

        final Resource configNode = resolver
                .getResource(payload.getPath() + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
        final ValueMap props = configNode.getValueMap();

        if (props.containsKey(DITAConstants.PN_SYNDICATION_SOURCE)) {
            syndSource = props.get(DITAConstants.PN_SYNDICATION_SOURCE).toString();
            return syndSource;
        } else {
            return getSyndicationSource(resolver, payload.getParent());

        }

    }

    /**
     * Returns resource from payload
     */
    private Resource getResourceFromPayload(final WorkItem item, final ResourceResolver resolver) {
        if (!item.getWorkflowData().getPayloadType().equals("JCR_PATH")) {
            return null;
        }
        final String path = item.getWorkflowData().getPayload().toString();
        return resolver.getResource(path);
    }

    /**
     * Get author comments from simple and full cycle workflows first step
     */
    private String getAuthorComments(final WorkItem item, final WorkflowSession wfSession) {
        String authorComments = StringUtils.EMPTY;

        try {
            final List<HistoryItem> historyItems = wfSession.getHistory(item.getWorkflow());
            authorComments = historyItems.get(0).getComment();

        } catch (final WorkflowException e) {
            log.error(e.getMessage(), e);
        }
        return authorComments;
    }
}
