package com.pwc.madison.core.schedulers;

import com.adobe.acs.commons.notifications.InboxNotification;
import com.adobe.acs.commons.notifications.InboxNotificationSender;
import com.adobe.granite.taskmanagement.TaskManagerException;
import com.day.cq.commons.Externalizer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pwc.madison.core.beans.GeneratedResponse;
import com.pwc.madison.core.beans.Output;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.AutoPublishDitamapSchedulerConfig;
import com.pwc.madison.core.services.PublishDitamapService;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.SyndicationUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.*;

@Component(service = Runnable.class, immediate = true)
@Designate(ocd = AutoPublishDitamapSchedulerConfig.class)
public class AutoPublishDitamapScheduler implements Runnable{

    public static final String PENDING = "pending";
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    @Reference
    private Scheduler scheduler;
    private int schedulerID;
    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    @Reference
    private PublishDitamapService publishDitamapService;
    @Reference
    private Externalizer externalizer;

    @Activate
    protected void activate(AutoPublishDitamapSchedulerConfig config) {
        schedulerID = config.schedulerName().hashCode();
        addScheduler(config);
    }

    @Modified
    protected void modified(AutoPublishDitamapSchedulerConfig config) {
        removeScheduler();
        schedulerID = config.schedulerName().hashCode(); // update schedulerID
        addScheduler(config);
    }

    @Deactivate
    protected void deactivate(AutoPublishDitamapSchedulerConfig config) {
        removeScheduler();
    }


    private void removeScheduler() {
        LOG.info("Removing Scheduler Job '{}'", schedulerID);
        scheduler.unschedule(String.valueOf(schedulerID));
    }

    /**
     * Add a scheduler based on the scheduler ID
     */
    private void addScheduler(AutoPublishDitamapSchedulerConfig config) {
        if (config.serviceEnabled()) {
            ScheduleOptions sopts = scheduler.EXPR(config.schedulerExpression());
            sopts.name(String.valueOf(schedulerID));
            scheduler.schedule(this, sopts);
            LOG.info("AutoPublishDitamapScheduler added succesfully. New Job ID : "+ schedulerID);
        } else {
            LOG.info("AutoPublishDitamapScheduler is Disabled, no scheduler job created");
        }
    }

    /**
     *
     */
    @Override
    public void run() {
        LOG.debug("Inside AutoPublishDitamapScheduler run Method");

        final ResourceResolver resolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                MadisonConstants.SYNDICATION_SERVICE_USER);
        Session session = resolver.adaptTo(Session.class);
        final Resource autoPublishingQueueResource = resolver.getResource(MadisonConstants.AUTO_PUBLISHING_QUEUE_PATH);

        // configuration not available
        if (null == autoPublishingQueueResource) {
            LOG.debug("Either Auto-Publishing Queue is not available or User does not have access");
                return;
        }

