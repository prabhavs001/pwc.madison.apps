package com.pwc.madison.core.services.impl;

import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.notifications.InboxNotification;
import com.adobe.acs.commons.notifications.InboxNotificationSender;
import com.adobe.granite.taskmanagement.TaskManagerException;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveActionFactory;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.commons.BaseAction;
import com.day.cq.wcm.msm.commons.BaseActionFactory;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.LiveCopyConfigurationProvider;
import com.pwc.madison.core.util.MadisonUtil;

@Component(
        immediate = true,
        service = LiveActionFactory.class,
        property = { LiveActionFactory.LIVE_ACTION_NAME + "=" + SendLiveCopyNotification.LIVE_ACTION_CLASS_NAME,
                LiveActionFactory.LIVE_ACTION_NAME + "=" + SendLiveCopyNotification.LIVE_ACTION_NAME })
public class SendLiveCopyNotification
        extends BaseActionFactory<SendLiveCopyNotification.SendLiveCopyNotificationAction> {

    public static final String LIVE_ACTION_CLASS_NAME = "SendLiveCopyNotificationAction";
    public static final String LIVE_ACTION_NAME = "SendLiveCopyNotification";

    public static final String TYPE_PROPERTY_NAME = "type";
    public static final String TITLE_PROPERTY_NAME = "title";
    public static final String MESSAGE_PROPERTY_NAME = "message";

    public static final String TOPIC_NAME_PLACEHOLDER = "<Topic Name>";
    public static final String DESTINATION_FOLDER_PATH_PLACEHOLDER = "<Destination Folder Path>";
    public static final String SOURCE_FOLDER_PATH_PLACEHOLDER = "<Source Folder Path>";

    @Reference
    InboxNotificationSender inboxNotificationSender;

    @Reference
    LiveCopyConfigurationProvider liveCopyConfigurationProvider;

    @Override
    protected SendLiveCopyNotificationAction newActionInstance(ValueMap valueMap) throws WCMException {
        return new SendLiveCopyNotificationAction(valueMap, this, inboxNotificationSender,
                liveCopyConfigurationProvider);
    }

    @Override
    public String createsAction() {
        return LIVE_ACTION_NAME;
    }

    /**
     * 
     * Action that is called if used in any rollout configuration. Responsible to send notifications with title given by
     * title property name and message given by message property name and type given by type property name on the action
     * node.
     * 
     */
    class SendLiveCopyNotificationAction extends BaseAction {

        private final Logger LOGGER = LoggerFactory.getLogger(SendLiveCopyNotificationAction.class);

        private final String MESSAGE_TYPE = this.getConfig().get(TYPE_PROPERTY_NAME, StringUtils.EMPTY);
        private final String TITLE = this.getConfig().get(TITLE_PROPERTY_NAME, StringUtils.EMPTY);
        private final String MESSAGE = this.getConfig().get(MESSAGE_PROPERTY_NAME, StringUtils.EMPTY);

        private InboxNotificationSender inboxNotificationSender;
        private LiveCopyConfigurationProvider liveCopyConfigurationProvider;

        protected SendLiveCopyNotificationAction(ValueMap configuration, BaseActionFactory factory,
                InboxNotificationSender inboxNotificationSender,
                LiveCopyConfigurationProvider liveCopyConfigurationProvider) {
            super(configuration, factory);
            this.inboxNotificationSender = inboxNotificationSender;
            this.liveCopyConfigurationProvider = liveCopyConfigurationProvider;
        }

        @Override
        protected boolean handles(Resource source, Resource target, LiveRelationship relation, boolean resetRollout)
                throws RepositoryException, WCMException {
            return target != null && relation.getStatus().isPage();
        }

        @Override
        protected void doExecute(Resource source, Resource target, LiveRelationship relation, boolean resetRollout)
                throws RepositoryException, WCMException {
            final ResourceResolver resolver = target.getResourceResolver();
            final String topicName = target.getValueMap().get(JcrConstants.JCR_TITLE, String.class);
            final String folderPath = relation.getLiveCopy().getPath();
            final String sourceFolderPath = relation.getLiveCopy().getBlueprintPath();
            final String territoryCode = MadisonUtil.getTerritoryCodeForPath(target.getPath());
            final String[] authorizables = liveCopyConfigurationProvider
                    .getNotificationAuthorizablesByTerritory(territoryCode);
            if (null == authorizables || authorizables.length == 0) {
                LOGGER.error(
                        "SendLiveCopyNotificationAction doExecute() : No Notification Groups/Users configured for territory code {}",
                        territoryCode);
                return;
            }
            final InboxNotification inboxNotification = inboxNotificationSender.buildInboxNotification();
            inboxNotification.setTitle(getReplacedString(TITLE, topicName, folderPath, sourceFolderPath));
            inboxNotification.setMessage(getReplacedString(MESSAGE, topicName, folderPath, sourceFolderPath));
            inboxNotification.setContentPath(target.getPath()
                    .replace(MadisonConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT, StringUtils.EMPTY));
            for (final String authorizable : authorizables) {
                inboxNotification.setAssignee(authorizable);
                try {
                    inboxNotificationSender.sendInboxNotification(resolver, inboxNotification);
                } catch (TaskManagerException e) {
                    LOGGER.error(
                            "SendLiveCopyNotificationAction doExecute() : Notification sent failed with  Exception {} for target path {} for Type "
                                    + MESSAGE_TYPE + " for user/group id " + authorizable,
                            e, target.getPath());
                }
            }

        }

        /**
         * Return the modified string by replacing {@value SendLiveCopyNotification#TOPIC_NAME_PLACEHOLDER} by given
         * topicName, {@value SendLiveCopyNotification#SOURCE_FOLDER_PATH_PLACEHOLDER} by given sourceFolderPathand
         * {@value SendLiveCopyNotification#DESTINATION_FOLDER_PATH_PLACEHOLDER} by given folderPath.
         * 
         * @param string
         *            {@link String}
         * @param topicName
         *            {@link String}
         * @param folderPath
         *            {@link String}
         * @param sourceFolderPath
         *            {@link String}
         * @return {@link String}
         */
        private String getReplacedString(final String string, final String topicName, final String folderPath,
                final String sourceFolderPath) {
            return string.replace(TOPIC_NAME_PLACEHOLDER, topicName)
                    .replace(DESTINATION_FOLDER_PATH_PLACEHOLDER, folderPath)
                    .replace(SOURCE_FOLDER_PATH_PLACEHOLDER, sourceFolderPath);
        }

    }
}
