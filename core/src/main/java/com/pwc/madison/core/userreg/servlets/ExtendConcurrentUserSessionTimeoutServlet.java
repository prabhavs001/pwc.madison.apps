package com.pwc.madison.core.userreg.servlets;

import com.adobe.granite.crypto.CryptoSupport;
import com.google.gson.Gson;
import com.pwc.madison.core.userreg.Constants;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.userreg.models.response.LoginResponse;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.userreg.utils.UserRegUtil;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import java.io.IOException;

/**
 *
 * Servlet to get authenticated concurrent user token while extending session timeout from Viewpoint.
 *
 */

@Component(
        service = Servlet.class,
        name = "PwC Viewpoint Extend Concurrent User Session Timeout Servlet",
        property = { org.osgi.framework.Constants.SERVICE_DESCRIPTION + "=PwC Viewpoint Extend Concurrent User Session Timeout Servlet",
                "sling.servlet.paths=/bin/userreg/extend/session", "sling.servlet.methods=" + HttpConstants.METHOD_GET })
public class ExtendConcurrentUserSessionTimeoutServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendConcurrentUserSessionTimeoutServlet.class);

    @Reference
    private transient UserRegRestService userRegRestService;

    @Reference
    private transient ResourceResolverFactory resourceResolverFactory;

    @Reference
    private transient CryptoSupport cryptoSupport;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {

        String authToken = StringUtils.EMPTY;
        final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
        if(null != madisonCookie) {
            authToken = madisonCookie.getValue();
        }
        if(StringUtils.isNotBlank(authToken)) {
            UserProfile userProfile = UserInformationUtil.getUserProfile(request,cryptoSupport,false,null,null,null,
                    null,false,false,null);
            if (null != userProfile) {
                if (!userProfile.getIsInternalUser() && userProfile.getContentAccessInfo().isConcurrentLicensedUser()) {
                    final ClientResponse extendConcurrentLicenseClientResponse = userRegRestService.getExtendConcurrentLicenseSessionTimeout(authToken);
                    if (extendConcurrentLicenseClientResponse.getStatus() == SlingHttpServletResponse.SC_OK) {
                        LOGGER.debug("ExtendConcurrentUserSessionTimeoutServlet : doGet() : extending user cookie expiry time in hours");
                        String extendConcurrentLicenseResponseString = extendConcurrentLicenseClientResponse.getEntity(String.class);
                        final LoginResponse loginResponse = new Gson().fromJson(extendConcurrentLicenseResponseString, LoginResponse.class);
                        if (StringUtils.isNotBlank(loginResponse.getData().getToken())) {
                            boolean isUserLoggedIn = UserRegUtil.extendConcurrentMadisonUserSession(request, response, loginResponse.getData().getToken(),
                                    userRegRestService);
                            LOGGER.debug("ExtendConcurrentUserSessionTimeoutServlet doGet() : Is User logged in successfully {}", isUserLoggedIn);
                            loginResponse.getData().setToken(null);
                            extendConcurrentLicenseResponseString = new Gson().toJson(loginResponse);
                        }
                        response.setContentType(Constants.CONTENT_TYPE_JSON);
                        response.setCharacterEncoding(Constants.UTF_8_ENCODING);
                        response.setStatus(extendConcurrentLicenseClientResponse.getStatus());
                        response.getWriter().write(extendConcurrentLicenseResponseString);
                    } else {
                        if (extendConcurrentLicenseClientResponse.getStatus() == SlingHttpServletResponse.SC_UNAUTHORIZED)
                            UserRegUtil.removeUserRegMadisonCookie(request, response);
                        response.sendError(extendConcurrentLicenseClientResponse.getStatus());
                    }

                }
            } else {
                response.sendError(SlingHttpServletResponse.SC_UNAUTHORIZED);
            }
        } else {
            response.sendError(SlingHttpServletResponse.SC_UNAUTHORIZED);
        }

    }
}
