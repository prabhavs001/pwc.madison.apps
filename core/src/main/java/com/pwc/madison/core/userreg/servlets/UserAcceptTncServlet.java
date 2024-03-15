package com.pwc.madison.core.userreg.servlets;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.http.Cookie;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import com.google.gson.Gson;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.Constants;
import com.pwc.madison.core.userreg.models.request.UserAcceptTncRequest;
import com.pwc.madison.core.userreg.models.response.GetUserResponse;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.userreg.utils.UserRegUtil;
import com.sun.jersey.api.client.ClientResponse;;

/**
 *
 * Servlet to handle accept tnc request.
 *
 */
@Component(
        service = Servlet.class,
        name = "PwC Viewpoint User Accept Terms and Conditions Servlet",
        property = {
                org.osgi.framework.Constants.SERVICE_DESCRIPTION
                        + "=PwC Viewpoint User Accept Terms and Conditions Servlet",
                "sling.servlet.paths=/bin/userreg/accepttnc", "sling.servlet.methods=" + HttpConstants.METHOD_POST })
public class UserAcceptTncServlet extends SlingAllMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAcceptTncServlet.class);

    private static final String REFERRER_JSON_KEY = "referrer";

    @Reference
    private transient UserRegRestService userregRestService;

    @Reference
    private transient CountryTerritoryMapperService countryTerritoryMapperService;

    @Reference
    private transient CryptoSupport cryptoSupport;

    @Reference
    private transient ResourceResolverFactory resourceResolverFactory;

    @Reference
    private transient UserPreferencesProviderService userPreferencesProviderService;

    @Reference
    private XSSAPI xssAPI;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {

        LOGGER.debug("doPost() of: {}", this.getClass().getName());
        final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
        if (null != madisonCookie) {
            final Object requestObject = UserRegUtil.getObjectFromRequest(request, UserAcceptTncRequest.class);
            if (requestObject != null) {
                final UserAcceptTncRequest userAcceptTncRequest = (UserAcceptTncRequest) requestObject;
                String referrerAccepted = userAcceptTncRequest.getReferrerAccepted();
                if (cryptoSupport.isProtected(referrerAccepted)) {
                    try {
                        referrerAccepted = cryptoSupport.unprotect(referrerAccepted);
                        userAcceptTncRequest.setReferrerAccepted(referrerAccepted);
                    } catch (final CryptoException cryptoException) {
                        LOGGER.error("UserAcceptTncServlet doPost() : CryptoException while decrypting referrer : {}",
                                cryptoException);
                    }
                }
                userAcceptTncRequest.setVersionAccepted(countryTerritoryMapperService.getTerritoryCodeToTerritoryMap()
                        .get(userAcceptTncRequest.getTerritoryCode()).getTermsAndConditionsVersion());
                final ClientResponse clientResponse = userregRestService.acceptTnc(userAcceptTncRequest,
                        madisonCookie.getValue());
                String responseString = clientResponse.getEntity(String.class);
                if (clientResponse.getStatus() == SlingHttpServletResponse.SC_OK) {
                    final GetUserResponse getUserResponse = new Gson().fromJson(responseString, GetUserResponse.class);
                    UserInformationUtil.updateMadisonUserProfileCookie(request, response,
                            getUserResponse.getData().getUserProfile(), countryTerritoryMapperService, cryptoSupport,
                            userPreferencesProviderService, false, xssAPI);
                    final JSONObject responseData = new JSONObject();
                    try {
                        responseData.put(REFERRER_JSON_KEY, referrerAccepted);
                        responseString = responseData.toString();
                    } catch (JSONException jsonException) {
                        LOGGER.error(
                                "UserAcceptTncServlet doPost() : JSON Exception occured while writing json data to response for referrer {} : {}",
                                referrerAccepted, jsonException);
                    }
                } else if (clientResponse.getStatus() == SlingHttpServletResponse.SC_UNAUTHORIZED) {
                    UserRegUtil.removeUserRegMadisonCookie(request, response);
                }
                responseString = StringUtils.isBlank(responseString) ? "{}" : responseString;
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
