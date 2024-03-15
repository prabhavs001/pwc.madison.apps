package com.pwc.madison.core.workflows;

import static com.pwc.madison.core.constants.MadisonConstants.PN_IS_SYNDICATED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
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
import com.pwc.madison.core.services.SyndicationService;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.SyndicationUtil;

@Component(service = WorkflowProcess.class, property = { "process.label=Madison - Syndication Content Update" })
public class SyndicationContentUpdateProcess implements WorkflowProcess {

    @Reference
    SyndicationService syndicationService;
    @Reference
    ResourceResolverFactory resourceResolverFactory;
    @Reference
    private QueryBuilder queryBuilder;
    @Reference
    private MailSenderService mailSenderService;
    @Reference
    private InboxNotificationSender inboxNotificationSender;

    final Logger LOG = LoggerFactory.getLogger(SyndicationContentUpdateProcess.class);

    @Override
    public void execute(final WorkItem workItem, final WorkflowSession workflowSession, final MetaDataMap metaDataMap)
            throws WorkflowException {

        final MetaDataMap dataMap = workItem.getWorkflowData().getMetaDataMap();

        final String inboxNotificationMessage = dataMap.get("description").toString();

        final String topics = dataMap.get(DITAConstants.ORIGINAL_TOPICS, String.class);
        final String[] topicsList = StringUtils.isNotBlank(topics) ? topics.split("\\|") : null;

        List<String> referencedDitaMapsList = Collections.EMPTY_LIST;
        final String refDitaMaps = dataMap.get(DITAConstants.REVIEW_DITAMAPS, String.class);
        if (null != refDitaMaps && !refDitaMaps.isEmpty()) {
            final String[] refDitaMapsArray = refDitaMaps.split("\\|");
            referencedDitaMapsList = Arrays.asList(refDitaMapsArray);
        }

        if (null == topicsList || topicsList.length < 1) {
            LOG.error("No topics present for syndication update");
            return;
        }

        final ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                MadisonConstants.SYNDICATION_SERVICE_USER);
        try {
            // check if the folder is syndicated
            final String topic = topicsList[0];
            Resource dita = resourceResolver.getResource(topic);

            final Resource syndicationSource = SyndicationUtil.checkIfSourceIsSyndicated(dita.getParent());

            // folder not syndicated. ignore
            if (null == syndicationSource) {
                return;
            }

            final Set<String> toBeSyndicatedAssets = new LinkedHashSet<>();
            final Set<Resource> toBeUpdatedAssets = new LinkedHashSet<>();

            for (final String element : topicsList) {

                dita = resourceResolver.getResource(element);
                final StringBuilder sb = new StringBuilder(element + DITAConstants.FORWARD_SLASH
                        + JcrConstants.JCR_CONTENT + DITAConstants.FORWARD_SLASH + DITAConstants.METADATA_NAME);
                final Resource ditaMetaResource = resourceResolver.getResource(sb.toString());

                if (null == dita || null == ditaMetaResource) {
                    continue;
                }

                boolean isSyndicated = false;

                if (ditaMetaResource.getValueMap().containsKey(PN_IS_SYNDICATED)) {
                    isSyndicated = ditaMetaResource.getValueMap().get(PN_IS_SYNDICATED, Boolean.class);
                }
                // new asset. need to syndicate
                if (!isSyndicated) {
                    toBeSyndicatedAssets.add(dita.getPath());
                } else {
                    toBeUpdatedAssets.add(dita);
                }
            }

            // if there are any new topics, syndicate the corresponding dita maps along with the topics
            if (!toBeSyndicatedAssets.isEmpty()) {
                toBeSyndicatedAssets
                        .addAll(getUpdatedDitaMaps(toBeSyndicatedAssets, syndicationSource, resourceResolver));
            }

            // syndicate the dita maps which have structural changes
            toBeSyndicatedAssets.addAll(referencedDitaMapsList);

            // no assets to syndicate. no updates
            if (toBeSyndicatedAssets.isEmpty() && toBeUpdatedAssets.isEmpty()) {
                return;
            }

            LOG.debug("To be syndicated assets are :: {} ",toBeSyndicatedAssets);
            LOG.debug("To be updated assets are :: {}",toBeUpdatedAssets);
            
            final List<Resource> syndicatedTerritories = SyndicationUtil
                    .getSyndicatedSubscribers(syndicationSource.getPath(), resourceResolver);
            LOG.debug("No of syndicated teriories are {} ",syndicatedTerritories.size());
            
            for (final Resource territory : syndicatedTerritories) {
            	LOG.debug("Syndicating for this territory {} ",territory.getPath());
            	syndicationService.processSyndication(toBeSyndicatedAssets, syndicationSource, territory);
               LOG.debug("Syndication has been done for this teritory, now Sending notification");
                sendSyndicationNotifications(resourceResolver, syndicationSource, territory, inboxNotificationMessage);
                LOG.debug("Notifications has been sent, coping non editable metadat of tobeUpdated assets");
                syndicationService.copyNonEditableMetadata(toBeUpdatedAssets, syndicationSource, territory,resourceResolver);
                LOG.debug("NonEditableMetadata has been copied");
            }

        } catch (final Exception e) {
            LOG.error("Error in update syndication process:: ", e);
        } finally {
            if (null != resourceResolver && resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }

    }

