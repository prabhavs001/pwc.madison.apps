
package com.pwc.madison.core.userreg.servlets;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.google.gson.Gson;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.Constants;
import com.pwc.madison.core.userreg.models.response.GetUserResponse;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.userreg.utils.UserRegUtil;
import com.sun.jersey.api.client.ClientResponse;

/**
 * 
 * Servlet to get authenticated User Profile from Viewpoint.
 * 
 */
@Component(
        service = Servlet.class,
        name = "PwC Viewpoint Get User Profile Servlet",
        property = { org.osgi.framework.Constants.SERVICE_DESCRIPTION + "=PwC Viewpoint Get User Profile Servlet",
                "sling.servlet.paths=/bin/userreg/getuser", "sling.servlet.methods=" + HttpConstants.METHOD_GET })
public class GetUserProfile extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetUserProfile.class);
    public static final String US_COUNTRY_CODE = "US";

    @Reference
    private transient UserRegRestService userRegRestService;

    @Reference
    private transient CountryTerritoryMapperService countryTerritoryMapperService;

    @Reference
    private transient UserPreferencesProviderService userPreferencesProviderService;

    @Reference
    private transient CryptoSupport cryptoSupport;
    
    @Reference
    private transient XSSAPI xssapi;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {

        //in case of external user, we have to call the getProfile for filling the profile details without logging in the user
        //don't set user profile cookie in case of auth token
        boolean setUserProfileCookie = false;
        String authToken = request.getParameter(Constants.GET_PROFILE_TOKEN_QUERY_PARAM);
        if(StringUtils.isBlank(authToken)){
            setUserProfileCookie = true;
            final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
            if (null != madisonCookie) {
                authToken = madisonCookie.getValue();
            }
        }
        if (StringUtils.isNotBlank(authToken)) {
            final ClientResponse clientResponse = userRegRestService.getUser(authToken);
            if (clientResponse.getStatus() == SlingHttpServletResponse.SC_OK) {
                LOGGER.debug("GetUserProfile : doGet() : updating user cookie");
                String responseString = clientResponse.getEntity(String.class);
                final GetUserResponse getUserResponse = new Gson().fromJson(responseString, GetUserResponse.class);
                if(getUserResponse.getData().getUserProfile().getCountry().equals(US_COUNTRY_CODE)) {
                    getUserResponse.getData().setUserProfile(UserRegUtil.updateUsersIndustry(getUserResponse.getData().getUserProfile()));
                }
                if(setUserProfileCookie) {
                    UserInformationUtil.updateMadisonUserProfileCookie(request, response,
                        getUserResponse.getData().getUserProfile(), countryTerritoryMapperService, cryptoSupport,
                        userPreferencesProviderService, false, xssapi);
                    getUserResponse.getData().getUserProfile().setContentAccessInfo(UserInformationUtil.getMadisonUserContentAccessInfo(request, cryptoSupport));
                    responseString = new Gson().toJson(getUserResponse);
                }
                response.setContentType(Constants.CONTENT_TYPE_JSON);
                response.setCharacterEncoding(Constants.UTF_8_ENCODING);
                response.getWriter().write(responseString);
            } else {
                if (clientResponse.getStatus() == SlingHttpServletResponse.SC_UNAUTHORIZED)
                    UserRegUtil.removeUserRegMadisonCookie(request, response);
                response.sendError(clientResponse.getStatus());
            }
        } else {
            response.sendError(SlingHttpServletResponse.SC_UNAUTHORIZED);
        }
    }

}
