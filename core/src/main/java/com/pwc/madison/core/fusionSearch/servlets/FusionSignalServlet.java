package com.pwc.madison.core.fusionSearch.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.fusionSearch.services.FusionSearchConfigurationService;
import com.pwc.madison.core.userreg.Constants;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;

@Component(
        service = Servlet.class,
        name = "PwC Viewpoint Fusion Signal Servlet",
        property = {
                org.osgi.framework.Constants.SERVICE_DESCRIPTION
                        + "=Lucidworks Fusion Signal Servlet for PwC Viewpoint",
                "sling.servlet.paths=/bin/pwc-madison/vp-signal",
                "sling.servlet.methods=" + HttpConstants.METHOD_POST })
public class FusionSignalServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Reference
    private FusionSearchConfigurationService fusionSearchConfigurationService;

    @Override
    protected void doPost(@Nonnull final SlingHttpServletRequest request,
            @Nonnull final SlingHttpServletResponse response) throws ServletException, IOException {

        try {
            final String username = fusionSearchConfigurationService.getUsername();
            final String password = fusionSearchConfigurationService.getPassword();
            final String encoding = new String(Base64
                    .encodeBase64((username + MadisonConstants.COLON + password).getBytes(MadisonConstants.UTF_8)));
            final Client client = Client.create();
            final WebResource webResource = client.resource(fusionSearchConfigurationService.getSignalEndpoint());
            final Builder builder = webResource.type(MediaType.APPLICATION_JSON);
            builder.header(HttpHeaders.AUTHORIZATION, MadisonConstants.BASIC_AUTHENTICATION + encoding);
            String postData = StringUtils.EMPTY;
            try (final BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(request.getInputStream(), Constants.UTF_8_ENCODING))) {
                if (bufferedReader != null) {
                    postData = bufferedReader.readLine();
                }
            } catch (final IOException ioException) {
                LOG.error("FusionSignalServlet : doPost() : IO Exception occured while getting Object from request {}",
                        ioException);
            }
            final ClientResponse clientResponse = builder.post(ClientResponse.class, postData);
            response.setContentType(Constants.CONTENT_TYPE_JSON);
            response.setCharacterEncoding(Constants.UTF_8_ENCODING);
            response.setStatus(clientResponse.getStatus());
        } catch (final Exception e) {
            LOG.error("Exception occured in Fusion Search Servlet: {}", e);
            sendErrorResponse(response);
        }
    }

    /**
     * Method to set 500 error code in the response status and write error response.
     * 
     * @param response
     */
    private void sendErrorResponse(final SlingHttpServletResponse response) throws IOException {
        response.setContentType(ContentType.TEXT_HTML.getMimeType());
        response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().write("Error in Viewpoint Signal Servlet");
    }

}
