package com.pwc.madison.core.userreg.servlets;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.userreg.Constants;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserRegUtil;
import com.sun.jersey.api.client.ClientResponse;

/**
 * 
 * Servlet to handle user logout. The servlet is also responsible for removing cookies maintained for user.
 * 
 */
@Component(
        service = Servlet.class,
        name = "PwC Viewpoint User Logout Servlet",
        property = { org.osgi.framework.Constants.SERVICE_DESCRIPTION + "=PwC Viewpoint User Logout Servlet",
                "sling.servlet.paths=/bin/userreg/logout", "sling.servlet.methods=" + HttpConstants.METHOD_POST })
public class LogoutServlet extends SlingAllMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutServlet.class);

    @Reference
    private transient UserRegRestService userregRestService;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
        if (null != madisonCookie) {
            final ClientResponse clientResponse = userregRestService.logout(madisonCookie.getValue());
            String responseString = clientResponse.getEntity(String.class);
            LOGGER.debug("LogoutServlet doPost() : Response String for User logout {} with HTTP status {}",
                    responseString, clientResponse.getStatus());
            if (clientResponse.getStatus() == SlingHttpServletResponse.SC_OK) {
                UserRegUtil.removeUserRegMadisonCookie(request, response);
            }
            responseString = StringUtils.isBlank(responseString) ? "{}" : responseString;
            response.setContentType(Constants.CONTENT_TYPE_JSON);
            response.setCharacterEncoding(Constants.UTF_8_ENCODING);
            response.setStatus(clientResponse.getStatus());
            response.getWriter().write(responseString);
        }

    }

}
