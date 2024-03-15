package com.pwc.madison.core.schedulers;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.services.ReplicationPurgeSchedulerConfig;
import com.pwc.madison.core.util.MadisonUtil;

@Component(immediate = true, service = ReplicationPurgeScheduler.class)
@Designate(ocd = ReplicationPurgeSchedulerConfig.class)
public class ReplicationPurgeScheduler implements Runnable {

    private static final String JCR_CREATED_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    private Session session;
    private ResourceResolver resourceResolver;
    private int schedulerID;
    private int dateLimit;
    @Reference
    private QueryBuilder queryBuilder;

    @Activate
    protected void activate(final ReplicationPurgeSchedulerConfig config) {
        log.debug("ReplicationPurgeScheduler :: activate() :: activate method called");
        removeScheduler();
        schedulerID = config.schedulerName().hashCode();
        dateLimit = config.dateLimit();
        addScheduler(config);
    }

    /**
     * Remove a scheduler based on the scheduler ID
     */
    private void removeScheduler() {
        log.debug("ReplicationPrugeScheduler :: removeScheduler() :: Removing Scheduler Job '{}'", schedulerID);
        scheduler.unschedule(String.valueOf(schedulerID));
    }

    @Deactivate
    protected void deactivate() {
        if (session != null) {
            session.logout();
        }
    }

    /**
     * Add a scheduler based on the scheduler ID
     */
    private void addScheduler(final ReplicationPurgeSchedulerConfig config) {
        log.debug("ReplicationPurgeScheduler :: addScheduler :: inside add scheduler Method");
        if (config.serviceEnabled()) {
            final ScheduleOptions sopts = scheduler.EXPR(config.schedulerExpression());
            sopts.name(String.valueOf(schedulerID));
            sopts.canRunConcurrently(config.schedulerConcurrent());
            scheduler.schedule(this, sopts);
            log.debug("ReplicationPurgeScheduler added succesfully");
        } else {
            log.debug("ReplicationPurgeScheduler is Disabled, no scheduler job created");
        }
    }

    @Override
    public void run() {
        resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                madisonSystemUserNameProviderService.getReplicationServiceUsername());
        if (null == resourceResolver) {
            return;
        }

        session = resourceResolver.adaptTo(Session.class);

        if (null == session) {
            return;
        }

        try {
            final Map<String, Object> predicateMap = getPredicateMap(dateLimit);
            final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap), session);
            final SearchResult searchResult = query.getResult();
            final Iterator<Resource> resources = searchResult.getResources();
            while (resources.hasNext()) {
                final Resource resource = resources.next();
                purgeNode(resource);
            }

            if (null != session) {
                session.save();
            }

        } catch (final RepositoryException e) {
            log.error("Error Will doing purge activity {}", e);
        } finally {
            if (null != resourceResolver) {
                resourceResolver.close();
            }
        }
    }

    private void purgeNode(final Resource resource) {
        try {
            if (null != resource) {
                final Node node = resource.adaptTo(Node.class);
                if (null != node) {
                    log.debug("node for the path would be deleted: {}", node.getPath());
                    node.remove();
                }
            }
        } catch (final RepositoryException e) {
            log.error("error while removing the node for the resource {}", resource.getPath());
        }
    }

    /**
     * Private method to return the Search predicate map
     *
     * @param outputPath
     *            pathOfOutputPage
     * @param dateLimitVal
     * @return predicateMap
     */
    private Map<String, Object> getPredicateMap(final int dateLimitVal) {

        final SimpleDateFormat sdf = new SimpleDateFormat(JCR_CREATED_DATE_FORMAT);
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -dateLimitVal);
        final String currDateStr = sdf.format(cal.getTime());

        final Map<String, Object> predicateMap = new HashMap<>();
        predicateMap.put("group.1_group.path", MadisonConstants.REP_STATUS_CONTENT_PATH);
        predicateMap.put("group.1_group.type", "nt:unstructured");
        predicateMap.put("group.2_group.path", MadisonConstants.AUTO_REPLICATION_PACKAGE_PATH);
        predicateMap.put("group.2_group.type", "nt:file");
        predicateMap.put("group.p.or", "true");
        predicateMap.put("p.limit", "-1");
        predicateMap.put("daterange.property", "jcr:created");
        predicateMap.put("daterange.upperBound", currDateStr);

        return predicateMap;
    }

}
