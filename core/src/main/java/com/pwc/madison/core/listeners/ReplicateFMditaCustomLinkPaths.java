package com.pwc.madison.core.listeners;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.Replicator;
import com.day.cq.search.QueryBuilder;
import com.pwc.madison.core.beans.VpRedirectionModifyEventInfo;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.services.VpToVpRedirectionService;
import com.pwc.madison.core.util.MadisonUtil;

@Component(service = EventListener.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ReplicateFMditaCustomLinkPaths implements EventListener {

    private final String BEFORE_VALUE = "beforeValue";
    private final String AFTER_VALUE = "afterValue";

    private ObservationManager observationManager;
    @Reference
    Replicator replicator;
    @Reference
    private SlingRepository repository;
    @Reference
    private ResourceResolverFactory resolverFactory;
    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    private VpToVpRedirectionService vpRedirectionService;
    
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Session session;

    protected void activate(final ComponentContext context) throws Exception {
        session = repository.loginService(madisonSystemUserNameProviderService.getFmditaServiceUsername(), null);
        observationManager = session.getWorkspace().getObservationManager();
        observationManager.addEventListener(this, Event.PROPERTY_ADDED | Event.NODE_ADDED | Event.PROPERTY_CHANGED,
                MadisonConstants.CONTENT_FMDITACUSTOM_INDEX, true, null, null, true);
        logger.debug("*************added JCR event listener");
    }

    protected void deactivate(final ComponentContext componentContext) {
        try {
            if (observationManager != null) {
                observationManager.removeEventListener(this);
                logger.debug("*************removed JCR event listener");
            }
        } catch (final RepositoryException re) {
            logger.error("*************error removing the JCR event listener ", re);
        } finally {
            if (session != null) {
                session.logout();
                session = null;
            }
        }
    }

    @Override
    public void onEvent(final EventIterator events) {
        ResourceResolver resourceResolver = null;
        try {

            resourceResolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
            if (null == resourceResolver) {
                logger.error("resourceResolver is null for the User {}", madisonSystemUserNameProviderService.getFmditaServiceUsername());
                return;
            }
            final HashSet<String> replicationPaths = new HashSet<>();
            final List<VpRedirectionModifyEventInfo> modifiedUrlsInfoList = new ArrayList<>();
            while (events.hasNext()) {
                final Event event = events.nextEvent();
                logger.debug("********Event Triggred for the Path ***** {}", event.getPath());

                String nodePath = event.getPath();

                if (event.getType() == Event.NODE_ADDED) {
                    logger.debug("Event info NODE_ADDED {}", event.getPath());
                    replicationPaths.add(nodePath);
                }

                if (event.getType() == Event.PROPERTY_ADDED
                        || event.getType() == Event.PROPERTY_CHANGED && isValidEvent(event)) {
                    logger.debug("Event info PROPERTY_ADDED || PROPERTY_CHANGED {}", event.getPath());
                    if (event.getType() == Event.PROPERTY_CHANGED) {
                        modifiedUrlsInfoList
                                .add(new VpRedirectionModifyEventInfo(event.getInfo().get(BEFORE_VALUE).toString(),
                                        event.getInfo().get(AFTER_VALUE).toString(),
                                        nodePath.substring(nodePath.lastIndexOf("/") + 1, nodePath.length())));
                    }
                    nodePath = StringUtils.substring(nodePath, 0, nodePath.lastIndexOf(MadisonConstants.FORWARD_SLASH));
                    replicationPaths.add(nodePath);
                }

            }
            if(modifiedUrlsInfoList.size() > 0) {
                vpRedirectionService.captureModifiedUrls(modifiedUrlsInfoList);
            }

            for (final String replicationPath : replicationPaths) {
                final Session session = resourceResolver.adaptTo(Session.class);
                if (session.nodeExists(replicationPath)) {
                    replicator.replicate(session, ReplicationActionType.ACTIVATE, replicationPath);
                }
            }

        } catch (final Exception e) {
            logger.error("Error in ReplicateFMditaCustomLinkPaths  {}", e);
        } finally {
            if (null != resourceResolver && resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }

    }

    private boolean isValidEvent(final Event event) throws RepositoryException {

        boolean isValidEvent = true;

        if (event.getPath().endsWith("cq:lastReplicatedBy") || event.getPath().endsWith("cq:lastReplicated")
                || event.getPath().endsWith("cq:lastReplicationAction")) {
            isValidEvent = false;
        }

        return isValidEvent;
    }

}
