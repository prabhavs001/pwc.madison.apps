package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.constants.DITAConstants;

/**
 * Servlet to check whether a user has a specifc permission on an asset
 */
@Component(immediate = true,
           service = Servlet.class,
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = { "service.description=Checks logged in user permission on an asset", "sling.servlet.methods=GET",
                   "sling.servlet.paths=/bin/pwc-madison/checkuserpermission" })
public class CheckUserPermissionServlet extends SlingSafeMethodsServlet {
    private static final String PERMISSION = "permission";
    private static final String WRITE = "write";
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        RequestParameterMap requestParameterMap = request.getRequestParameterMap();
        String asset = StringUtils.EMPTY;
        String permission = StringUtils.EMPTY;
        boolean hasPermission = false;
        if (requestParameterMap.containsKey(DITAConstants.INPUT_PAYLOAD_ASSET)) {
            asset = requestParameterMap.getValue(DITAConstants.INPUT_PAYLOAD_ASSET).getString();
        }
        asset = java.net.URLDecoder.decode(asset, StandardCharsets.UTF_8.name());
        if (requestParameterMap.containsKey(PERMISSION)) {
            permission = requestParameterMap.getValue(PERMISSION).getString();
            if (permission.equals(WRITE)) {
                permission = Privilege.JCR_WRITE;
            } else {
                permission = Privilege.JCR_READ;
            }
        }
        try {
            ResourceResolver resourceResolver = request.getResourceResolver();
            Session userSession = resourceResolver.adaptTo(Session.class);
            AccessControlManager acMgr = userSession.getAccessControlManager();
            hasPermission = userSession.getAccessControlManager()
                    .hasPrivileges(asset, new Privilege[] { acMgr.privilegeFromName(permission) });
        } catch (RepositoryException e) {
            log.error("RepositoryException in CheckUserPermissionServlet {}", e);
        }
        PrintWriter printWriter = response.getWriter();
        printWriter.print(hasPermission);
    }
}
