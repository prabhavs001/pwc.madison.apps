package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.AgentFilter;
import com.day.cq.replication.AgentManager;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.services.ReplicateReferecedAssetsService;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Servlet to trigger replication of assets after regeneration of output in fullcycle and simple workflows
 */
@Component(
    service = Servlet.class,
    property = { Constants.SERVICE_DESCRIPTION + "=Servlet to trigger referenced assets on a page",
            "sling.servlet.methods=" + HttpConstants.METHOD_GET,
            "sling.servlet.paths=/bin/pwc/triggerAssetReplication" },
    configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TriggerAssetReplicationServlet extends SlingSafeMethodsServlet {
    private static final String PN_TYPE = "type";
    private static final String HTML_EXTENSION = ".html";
    private static final String REP_AGENT = "pwc-ditacontent-replication-agent-%s";
    private static final Logger LOGGER = LoggerFactory.getLogger(TriggerAssetReplicationServlet.class);

    @Reference
    transient ReplicateReferecedAssetsService replicateReferecedAssetsService;
    @Reference
    transient ResourceResolverFactory resolverFactory;
    @Reference
    transient QueryBuilder queryBuilder;
    @Reference
    transient AgentManager agentMgr;
    @Reference
    transient Replicator replicator;
    @Reference
    transient MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;
    
    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        final ResourceResolver resolver = MadisonUtil.getResourceResolver(resolverFactory,
                madisonSystemUserNameProviderService.getFmditaServiceUsername());
        final RequestParameterMap requestParameterMap = request.getRequestParameterMap();
        final String regenTopicsString = requestParameterMap.containsKey(MadisonConstants.REGENERATED_TOPICS)
                ? requestParameterMap.getValue(MadisonConstants.REGENERATED_TOPICS).getString()
                : StringUtils.EMPTY;
        final String outputPath = requestParameterMap.containsKey(MadisonConstants.OUTPUT_PATH)
                ? requestParameterMap.getValue(MadisonConstants.OUTPUT_PATH).getString()
                : StringUtils.EMPTY;
        if (regenTopicsString.isEmpty() || outputPath.isEmpty()) {
            LOGGER.error(
                    "Output path parameter/regenerated topic paths parameter is empty. Can not trigger asset replication");
            return;
        }
        final String basePath = outputPath.substring(0, outputPath.indexOf(HTML_EXTENSION));

        /*
         * This below piece of code is written for fetching the specific replication agent based on territory and
         * replicate the assets via that agent instead of default agent
         */
        final String repAgent = String.format(REP_AGENT, MadisonUtil.getTerritoryCodeForPathUrl(basePath));

        final Session session = resolver.adaptTo(Session.class);
        final Map<String, Object> predicateMap = getPredicateMap(regenTopicsString, basePath);
        final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap), session);
        final SearchResult searchResult = query.getResult();

        final Iterator<Resource> resources = searchResult.getResources();
        while (resources.hasNext()) {
            final Resource resource = resources.next();
            final List<String> repAgentNames = MadisonUtil.getReplicationAgents(repAgent, agentMgr);
            if (!repAgentNames.isEmpty()) {
                replicateResource(session, resource.getPath(), repAgentNames);
            } else {
                LOGGER.error("Error while replicating the page for path " + resource.getPath() + "  for the agent {}",
                        repAgent);
            }
            replicateReferecedAssetsService.replicateReferencedAssets(resource);
        }
        response.setStatus(200);
    }

    /**
     * @param session
     * @param resourcePath
     * @param repAgentNames
     */
    private void replicateResource(final Session session, final String resourcePath, final List<String> repAgentNames) {

        for (final String repAgent : repAgentNames) {
            try {
                final ReplicationOptions opts = new ReplicationOptions();
                opts.setFilter(new AgentFilter() {
                    @Override
                    public boolean isIncluded(final com.day.cq.replication.Agent agent) {
                        return agent.getId().equalsIgnoreCase(repAgent);
                    }
                });
                replicator.replicate(session, ReplicationActionType.ACTIVATE, resourcePath, opts);
            } catch (final ReplicationException e) {
                LOGGER.error("Error while replicating the page for path {}", resourcePath);
            }
        }

    }

    /**
     * Private method to return the Search predicate map
     *
     * @param regeneratedTopicsString
     *            dita paths for which incremental regeneration is triggered
     * @return predicateMap
     */
    private Map<String, Object> getPredicateMap(final String regeneratedTopicsString, final String outputPath) {
        final JsonParser parser = new JsonParser();
        final JsonArray jsonArray = parser.parse(regeneratedTopicsString).getAsJsonArray();
        final Map<String, Object> predicateMap = new HashMap<>();
        predicateMap.put("p.limit", "-1");
        predicateMap.put("path", outputPath);
        predicateMap.put(PN_TYPE, "cq:Page");
        predicateMap.put("property", "jcr:content/@sourcePath");
        for (int i = 0, j = 1; i < jsonArray.size(); i++, j++) {
            predicateMap.put("property." + j + "_value", jsonArray.get(i).getAsString());
        }
        predicateMap.put("property.operation", "equals");

        return predicateMap;
    }

}
