package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.util.GregorianCalendar;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.http.entity.ContentType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.eclipse.jetty.http.HttpStatus;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Servlet class to deactivate and delete pages for a publishing point
 */
@Component(
    immediate = false,
    service = Servlet.class,
    property = { "sling.servlet.paths=/bin/pwc-madison/unpublish", "sling.servlet.methods=GET" },
    configurationPolicy = ConfigurationPolicy.REQUIRE)
public class UnpublishServlet extends SlingAllMethodsServlet {

    @Reference
    private transient Replicator replicator;

    @Reference
    private transient ResourceResolverFactory resolverFactory;

    @Reference
    private transient MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(UnpublishServlet.class);
    private static final String REQUEST_PARAM_MAP_PATH = "mapPath";

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {
        final ResourceResolver resolver = MadisonUtil.getResourceResolver(resolverFactory,
                madisonSystemUserNameProviderService.getFmditaServiceUsername());
        if (null == resolver) {
            sendErrorResponse(response);
            return;
        }

        final Session session = resolver.adaptTo(Session.class);
        if (session == null) {
            sendErrorResponse(response);
            return;
        }

        try {
            final String mapPath = request.getParameter(REQUEST_PARAM_MAP_PATH);
            final Resource presetResource = resolver.getResource(mapPath + DITAConstants.AEMSITE_PRESETS_NODE);

            if (null == presetResource) {
                sendErrorResponse(response);
                return;
            }

            final ValueMap presetProperties = presetResource.adaptTo(ValueMap.class);

            if (null == presetProperties) {
                sendErrorResponse(response);
                return;
            }

            if (!presetProperties.containsKey(DITAConstants.PN_LAST_PUBLISHED_PATH)) {
                sendErrorResponse(response);
                return;
            }

            String lastPublishedPath = presetProperties.get(DITAConstants.PN_LAST_PUBLISHED_PATH).toString();
            lastPublishedPath = lastPublishedPath.substring(0, lastPublishedPath.indexOf(DITAConstants.HTML_EXT));
            replicator.replicate(session, ReplicationActionType.DEACTIVATE, lastPublishedPath);

            final Resource metadataResource = resolver.getResource(mapPath + MadisonConstants.METADATA_PATH);

            final ModifiableValueMap metadata = metadataResource.adaptTo(ModifiableValueMap.class);
            metadata.put(DITAConstants.PN_METADATA_LAST_UNPUBLISHED, new GregorianCalendar());
            resolver.commit();

            if (session.nodeExists(lastPublishedPath)) {
                session.getNode(lastPublishedPath).remove();
                session.save();
            }

        } catch (ReplicationException | RepositoryException e) {
            LOGGER.error("Error in unpublish servlet", e);
            sendErrorResponse(response);
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
    }

    /**
     * Method to set 500 error code in the response status and write error response.
     *
     * @param response
     * @throws IOException
     */
    private void sendErrorResponse(final SlingHttpServletResponse response) throws IOException {
        response.setContentType(ContentType.TEXT_HTML.getMimeType());
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
        response.getWriter().write("Error in unpublishing pages");
    }

}
