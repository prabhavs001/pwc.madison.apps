package com.pwc.madison.core.workflows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
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
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MailSenderService;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.SyndicationUtil;

/**
 * This is a Process Step to notify users/groups regarding local content update. The references of the associated topics
 * (in workflow), are found within same territory and language and the users/groups which are assigned to the reference
 * folder are notified.
 *
 */
@Component(service = WorkflowProcess.class, property = { "process.label=Madison - Local Content Update Notification" })
public class LocalContentUpdateNotificationProcess implements WorkflowProcess {

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    private MailSenderService mailSenderService;

    @Reference
    private InboxNotificationSender inboxNotificationSender;

    private static final Logger LOG = LoggerFactory.getLogger(LocalContentUpdateNotificationProcess.class);

    /*
     * (non-Javadoc)
     *
     * @see com.adobe.granite.workflow.exec.WorkflowProcess#execute(com.adobe.granite. workflow.exec.WorkItem,
     * com.adobe.granite.workflow.WorkflowSession, com.adobe.granite.workflow.metadata.MetaDataMap)
     */
    @Override
    public void execute(final WorkItem workItem, final WorkflowSession workflowSession, final MetaDataMap metaDataMap)
            throws WorkflowException {

        LOG.debug("Local Content Update Notification");

        final MetaDataMap metadataMap = workItem.getWorkflowData().getMetaDataMap();

        final String payload = (String) workItem.getWorkflowData().getPayload();

        final String inboxNotificationMessage = metadataMap.get("description").toString();

        final String topics = metadataMap.get(DITAConstants.ORIGINAL_TOPICS, String.class);
        final String[] topicsList = StringUtils.isNotBlank(topics) ? topics.split("\\|") : null;

        if (null == topicsList || topicsList.length < 1) {
            LOG.error("No topics present");
            return;
        }

        final ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                MadisonConstants.SYNDICATION_SERVICE_USER);
        final List<String> mapRefsToBeExcluded = new ArrayList<>();
        mapRefsToBeExcluded.add(payload);
        final Resource payloadResource = resourceResolver.getResource(payload);
        if (payloadResource != null) {
            getMapRefsToBeExcluded(mapRefsToBeExcluded, payloadResource, resourceResolver);
        }

