package com.pwc.madison.core.listeners;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.replication.AgentFilter;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;

@Component(service = EventListener.class,
           immediate = true,
           configurationPolicy = ConfigurationPolicy.REQUIRE)
public class PackageReplicationListener implements EventListener {
    private static final String CREATED_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    private static String REP_AGENT = "outbox";
    private static String PN_GROUP = "group";
    private static String PN_LAST_UNWRAPPED = "lastUnwrapped";
    private static String PN_NAME = "name";
    private static String PN_ROOT = "root";
    private static String FILTER_PATH = "filter/f0";
    private static String PN_ROOT_PAGE_PATH = "rootPagePath";
    private static String PN_PACKAGE_PATH = "packagePath";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Reference
    transient Replicator replicator;
    @Reference
    private SlingRepository repository;
    @Reference
    private ResourceResolverFactory resolverFactory;
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;


    private Session session;
    private ObservationManager observationManager;

    protected void activate(final ComponentContext context) throws Exception {
        session = repository.loginService(madisonSystemUserNameProviderService.getFmditaServiceUsername(), null);
        observationManager = session.getWorkspace().getObservationManager();
        observationManager
                .addEventListener(this, Event.NODE_ADDED, MadisonConstants.AUTO_REPLICATION_PACKAGE_PATH, true, null,
                        null, true);

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
    public void onEvent(final EventIterator it) {
        ResourceResolver resourceResolver = null;
        try {
            while (it.hasNext()) {
                final Event event = it.nextEvent();
                resourceResolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getReplicationServiceUsername());
                final Resource packageResource = resourceResolver.getResource(event.getPath());
                if (null != packageResource) {
                    logger.debug("Package {} got replicated", packageResource.getPath());
                    final ValueMap packageProperties = packageResource.getValueMap();
                    if (packageProperties.containsKey(PN_GROUP) && packageProperties.containsKey(PN_LAST_UNWRAPPED)) {
                        final String groupName = packageProperties.get(PN_GROUP, String.class);
                        if (groupName.equals(MadisonConstants.AUTO_REPLICATION_PACKAGE_GROUP)) {
                            addNodeAndReverseReplicate(resourceResolver, packageResource);
                        }
                    }
                }
            }
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (null != resourceResolver && resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }
    }

    /**
     * This method sets flag to notify author about reverse replication
     *
     * @param resourceResolver
     * @param packageRes
     */
    private void addNodeAndReverseReplicate(ResourceResolver resourceResolver, Resource packageRes) {
        try {
            logger.debug("Reverse replication started for {}", packageRes.getPath());
            Session session = resourceResolver.adaptTo(Session.class);
            ValueMap packageProperties = packageRes.getValueMap();
            Resource folderRes = resourceResolver.getResource(MadisonConstants.REP_STATUS_CONTENT_PATH);
            if (null != folderRes && packageProperties.containsKey(PN_NAME)) {
                Node folderNode = folderRes.adaptTo(Node.class);
                Node newRepDetailsNode = folderNode
                        .addNode(packageProperties.get(PN_NAME, String.class), JcrConstants.NT_UNSTRUCTURED);
                Resource filterRes = packageRes.getChild(FILTER_PATH);
                if (null != filterRes) {
                    ValueMap filterProperties = filterRes.getValueMap();
                    String filterPath = filterProperties.containsKey(PN_ROOT) ?
                            filterProperties.get(PN_ROOT).toString() :
                            StringUtils.EMPTY;
                    newRepDetailsNode.setProperty(PN_ROOT_PAGE_PATH, filterPath);
                    newRepDetailsNode.setProperty(PN_PACKAGE_PATH,
                            packageRes.getPath().split(DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT)[0]);
                    newRepDetailsNode.setProperty(JcrConstants.JCR_CREATED,
                            new SimpleDateFormat(CREATED_DATE_FORMAT).format(new Date()));
                }
                session.save();
                final ReplicationOptions opts = new ReplicationOptions();
                opts.setFilter(new AgentFilter() {
                    @Override
                    public boolean isIncluded(final com.day.cq.replication.Agent agent) {
                        return agent.getId().equalsIgnoreCase(REP_AGENT);
                    }
                });
                replicator.replicate(session, ReplicationActionType.ACTIVATE, newRepDetailsNode.getPath(), opts);
            }
        } catch (final RepositoryException | ReplicationException e) {
            logger.error("RepositoryException or ReplicationException in addNodeAndReverseReplicate method {}", e);
        }
    }
}

