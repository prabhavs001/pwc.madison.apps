package com.pwc.madison.core.workflows;

import com.adobe.acs.commons.notifications.InboxNotificationSender;
import com.adobe.granite.taskmanagement.TaskManagerException;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.commons.Externalizer;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.search.QueryBuilder;
import com.google.gson.JsonObject;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.PublishDitamapService;
import com.pwc.madison.core.util.DITALinkUtils;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.PublishingUtil;
import com.pwc.madison.core.util.SyndicationUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Component(service = WorkflowProcess.class, property = { "process.label=Madison - Auto-Publish Process" })
public class AutoPublishProcess implements WorkflowProcess {

    private static final String AEMSITE = "aemsite";
    public static final String INITIATOR = "initiator";
    private static final String PN_DESTINATION_PATH = "destinationPath";
    public static final String AUTO_PUBLISH_MODE = "autoPublishMode";
    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Reference
    private PublishDitamapService publishDitamapService;

    @Reference
    private Externalizer externalizer;
    @Reference
    private QueryBuilder queryBuilder;
    @Reference
    transient InboxNotificationSender inboxNotificationSender;

    private static final Logger LOG = LoggerFactory.getLogger(AutoPublishProcess.class);

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        LOG.debug("Starting with Auto-Publish Process");

        final MetaDataMap metadataMap = workItem.getWorkflowData().getMetaDataMap();

