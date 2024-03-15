package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.security.AccessControlException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.userreg.Constants;

@Component(
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    service = Servlet.class,
    property = {
            org.osgi.framework.Constants.SERVICE_DESCRIPTION
                    + "=Check curent logged in user permission on tagging console",
            "sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.paths=" + "/bin/validateUserPermission",
            "sling.servlet.extensions=" + "html" })
public class ValidateUserPermission extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ValidateUserPermission.class);
    private static final String TAGGINNG_CONSOLE_PATH = "/etc/tags";
    private static final String TAGGINNG_CONSOLE_CONTENT_PATH = "/content/cq:tags";
    private static final String SESSION_ACTION_ADD_NODE = "add_node";
    private static final String SESSION_ACTION_SET_PROPERTY = "set_property";
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    boolean hasPermission = false;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
     * org.apache.sling.api.SlingHttpServletResponse)
     */
    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        ResourceResolver resourceResolver = null;
        JSONObject jsonObj = new JSONObject();
        response.setContentType(Constants.CONTENT_TYPE_JSON);
        try {
            resourceResolver = request.getResourceResolver();
            Session session = resourceResolver.adaptTo(Session.class);
            hasPermission = (session.hasPermission(TAGGINNG_CONSOLE_PATH, SESSION_ACTION_ADD_NODE)
                    && session.hasPermission(TAGGINNG_CONSOLE_PATH, SESSION_ACTION_SET_PROPERTY))
                    || (session.hasPermission(TAGGINNG_CONSOLE_CONTENT_PATH, SESSION_ACTION_ADD_NODE)
                            && session.hasPermission(TAGGINNG_CONSOLE_CONTENT_PATH, SESSION_ACTION_SET_PROPERTY));
            jsonObj.put("hasPermission", hasPermission);
            response.getWriter().write("" + jsonObj);
        } catch (JSONException | RepositoryException | AccessControlException e) {
            LOG.error("Error occurred : {}" + e);
        }
    }
}