        for (final String topic : topicsList) {
            try {
                final Resource ditaTopic = resourceResolver.getResource(topic);
                final StringBuilder sb = new StringBuilder(topic + DITAConstants.FORWARD_SLASH
                        + JcrConstants.JCR_CONTENT + DITAConstants.FORWARD_SLASH + DITAConstants.METADATA_NAME);
                final Resource ditaMetaResource = resourceResolver.getResource(sb.toString());

                if (null == ditaTopic || null == ditaMetaResource) {
                    continue;
                }
                getReferencesAndNotify(ditaTopic, resourceResolver, inboxNotificationMessage, mapRefsToBeExcluded);
            } catch (final Exception e) {
                LOG.error("Error in local content update process:: ", e);
            }
        }
        if (resourceResolver.isLive()) {
            resourceResolver.close();
        }
    }

    /**
     * Gets the dita map references to be excluded.
     *
     * @param mapRefsToBeExcluded
     *            the map refs to be excluded
     * @param payloadResource
     *            the payload resource
     * @param resourceResolver
     *            the resource resolver
     * @return the map refs to be excluded
     */
    private void getMapRefsToBeExcluded(final List<String> mapRefsToBeExcluded, final Resource payloadResource,
            final ResourceResolver resourceResolver) {
        final Resource resourceContent = payloadResource.getChild(JcrConstants.JCR_CONTENT);
        if (resourceContent != null) {
            final ValueMap resourceProps = resourceContent.adaptTo(ValueMap.class);
            if (resourceProps != null && resourceProps.containsKey(DITAConstants.PN_FMDITATOPICREFS)) {
                final String[] topicRefs = resourceProps.get("fmditaTopicrefs", String[].class);
                for (String topicRef : topicRefs) {
                    if (topicRef.endsWith(DITAConstants.DITAMAP_EXT)) {
                        topicRef = topicRef.replace(MadisonConstants.COMMA_SEPARATOR, "");
                        mapRefsToBeExcluded.add(topicRef);
                        final Resource topicRefResource = resourceResolver.getResource(topicRef);
                        if(null == topicRefResource){
                            continue;
                        }
                        getMapRefsToBeExcluded(mapRefsToBeExcluded, topicRefResource, resourceResolver);
                    }
                }
            }
        }
    }

    /**
     * Gets the references and notify.
     *
     * @param ditaTopic
     *            the dita topic
     * @param resourceResolver
     *            the resource resolver
     * @param inboxNotificationMessage
     *            the inbox notification message
     * @param mapRefsToBeExcluded
     *            the map refs to be excluded
     * @return the references and notify
     */
    private void getReferencesAndNotify(final Resource ditaTopic, final ResourceResolver resourceResolver,
            final String inboxNotificationMessage, final List<String> mapRefsToBeExcluded) {
        final Iterator<Resource> ditaReferences = getReferences(ditaTopic, resourceResolver);
        if (ditaReferences != null) {
            final List<String> sourceFolders = new ArrayList<>();
            final Set<Authorizable> authorizables = new HashSet<>();
            while (ditaReferences.hasNext()) {
                final Resource ditaReference = ditaReferences.next();
                if (ditaReference.getPath().endsWith(DITAConstants.DITAMAP_EXT)
                        && mapRefsToBeExcluded.contains(ditaReference.getPath())) {
                    continue;
                }
                final Resource ditaReferenceContent = ditaReference.getChild(JcrConstants.JCR_CONTENT);
                if (null != ditaReferenceContent) {
                    final ValueMap ditaRefProps = ditaReferenceContent.adaptTo(ValueMap.class);
                    validateReferenceAndAddMembers(resourceResolver, ditaTopic, ditaReference, ditaRefProps,
                            authorizables, sourceFolders);
                }
            }
            sendNotifications(resourceResolver, ditaTopic, authorizables, inboxNotificationMessage);
        }

    }

    /**
     * Validate reference and add members.
     *
     * @param resourceResolver
     *            the resource resolver
     * @param ditaTopic
     *            the dita topic
     * @param ditaReference
     *            the dita reference
     * @param ditaRefProps
     *            the dita ref props
     * @param authorizables
     *            the authorizables
     * @param sourceFolders
     *            the source folders
     */
    private void validateReferenceAndAddMembers(final ResourceResolver resourceResolver, final Resource ditaTopic,
            final Resource ditaReference, final ValueMap ditaRefProps, final Set<Authorizable> authorizables,
            final List<String> sourceFolders) {
        if (ditaRefProps != null && isValidReference(resourceResolver, ditaTopic, ditaReference, ditaRefProps)) {
            // If the references from the query are within same folder, select only one valid reference from the folder.
            // This is because we are notifying users/groups based on folder and not based on the reference file.
            final Resource ditaReferenceParent = ditaReference.getParent();
            if (ditaReferenceParent != null && !sourceFolders.contains(ditaReferenceParent.getPath())) {
                sourceFolders.add(ditaReferenceParent.getPath());
                final Set<Authorizable> members = getMembersToBeNotified(ditaReference, resourceResolver);
                if (null != members) {
                    authorizables.addAll(members);
                }
            }
        }

    }

    /**
     * Gets the members to be notified.
     *
     * @param ditaReference
     *            the dita reference
     * @param resourceResolver
     *            the resource resolver
     * @return the members to be notified
     */
    private Set<Authorizable> getMembersToBeNotified(final Resource ditaReference,
            final ResourceResolver resourceResolver) {
        try {
            // parent author group (<territory>-madison-author)
            final String territoryAuthorGroupName = MadisonUtil.getTerritoryCodeForPath(ditaReference.getPath()) + "-"
                    + MadisonConstants.MADISON_PUBLISHER;

            // get all the territory author groups
            return SyndicationUtil.getTerritoryGroups(ditaReference, resourceResolver, territoryAuthorGroupName);

        } catch (final RepositoryException e) {
            LOG.error("Local Content Update Notification Error  ", e);
        }
        return new HashSet<>();

    }

    /**
     * Checks if reference is valid.
     *
     * @param resourceResolver
     *            the resource resolver
     * @param ditaTopic
     *            the dita topic
     * @param ditaReference
     *            the dita reference
     * @param ditaRefProps
     *            the dita ref props
     * @return true, if is valid reference
     */
    private boolean isValidReference(final ResourceResolver resourceResolver, final Resource ditaTopic,
            final Resource ditaReference, final ValueMap ditaRefProps) {
        if (ditaRefProps.containsKey(DITAConstants.PN_FMDITATOPICREFS)) {
            final String[] topicRefs = ditaRefProps.get(DITAConstants.PN_FMDITATOPICREFS, String[].class);
            for (final String topicRef : topicRefs) {
                if (topicRef.equals("," + ditaTopic.getPath())) {
                    return true;
                }
            }
        }

        return isConrefValid(resourceResolver, ditaTopic, ditaReference, ditaRefProps);
    }

    /**
     * Checks if conref is valid.
     *
     * @param resourceResolver
     *            the resource resolver
     * @param ditaTopic
     *            the dita topic
     * @param ditaReference
     *            the dita reference
     * @param ditaRefProps
     *            the dita ref props
     * @return true, if is conref valid
     */
    private boolean isConrefValid(final ResourceResolver resourceResolver, final Resource ditaTopic,
            final Resource ditaReference, final ValueMap ditaRefProps) {
        if (ditaRefProps.containsKey(DITAConstants.PN_FMDITA_CONREF)) {
            final String[] conRefs = ditaRefProps.get(DITAConstants.PN_FMDITA_CONREF, String[].class);
            for (final String conRef : conRefs) {
                final String fullConrefPath = getFullConrefPath(conRef, ditaReference, resourceResolver);
                if (null != fullConrefPath && fullConrefPath.equals(ditaTopic.getPath())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets the full conref path.
     *
     * @param conRef
     *            the con ref
     * @param ditaReference
     *            the dita reference
     * @param resourceResolver
     *            the resource resolver
     * @return the full conref path
     */
    private String getFullConrefPath(String conRef, final Resource ditaReference,
            final ResourceResolver resourceResolver) {
        if (conRef.contains("#")) {
            conRef = conRef.substring(1, conRef.indexOf('#'));
        }
        if (conRef.contains("..") || !conRef.contains("/")) {
            final Resource referenceResource = resourceResolver.getResource(ditaReference.getParent(), conRef);
            if (referenceResource != null) {
                return referenceResource.getPath();
            }
        }
        return null;
    }

    /**
     * Send notifications.
     *
     * @param resourceResolver
     *            the resource resolver
     * @param sourceTopic
     *            the source topic
     * @param authorizables
     *            the authorizables
     * @param inboxNotificationMessage
     *            the inbox notification message
     */
    private void sendNotifications(final ResourceResolver resourceResolver, final Resource sourceTopic,
            final Set<Authorizable> authorizables, String inboxNotificationMessage) {

        try {
            final Map<String, String> emailParams = new HashMap<>();
            final String title = sourceTopic.getName();
            emailParams.put("title", title);
            emailParams.put("source", MadisonUtil.getTerritoryCodeForPath(sourceTopic.getPath()));
            final String subject = title + " has been updated and is now available";
            final String territoryCode = MadisonUtil.getTerritoryCodeForPath(sourceTopic.getPath());
            if (StringUtils.isBlank(territoryCode)) {
                return;
            }

            if (null == authorizables || authorizables.isEmpty()) {
                LOG.error("Local Content Update -  no authors configured for territory::: {}", sourceTopic.getPath());
                return;
            }

            if (StringUtils.isBlank(inboxNotificationMessage)) {
                inboxNotificationMessage = subject;
            }

            for (final Authorizable group : authorizables) {
                if (group.isGroup()) {
                    SyndicationUtil.sendInboxNotification(inboxNotificationSender, resourceResolver, sourceTopic,
                            "Local Content Update - " + title, inboxNotificationMessage, group.getID());
                }
            }

            /*SyndicationUtil.sendSyndicationEmailNotification(MadisonConstants.LOCAL_CONTENT_UPDATE_TEMPLATE, subject,
                    emailParams, mailSenderService, authorizables);*/
        } catch (final RepositoryException | TaskManagerException e) {
            LOG.error("Local Content Update Notification Error  ", e);
        }

    }

    /**
     * Gets the references.
     *
     * @param ditaTopic
     *            the dita topic
     * @param resourceResolver
     *            the resource resolver
     * @return the references
     */
    private Iterator<Resource> getReferences(final Resource ditaTopic, final ResourceResolver resourceResolver) {
        final Node ditaNode = ditaTopic.adaptTo(Node.class);
        try {
            if (ditaNode != null && ditaNode.getAncestor(6) != null) {
                final String territoryLanguagePath = ditaNode.getAncestor(6).getPath();
                final Map<String, Object> predicateMap = new HashMap<>();
                predicateMap.put("path", territoryLanguagePath);
                predicateMap.put("type", "dam:Asset");
                predicateMap.put("group.p.or", "true");
                predicateMap.put("group.1_property", "jcr:content/@fmditaTopicrefs");
                predicateMap.put("group.1_property.value", "," + ditaTopic.getPath());
                predicateMap.put("group.2_property", "jcr:content/@fmditaConrefs");
                predicateMap.put("group.2_property.value", "%" + ditaTopic.getName() + "%");
                predicateMap.put("group.2_property.operation", "like");
                predicateMap.put("p.limit", "-1");

                final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap),
                        resourceResolver.adaptTo(Session.class));
                final SearchResult searchResult = query.getResult();
                LOG.debug("searchResult size::: {}", searchResult.getHits().size());

                return searchResult.getResources();

            }
        } catch (final RepositoryException e) {
            LOG.error("Error while accessing dita");
        }
        return null;
    }

}