        final ResourceResolver resolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                MadisonConstants.SYNDICATION_SERVICE_USER);

        final Resource payload;
        if (!workItem.getWorkflowData().getPayloadType().equals(MadisonConstants.PN_JCR_PATH)) {
            payload = null;
        }else {
            final String path = workItem.getWorkflowData().getPayload().toString();
            payload = resolver.getResource(path);
        }
        if (payload == null) {
            LOG.error("Empty payload");
            return;
        }

        Resource sourceNode = SyndicationUtil.getSourceNode(Objects.requireNonNull(payload.getParent()).getPath(), resolver);
        if(sourceNode != null && sourceNode.hasChildren()){
            Iterator<Resource> sourceResourceIterator = sourceNode.listChildren();
            List<String> publishingPoints = new ArrayList<String>();
            while (sourceResourceIterator.hasNext()){
                Resource subscriber = sourceResourceIterator.next();
                ValueMap subscribingNodeProperties = subscriber.getValueMap();
                if(subscribingNodeProperties.containsKey(AUTO_PUBLISH_MODE) && subscribingNodeProperties.get(AUTO_PUBLISH_MODE, Boolean.class)) {
                    String subscribingTerritoryPath = subscribingNodeProperties.get(PN_DESTINATION_PATH, String.class);
                    String subscribingPublishingPoint = subscribingTerritoryPath + MadisonConstants.FORWARD_SLASH + payload.getPath().substring(payload.getPath().lastIndexOf("/") + 1);
                    publishingPoints.add(subscribingPublishingPoint);
                }
            }

            // Check Permission is disabled for now, as there is no requirement at the moment.
//            boolean hasPermission = PublishingUtil.checkIfUserHasPermission(resolver, publishingPoints.get(0), PublishingUtil.WRITE);
//            if(hasPermission) {
            LOG.debug("Initiator of Auto-Publishing is :: "+ metadataMap.get(INITIATOR, String.class));
            boolean alreadyInPublishingQueue = PublishingUtil.checkIfSourceIsAlreadyInPublishingQueue(
                    sourceNode.getValueMap().get(DITAConstants.SOURCE_PATH, String.class), resolver);
            Session session = null;
            try {
                session = resolver.adaptTo(Session.class);
                if (!alreadyInPublishingQueue && !publishingPoints.isEmpty()) {
                    String ongoingPublishingPath = publishingPoints.get(0);
                    try {
                        PublishingUtil.createEntryInAutoPublishingQueue(sourceNode.getName(), session);
                    } catch (RepositoryException e) {
                        LOG.error("Repository Exception in AutoPublish Process - createEntryInAutoPublishingQueue() :: "+ e);
                        final UserManager userManager = resolver.adaptTo(UserManager.class);
                        Authorizable initiatorAuthorizable = userManager.getAuthorizable(metadataMap.get(INITIATOR, String.class));
                        String sourcePath = sourceNode.getValueMap().get(DITAConstants.SOURCE_PATH, String.class);
                        String title = "Auto-Publish Failed";
                        String message = String.format("Auto-publish failed for source path (%s). Check syndication logs.", sourcePath);
                        SyndicationUtil.sendInboxNotification(inboxNotificationSender,resolver, resolver.getResource(sourcePath), title, message, initiatorAuthorizable.getID());
                    }
                    JsonObject publishingStatusJsonObject = createDefaultPublishingStatusJsonObject(publishingPoints);
                    publishingStatusJsonObject.addProperty(ongoingPublishingPath, MadisonConstants.IN_PROGRESS);

                    addPublishingProperties(sourceNode, ongoingPublishingPath, publishingStatusJsonObject,
                            publishingPoints, resolver);

                    publishDitamapService.initiatePublishingProcess(ongoingPublishingPath, resolver, AEMSITE, externalizer);
                }else{
                    LOG.info("Source Node {} already in publishing queue", sourceNode);
                    final UserManager userManager = resolver.adaptTo(UserManager.class);
                    try {
                        Authorizable initiatorAuthorizable = userManager.getAuthorizable(metadataMap.get(INITIATOR, String.class));
                        String sourcePath = sourceNode.getValueMap().get(DITAConstants.SOURCE_PATH, String.class);
                        String title = "Auto-Publish - Source Path already in Publishing Queue";
                        String message = String.format("Source Path already in Publishing queue, hence not starting publishing.");
                        SyndicationUtil.sendInboxNotification(inboxNotificationSender,resolver, resolver.getResource(sourcePath), title, message, initiatorAuthorizable.getID());
                    } catch (RepositoryException e) {
                        LOG.error("Repository Exception in AutoPublish Process :: "+ e);
                    }
                }
            } catch(TaskManagerException | RepositoryException e){
                LOG.error("Exception in AutoPublish Process :: "+ e);
            } finally {
                if (session != null)
                    session.logout();
            }
        }
    }

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

    private void addPublishingProperties(final Resource sourceNode, final String ongoingPublishingPath,
                                         final JsonObject publishingStatus, List<String> publishingPoints,
                                         final ResourceResolver resolver){
        Session session = null;
        try {
            session = resolver.adaptTo(Session.class);

            // Get the AUTO_PUBLISHING_QUEUE_PATH node
            Node autoPublishingQueueNode = null;
            if (session != null) {
                autoPublishingQueueNode = session.getNode(MadisonConstants.AUTO_PUBLISHING_QUEUE_PATH+MadisonConstants.FORWARD_SLASH+
                        sourceNode.getName());
            }
            if (autoPublishingQueueNode != null) {
                autoPublishingQueueNode.setProperty(DITAConstants.SOURCE_PATH, sourceNode.getValueMap().get(DITAConstants.SOURCE_PATH, String.class));
                autoPublishingQueueNode.setProperty(MadisonConstants.ONGOING_PUBLISHING, ongoingPublishingPath);
                autoPublishingQueueNode.setProperty(MadisonConstants.PN_PUBLISHING_STATUS, publishingStatus.toString());
                autoPublishingQueueNode.setProperty(MadisonConstants.PUBLISHING_POINTS, createPublishingPointValueArray(session, publishingPoints));
            }

            if (session != null) {
                session.save();
            }
        } catch (Exception e){
            LOG.error("Exception while adding node to Auto-Publishing Queue :: "+ e);
        }
    }

    private JsonObject createDefaultPublishingStatusJsonObject(List<String> publishingPoints) {
        JsonObject publishingStatusJsonObject = new JsonObject();
        for (String point : publishingPoints) {
            publishingStatusJsonObject.addProperty(point, MadisonConstants.PENDING);
        }
        return publishingStatusJsonObject;
    }


    private Value[] createPublishingPointValueArray (final Session session, final List<String> publishingPoints){

        ValueFactory valueFactory = null;
        try {
            if (session != null) {
                valueFactory = session.getValueFactory();
            }

            Value[] values = new Value[publishingPoints.size()];
            if (valueFactory != null) {
                for (int i = 0; i < publishingPoints.size(); i++) {
                    values[i] = valueFactory.createValue(publishingPoints.get(i));
                }
            }
            return values;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
