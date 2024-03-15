package com.pwc.madison.core.listeners;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
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
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.replication.Replicator;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;

@Component(service = EventListener.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class RootNodeReverseReplicationListener implements EventListener {
    private static final String PV_ACTIVATE = "Activate";
    private static final String PN_CQ_LAST_REPLICATION_ACTION = "cq:lastReplicationAction";
    private static final String PN_CQ_LAST_REPLICATED = "cq:lastReplicated";
    private static final String PN_CQ_LAST_REPLICATED_BY = "cq:lastReplicatedBy";
    private static final String PN_TYPE = "type";
    private static final String PN_PACKAGE_PATH = "packagePath";
    private static final String JCR_PATH = "/jcr:content";
    private static final String PN_MAP_PARENT = "mapParent";
    private static String PN_ROOT_PAGE_PATH = "rootPagePath";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Reference
    transient Replicator replicator;
    @Reference
    private SlingRepository repository;
    @Reference
    private ResourceResolverFactory resolverFactory;
    @Reference
    private QueryBuilder queryBuilder;
    
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    private Session session;
    private ObservationManager observationManager;

    protected void activate(final ComponentContext context) throws Exception {
        session = repository.loginService(madisonSystemUserNameProviderService.getFmditaServiceUsername(), null);
        observationManager = session.getWorkspace().getObservationManager();
        observationManager.addEventListener(this, Event.NODE_ADDED, MadisonConstants.REP_STATUS_CONTENT_PATH, true,
                null, null, true);

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
            resourceResolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
            if (null != resourceResolver) {
                while (it.hasNext()) {
                    final Event event = it.nextEvent();
                    logger.debug("********INSIDE TRY ***** {}", event.getPath());
                    final Session session = resourceResolver.adaptTo(Session.class);
                    final Node addedNode = session.getNode(event.getPath());
                    final String sourcePath = addedNode.hasProperty(PN_ROOT_PAGE_PATH)
                            ? addedNode.getProperty(PN_ROOT_PAGE_PATH).getString()
                            : StringUtils.EMPTY;
                    final String packagePath = addedNode.hasProperty(PN_PACKAGE_PATH)
                            ? addedNode.getProperty(PN_PACKAGE_PATH).getString()
                            : StringUtils.EMPTY;
                    if (!sourcePath.isEmpty()) {
                        final Node sourceNode = session.getNode(sourcePath.concat(JCR_PATH));
                        if (null != sourceNode) {
                            final String parentMapPath = sourceNode.hasProperty(PN_MAP_PARENT)
                                    ? sourceNode.getProperty(PN_MAP_PARENT).getString()
                                    : StringUtils.EMPTY;
                            if (session.nodeExists(parentMapPath)) {
                                final Node mapNode = session
                                        .getNode(parentMapPath.concat(MadisonConstants.METADATA_PATH));
                                mapNode.setProperty(PN_PACKAGE_PATH, packagePath);
                            }

                            setReplicationProperties(sourcePath, session);
                            final Map<String, Object> predicateMap = getPredicateMap(sourcePath);
                            final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap), session);
                            final SearchResult searchResult = query.getResult();
                            final Iterator<Resource> resources = searchResult.getResources();
                            while (resources.hasNext()) {
                                setReplicationProperties(resources.next().getPath(), session);
                            }
                        }
                    }
                }
            }
        } catch (final PathNotFoundException e) {
            logger.error("PathNotFoundException in onEvent method {}", e);
        } catch (final RepositoryException e) {
            logger.error("RepositoryException in onEvent method {}", e);
        } finally {
            if (null != resourceResolver && resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }
    }

    private void setReplicationProperties(final String pagePath, final Session session) throws RepositoryException {
        final Node node = session.getNode(pagePath);
        final Node jcrContent = node.getNode(JcrConstants.JCR_CONTENT);
        jcrContent.setProperty(PN_CQ_LAST_REPLICATED_BY, session.getUserID());
        jcrContent.setProperty(PN_CQ_LAST_REPLICATED, Calendar.getInstance());
        jcrContent.setProperty(PN_CQ_LAST_REPLICATION_ACTION, PV_ACTIVATE);
        session.save();
    }

    /**
     * Private method to return the Search predicate map
     *
     * @param outputPath
     *            pathOfOutputPage
     * @return predicateMap
     */
    private Map<String, Object> getPredicateMap(final String outputPath) {

        final Map<String, Object> predicateMap = new HashMap<>();
        predicateMap.put("p.limit", "-1");
        predicateMap.put("path", outputPath);
        predicateMap.put(PN_TYPE, "cq:Page");
        predicateMap.put("property", "jcr:content/@sourcePath");
        predicateMap.put("property.operation", "exists");

        return predicateMap;
    }
}
