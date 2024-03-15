package com.pwc.madison.core.userreg.servlets;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.google.gson.Gson;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.Constants;
import com.pwc.madison.core.userreg.models.request.EditContentPreferencesRequest;
import com.pwc.madison.core.userreg.models.response.GetUserResponse;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.userreg.utils.UserRegUtil;
import com.sun.jersey.api.client.ClientResponse;

/**
 *
 * Servlet to handle User edit content preferences request from Viewpoint.
 *
 */
@Component(
        service = Servlet.class,
        name = "PwC Viewpoint Edit Content Preferences Servlet",
        property = { org.osgi.framework.Constants.SERVICE_DESCRIPTION + "=PwC Viewpoint Edit Content Preferences Servlet",
                "sling.servlet.paths=/bin/userreg/preferences/content",
                "sling.servlet.methods=" + HttpConstants.METHOD_PUT })
public class EditContentPreferences extends SlingAllMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditContentPreferences.class);

    @Reference
    private transient UserRegRestService userregRestService;

    @Reference
    private transient ResourceResolverFactory resourceResolverFactory;

    @Reference
    private transient UserPreferencesProviderService userPreferencesProviderService;

    @Reference
    private transient CountryTerritoryMapperService countryTerritoryMapperService;

    @Reference
    private transient CryptoSupport cryptoSupport;

    @Reference
    private XSSAPI xssAPI;

    @Override
    protected void doPut(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
        if (null != madisonCookie) {
            final Object requestObject = UserRegUtil.getObjectFromRequest(request, EditContentPreferencesRequest.class);
            if (null != requestObject) {
                final EditContentPreferencesRequest editContentPreferencesRequest = (EditContentPreferencesRequest) requestObject;
                final ClientResponse clientResponse = userregRestService
                        .editContentPreferences(editContentPreferencesRequest, madisonCookie.getValue());
                final String responseString = clientResponse.getEntity(String.class);
                if (clientResponse.getStatus() == SlingHttpServletResponse.SC_OK) {
                    final GetUserResponse getUserResponse = new Gson().fromJson(responseString, GetUserResponse.class);
                    LOGGER.debug("EditContentPreferences doPut() : Initaiting Profile creation/updation in AEM {}",
                            xssAPI.encodeForHTML(getUserResponse.getData().getUserProfile().getEmail()));
                    UserInformationUtil.updateMadisonUserProfileCookie(request, response,
                            getUserResponse.getData().getUserProfile(), countryTerritoryMapperService, cryptoSupport,
                            userPreferencesProviderService, false, xssAPI);
                } else if (clientResponse.getStatus() == SlingHttpServletResponse.SC_UNAUTHORIZED) {
                    UserRegUtil.removeUserRegMadisonCookie(request, response);
                }
                response.setContentType(Constants.CONTENT_TYPE_JSON);
                response.setCharacterEncoding(Constants.UTF_8_ENCODING);
                response.setStatus(clientResponse.getStatus());
                response.getWriter().write(responseString);
            } else {
                response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            response.sendError(SlingHttpServletResponse.SC_UNAUTHORIZED);
        }
    }

}