        final Iterator<Resource> allQueueNodes = autoPublishingQueueResource.listChildren();
        JsonParser parser = new JsonParser();
        while (allQueueNodes.hasNext()) {
            final Resource autoPublishResource = allQueueNodes.next();
            if (!(autoPublishResource.getName().equals(DITAConstants.REP_POLICY_NODE))) {
                Node autoPublishNode = autoPublishResource.adaptTo(Node.class);
                JsonObject publishingStatusJsonObj;
                try {
                    if (autoPublishNode != null && autoPublishNode.hasProperty(MadisonConstants.ONGOING_PUBLISHING)) {
                        String onGoingPublishingDitamap = autoPublishNode.getProperty(MadisonConstants.ONGOING_PUBLISHING).getString();

                        String json = publishDitamapService.fetchPublishingStatus(onGoingPublishingDitamap, MadisonConstants.OUTPUT_TYPE_AEMSITE, externalizer, resolver);
                        boolean siteGenerated = publishDitamapService.isSiteGenerated(json,MadisonConstants.OUTPUT_TYPE_AEMSITE, resolver);
                        if(Boolean.TRUE == siteGenerated) {
                            // If it reaches here, it means site generation successful.
                            // Set the doc status, revision and published date only.
                            publishDitamapService.generateRevisionAndSetDocState(onGoingPublishingDitamap, resolver);

                            // Fetch the total time to generate the site for the given ditamap.
                            Gson gson = new Gson();
                            GeneratedResponse response = gson.fromJson(json, GeneratedResponse.class);
                            List<Output> outputs = response.getOutputs();
                            if(null != outputs && outputs.size() > 0) {
                                Output generatedSite = outputs.get(0);

                                //updating PublishingStatus Map
                                String publishingStatusString = autoPublishNode.hasProperty(MadisonConstants.PN_PUBLISHING_STATUS) ?
                                        autoPublishNode.getProperty(MadisonConstants.PN_PUBLISHING_STATUS).getString() : null;

                                if(publishingStatusString!=null){

                                    // Update status in the Publishing
                                    publishingStatusJsonObj = parser.parse(publishingStatusString).getAsJsonObject();
                                    if(publishingStatusJsonObj.has(onGoingPublishingDitamap)) {
                                        publishingStatusJsonObj.addProperty(onGoingPublishingDitamap, DITAConstants.DITA_DOCUMENTSTATE_DONE);
                                        LOG.info("Auto-Publishing Status for {} :: {}", onGoingPublishingDitamap, DITAConstants.DITA_DOCUMENTSTATE_DONE);
                                        updatePropertyInNode(autoPublishNode, MadisonConstants.PN_PUBLISHING_STATUS, publishingStatusJsonObj.toString());
                                    }
                                    float time = generatedSite.getGeneratedIn()/1000;
                                    LOG.info(String.format("Total time of execution (in seconds) for %s :: %f", onGoingPublishingDitamap, time));

                                    // Trigger Next publishing point from the pool.
                                    LOG.info("Auto-Publishing Status for {} :: {}", onGoingPublishingDitamap, DITAConstants.DITA_DOCUMENTSTATE_DONE);
                                    String nextPublishingPoint = fetchNextPublishingPointWithPendingValue(publishingStatusJsonObj);

                                    if(!nextPublishingPoint.isEmpty()) {
                                        updatePropertyInNode(autoPublishNode, MadisonConstants.ONGOING_PUBLISHING, nextPublishingPoint);
                                        publishingStatusJsonObj.addProperty(nextPublishingPoint, MadisonConstants.IN_PROGRESS);
                                        updatePropertyInNode(autoPublishNode, MadisonConstants.PN_PUBLISHING_STATUS, publishingStatusJsonObj.toString());
                                        publishDitamapService.initiatePublishingProcess(nextPublishingPoint, resolver, MadisonConstants.OUTPUT_TYPE_AEMSITE, externalizer);
                                    }else {
                                        LOG.info("Publishing completed for all subscribing territory of source :: "+autoPublishNode.getProperty(DITAConstants.SOURCE_PATH));
                                        Iterator<String> iterator = publishingStatusJsonObj.keySet().iterator();
                                        LOG.debug("Sending Auto-Publish Completion Notification :: ");
                                        while (iterator.hasNext()) {
                                            publishDitamapService.sendAutoPublishCompletionNotifications(iterator.next(), resolver);
                                        }
                                        autoPublishNode.remove();
                                    }
                                }
                                if (session != null) {
                                    session.save();
                                }
                            }
                        }else {
                            return;
                        }
                    }

                } catch (Exception e){
                    LOG.error("Error in Auto-Publish Ditamap Scheduler :: ",e);
                }

            }

        }

    }

    private void updatePropertyInNode(final Node node, final String propertyToUpdate, final String propertyValue){
        if (node != null) {
            try {
                node.setProperty(propertyToUpdate, propertyValue);
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String fetchNextPublishingPointWithPendingValue(JsonObject jsonObject) {
        return jsonObject.entrySet()
                .stream()
                .filter(entry -> entry.getValue().getAsString().equals(PENDING))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("");
    }

}
