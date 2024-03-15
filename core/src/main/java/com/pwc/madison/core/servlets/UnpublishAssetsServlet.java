package com.pwc.madison.core.servlets;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.eclipse.jetty.http.HttpStatus;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.servlet.Servlet;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Servlet class to deactivate and delete pages for a publishing point
 */
@Component(
        immediate = false,
        service = Servlet.class,
        property = { "sling.servlet.paths=/bin/pwc-madison/unpublish-assets", "sling.servlet.methods=GET" })
@Designate(ocd = UnpublishAssetsServlet.UnpublishAssetReportConfiguration.class)
public class UnpublishAssetsServlet extends SlingAllMethodsServlet {

    @Reference
    private transient Replicator replicator;

    @Reference
    private transient ResourceResolverFactory resolverFactory;

    @Reference
    private transient MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    private int timeoutVal = 1000;

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(UnpublishAssetsServlet.class);
    private static final String REQUEST_PARAM_ASSET= "assetPath";
    private static final String REQUEST_PARAM_ASSETS= "assetPaths";

    @Activate
    @Modified
    protected void Activate(final UnpublishAssetsServlet.UnpublishAssetReportConfiguration config) {
        timeoutVal = config.timeout_value();
    }

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
            final String assetPath = request.getParameter(REQUEST_PARAM_ASSET);

            replicator.replicate(session, ReplicationActionType.DEACTIVATE, assetPath);
            resolver.commit();


        } catch (ReplicationException e) {
            LOGGER.error("Error in unpublish asset servlet", e);
            sendErrorResponse(response);
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
    }

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws IOException{
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
            final List<String> assetPaths = Arrays.asList(request.getParameterValues(REQUEST_PARAM_ASSETS));
            LOGGER.error("Error in unpublish asset servlet");
            for (String assetPath:
                 assetPaths) {
                Thread.sleep(timeoutVal);
                replicator.replicate(session, ReplicationActionType.DEACTIVATE, assetPath);
                resolver.commit();
            }

       } catch (Exception e) {
          LOGGER.error("Error in unpublish asset servlet", e);
            sendErrorResponse(response);
        }
        finally {
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
        response.getWriter().write("Error in unpublishing asset");
    }

    @ObjectClassDefinition(name = "PwC Viewpoint Unpublish Assets Report Service Configuration")
    public @interface UnpublishAssetReportConfiguration{

        @AttributeDefinition(
                name = "Thread sleep time",
                description = "Thread sleep time")
        int timeout_value() default 1000;
    }

}
