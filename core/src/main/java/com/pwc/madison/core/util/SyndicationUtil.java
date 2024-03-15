package com.pwc.madison.core.util;

import com.adobe.acs.commons.notifications.InboxNotification;
import com.adobe.acs.commons.notifications.InboxNotificationSender;
import com.adobe.granite.taskmanagement.TaskManagerException;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.EmailProfile;
import com.pwc.madison.core.services.MailSenderService;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SyndicationUtil {
    private static final Logger LOG = LoggerFactory.getLogger(SyndicationUtil.class);
    private static String FIRST_NAME = "firstName";
    private static String LAST_NAME = "lastName";

    private SyndicationUtil() {
    }

    /**
     * Read syndication subscribers for a given source from conf
     *
     * @param source
     * @param resourceResolver
     * @return
     */
    public static List<Resource> getSyndicationSubscribers(final String source,
            final ResourceResolver resourceResolver) {
        List<Resource> destinationTerritories = new ArrayList<>();

        final String confPath = MadisonConstants.CONF_SYNDICATION_SETTINGS_ROOT;
        final Resource syndicationConfRoot = resourceResolver.getResource(confPath);

        // configuration not available
        if (null == syndicationConfRoot) {
            return destinationTerritories;
        }

        final Iterator<Resource> syndicationSources = syndicationConfRoot.listChildren();
        while (syndicationSources.hasNext()) {
            final Resource syndicationSource = syndicationSources.next();
            final String sourcePath = syndicationSource.getValueMap().get("sourcePath", String.class);

            if (source.equals(sourcePath)) {
                final Iterator<Resource> syndicationSubscribers = syndicationSource.listChildren();
                destinationTerritories = IteratorUtils.toList(syndicationSubscribers);
            }
        }

        return destinationTerritories;
    }

    /**
     * Read syndication subscribers which are already syndicated for a given source from conf
     *
     * @param source
     * @param resourceResolver
     * @return
     */
    public static List<Resource> getSyndicatedSubscribers(final String source,
            final ResourceResolver resourceResolver) {
        final List<Resource> syndicatedSubscribers = new ArrayList<>();

        final String confPath = MadisonConstants.CONF_SYNDICATION_SETTINGS_ROOT;
        final Resource syndicationConfRoot = resourceResolver.getResource(confPath);

        // configuration not available
        if (null == syndicationConfRoot) {
            return syndicatedSubscribers;
        }

        final Iterator<Resource> syndicationSources = syndicationConfRoot.listChildren();
        while (syndicationSources.hasNext()) {
            final Resource syndicationSource = syndicationSources.next();
            final String sourcePath = syndicationSource.getValueMap().get("sourcePath", String.class);

            if (source.equals(sourcePath)) {
                final Iterator<Resource> syndicationSubscribers = syndicationSource.listChildren();
                while (syndicationSubscribers.hasNext()) {
                    final Resource syndicationSubscriber = syndicationSubscribers.next();

                    if (syndicationSubscriber.getValueMap().containsKey(MadisonConstants.PN_IS_SYNDICATED)
                            && syndicationSubscriber.getValueMap().get(MadisonConstants.PN_IS_SYNDICATED,
                                    Boolean.class)) {

                        syndicatedSubscribers.add(resourceResolver
                                .getResource(syndicationSubscriber.getValueMap().get("destinationPath", String.class)));
                    }
                }
            }
        }

        return syndicatedSubscribers;
    }

    /**
     * Read syndication subscribers which are already syndicated inside a given source folder from conf
     *
     * @param source
     * @param resourceResolver
     * @return
     */
    public static Map<String,List<String>> getSyndicatedSubscribersMap(final String source,
            final ResourceResolver resourceResolver) {
        final Map<String,List<String>> syndicatedSubscribers = new HashMap<>();

        final String confPath = MadisonConstants.CONF_SYNDICATION_SETTINGS_ROOT;
        final Resource syndicationConfRoot = resourceResolver.getResource(confPath);

        // configuration not available
        if (null == syndicationConfRoot) {
            return syndicatedSubscribers;
        }

        final Iterator<Resource> syndicationSources = syndicationConfRoot.listChildren();
        while (syndicationSources.hasNext()) {
            final Resource syndicationSource = syndicationSources.next();
            final String sourcePath = syndicationSource.getValueMap().get("sourcePath", String.class);

            if (sourcePath.startsWith(source)) {
                final Iterator<Resource> syndicationSubscribers = syndicationSource.listChildren();
                List<String> destTeritories = new ArrayList<>();
                while (syndicationSubscribers.hasNext()) {
                    final Resource syndicationSubscriber = syndicationSubscribers.next();

                    if (syndicationSubscriber.getValueMap().containsKey(MadisonConstants.PN_IS_SYNDICATED)
                            && syndicationSubscriber.getValueMap().get(MadisonConstants.PN_IS_SYNDICATED,
                                    Boolean.class)) {

                    	destTeritories.add(syndicationSubscriber.getValueMap().get("destinationPath", String.class));
                    }
                }
                syndicatedSubscribers.put(sourcePath, destTeritories);
            }
        }

        return syndicatedSubscribers;
    }
    /**
     * Get territory groups/users from the given folder
     *
     * @param territoryResource
     * @param resourceResolver
     */
    public static Set<Authorizable> getTerritoryGroups(final Resource territoryResource,
            final ResourceResolver resourceResolver, final String territoryGroup) throws RepositoryException {

        final UserManager userManager = resourceResolver.adaptTo(UserManager.class);

        if (null == userManager) {
            return new HashSet<>();
        }

        final Authorizable territoryAuthorGroup = userManager.getAuthorizable(territoryGroup);

        if (null == territoryAuthorGroup) {
            return new HashSet<>();
        }

        Set<Authorizable> notificationGroups = MadisonUtil.checkIfGroupHasPermissionOnFolder(territoryResource,
                userManager, territoryAuthorGroup, resourceResolver, null, true);

        if (notificationGroups.isEmpty() && !territoryResource.getName().equals(MadisonConstants.MADISON_DAM_ROOT)) {
            notificationGroups = getTerritoryGroups(territoryResource.getParent(), resourceResolver, territoryGroup);
        }

        return notificationGroups;
    }

    /**
     * @param emailTemplatePath
     * @param subject
     * @param emailParams
     * @param mailSenderService
     * @param authorizables
     * @throws RepositoryException
     */
    public static void sendSyndicationEmailNotification(final String emailTemplatePath, final String subject,
            final Map<String, String> emailParams, final MailSenderService mailSenderService,
            final Set<Authorizable> authorizables) throws RepositoryException {

        final List<EmailProfile> recipients = WorkFlowUtil.getRecipientEmailIDs(new ArrayList<>(authorizables));

        for (final EmailProfile recipient : recipients) {
            emailParams.put(FIRST_NAME, recipient.getFirstName());
            emailParams.put(LAST_NAME, recipient.getLastName());
            mailSenderService.sendMailWithEmailTemplate(MadisonConstants.NO_REPLY_EMAIL_ID,
                    new String[] { recipient.getEmailId() }, subject, emailTemplatePath, emailParams);
        }
    }

    /**
     * @param sourcePath
     * @param destinationPath
     * @return
     */
    public static String getRelativePath(final String sourcePath, final String destinationPath) {
        final Path sourceFile = Paths.get(sourcePath);
        final Path targetFile = Paths.get(destinationPath);
        final String relativePath = targetFile.relativize(sourceFile).toString().replace("\\",
                DITAConstants.FORWARD_SLASH);
        return relativePath.substring(relativePath.indexOf(DITAConstants.FORWARD_SLASH) + 1, relativePath.length());
    }

    public static Resource checkIfSourceIsSyndicated(final Resource source) {
        if (null == source) {
            return null;
        }

        final Resource sourceContentResource = source.getChild("jcr:content");

        if (null == sourceContentResource) {
            return null;
        }

        boolean isSourceSyndicated = false;
        if (sourceContentResource.getValueMap().containsKey(MadisonConstants.PN_IS_SYNDICATED)) {
            isSourceSyndicated = sourceContentResource.getValueMap().get(MadisonConstants.PN_IS_SYNDICATED,
                    Boolean.class);
        }

        if (isSourceSyndicated) {
            return source;
        }

        if (!isSourceSyndicated && !source.getParent().getName().equals(MadisonConstants.MADISON_DAM_ROOT)) {
            return checkIfSourceIsSyndicated(source.getParent());
        } else {
            return null;
        }

    }

    /**
     * @param inboxNotificationSender
     * @param resourceResolver
     * @param territoryResource
     * @param title
     * @param message
     * @param principal
     * @throws TaskManagerException
     */
    public static void sendInboxNotification(final InboxNotificationSender inboxNotificationSender,
            final ResourceResolver resourceResolver, final Resource territoryResource, final String title,
            final String message, final String principal) throws TaskManagerException {
        if (null == inboxNotificationSender || null == resourceResolver || null == territoryResource) {
            return;
        }

        final InboxNotification inboxNotification = inboxNotificationSender.buildInboxNotification();
        inboxNotification.setTitle(title);
        inboxNotification.setAssignee(principal);
        inboxNotification.setMessage(message);
        inboxNotification.setContentPath(territoryResource.getPath());

        inboxNotificationSender.sendInboxNotification(resourceResolver, inboxNotification);
    }

    /**
     * @param inboxNotificationSender
     * @param resourceResolver
     * @param contentPath
     * @param title
     * @param message
     * @param principal
     * @throws TaskManagerException
     */
    public static void sendInboxNotification(final InboxNotificationSender inboxNotificationSender,
            final ResourceResolver resourceResolver, final String contentPath, final String title, final String message,
            final String principal, List<String> approvedRejectedContentList) throws TaskManagerException {
        if (null == inboxNotificationSender || null == resourceResolver || null == contentPath) {
            return;
        }

        LOG.debug("Content Path" + contentPath);
        final InboxNotification inboxNotification = inboxNotificationSender.buildInboxNotification();
        inboxNotification.setTitle(title);
        inboxNotification.setAssignee(principal);
        StringBuilder contentList = new StringBuilder();
        for (String approvedRejectedContent :
            approvedRejectedContentList) {
            contentList = contentList.append(approvedRejectedContent).append(";   ");
        }
        /* Adding approved/rejected content list to inbox description */
        inboxNotification.setMessage(message + "    -    " +  contentList.toString());
        inboxNotification.setContentPath(contentPath);

        LOG.debug("Send notification to" + principal);

        inboxNotificationSender.sendInboxNotification(resourceResolver, inboxNotification);
    }

    /**
     * Set syndication status for the given subscriber in the configuration
     *
     * @param subscriberResource
     * @param status
     * @throws PersistenceException
     */
    public static void setSyndicationStatus(final Resource subscriberResource, final boolean status)
            throws PersistenceException {

        final ModifiableValueMap valueMap = subscriberResource.adaptTo(ModifiableValueMap.class);
        valueMap.put(MadisonConstants.PN_IS_SYNDICATED, status);

        subscriberResource.getResourceResolver().commit();
    }

    /**
     * Return source node from conf for a given resource path
     *
     * @param resourcePath
     * @param resourceResolver
     * @return
     */
    public static Resource getSourceNode(final String resourcePath, final ResourceResolver resourceResolver) {
        final List<String> syndicatedSubscribers = new ArrayList<>();

        final String confPath = MadisonConstants.CONF_SYNDICATION_SETTINGS_ROOT;
        final Resource syndicationConfRoot = resourceResolver.getResource(confPath);

        final Iterator<Resource> syndicationSources = syndicationConfRoot != null ? syndicationConfRoot.listChildren() : null;
        if (syndicationSources != null) {
            while (syndicationSources.hasNext()) {
                final Resource syndicationSource = syndicationSources.next();
                final String sourcePath = syndicationSource.getValueMap().get("sourcePath", String.class);

                if (resourcePath.equals(sourcePath)) {
                    return syndicationSource;
                }
            }
        }
        return null;
    }
}
