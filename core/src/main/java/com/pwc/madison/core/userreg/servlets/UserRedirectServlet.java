package com.pwc.madison.core.userreg.servlets;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.userreg.Constants;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserRegUtil;

/**
 * 
 * Servlet to handle user redirection after logout.
 *
 */
@Component(
    service = Servlet.class,
    name = "PwC Viewpoint User Internal User Redirect Servlet",
    property = {org.osgi.framework.Constants.SERVICE_DESCRIPTION + "=PwC Viewpoint User Redirect Servlet",
        "sling.servlet.paths=/bin/userreg/redirect", "sling.servlet.methods=" + HttpConstants.METHOD_GET})
public class UserRedirectServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRedirectServlet.class);

    private final String SAML_LOGOUT_REDIRECT_PARAM = "redirectUrl";

    @Reference
    private transient UserRegRestService userregRestService;

    @Reference
    private transient XSSAPI xssAPI;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
        throws ServletException, IOException {
        final Cookie madisonRedirectCookie = UserRegUtil.getCookieByName(request,
            Constants.MADISON_USER_REDIRECT_PATH_COOKIE);
        String redirectionPath = userregRestService.getUserDefaultRedirectionPath();
        if (null != madisonRedirectCookie) {
            redirectionPath = madisonRedirectCookie.getValue();
            boolean isHttps = request.getProtocol().contains(Constants.HTTPS);
            UserRegUtil.setCookie(response, Constants.MADISON_USER_REDIRECT_PATH_COOKIE, null, 0, isHttps, false, null);
        }
        try {
            URIBuilder samlLogoutURLBuilder = new URIBuilder(userregRestService.getSamlLogoutEndPoint());
            samlLogoutURLBuilder.addParameter(SAML_LOGOUT_REDIRECT_PARAM, redirectionPath);
            redirectionPath = samlLogoutURLBuilder.toString();
        } catch (URISyntaxException e) {
            LOGGER.error("Unable to create URI for SAML logout API: {}", userregRestService.getSamlLogoutEndPoint());
        }
        LOGGER.debug("LogoutServlet doGet() : Redirection Path after logout is {} : ", xssAPI.encodeForHTML(redirectionPath));
        response.sendRedirect(StringUtils.normalizeSpace(redirectionPath));
    }

}