    private void sendSyndicationNotifications(final ResourceResolver resourceResolver, final Resource syndicationSource,
            final Resource territory, String inboxNotificationMessage) {

        try {
            final Map<String, String> emailParams = new HashMap<>();
            final String title = territory.getName();
            emailParams.put("title", title);
            emailParams.put("source", MadisonUtil.getTerritoryCodeForPath(syndicationSource.getPath()));
            final String subject = title + " has been updated and is now available";
            final String territoryCode = MadisonUtil.getTerritoryCodeForPath(territory.getPath());
            if (StringUtils.isBlank(territoryCode)) {
                return;
            }

            // parent author group (<territory>-madison-author)
            final String territoryAuthorGroupName = MadisonUtil.getTerritoryCodeForPath(territory.getPath()) + "-"
                    + MadisonConstants.MADISON_PUBLISHER;

            // get all the territory author groups
            final Set<Authorizable> authorizables = SyndicationUtil.getTerritoryGroups(territory, resourceResolver,
                    territoryAuthorGroupName);

            if (null == authorizables || authorizables.isEmpty()) {
                LOG.error("sendSyndicationNotifications no authors configured for territory::: {}",
                        territory.getPath());
                return;
            }

            if (StringUtils.isBlank(inboxNotificationMessage)) {
                inboxNotificationMessage = subject;
            }

            for (final Authorizable group : authorizables) {
                if (group.isGroup()) {
                    SyndicationUtil.sendInboxNotification(inboxNotificationSender, resourceResolver, territory, title,
                            inboxNotificationMessage, group.getID());
                }
            }

            /*SyndicationUtil.sendSyndicationEmailNotification(MadisonConstants.SYNDICATION_UPDATE_EMAIL_TEMPLATE,
                    subject, emailParams, mailSenderService, authorizables);*/
        } catch (final RepositoryException | TaskManagerException e) {
            LOG.error("sendSyndicationNotifications Error  ", e);
        }

    }

    private Set<String> getUpdatedDitaMaps(final Set<String> toBeSyndicatedAssets, final Resource syndicationSource,
            final ResourceResolver resourceResolver) {
        final Set<String> resourcesSet = new HashSet<>();
        for (final String resource : toBeSyndicatedAssets) {
            final Iterator<Resource> ditaMaps = getDitaMap(resource, syndicationSource.getPath(), resourceResolver)
                    .getResources();
            while (ditaMaps.hasNext()) {
                final Resource res = ditaMaps.next();
                resourcesSet.add(res.getPath());
            }
        }
        return new HashSet<>(resourcesSet);
    }

    private SearchResult getDitaMap(final String resource, final String path, final ResourceResolver resourceResolver) {

        final Map<String, Object> predicateMap = new HashMap<>();
        predicateMap.put("path", path);
        predicateMap.put("type", "dam:Asset");
        predicateMap.put("property", "jcr:content/@fmditaTopicrefs");
        predicateMap.put("property.value", "%" + resource + "%");
        predicateMap.put("property.operation", "like");
        predicateMap.put("p.limit", "-1");

        final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap),
                resourceResolver.adaptTo(Session.class));
        final SearchResult searchResult = query.getResult();
        LOG.debug("searchResult size::: {}", searchResult.getHits().size());

        return searchResult;

    }

}
