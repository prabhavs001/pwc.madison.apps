package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Servlet to check if the logged in user is a member of the given groups
 */

@Component(service = Servlet.class,
           property = { Constants.SERVICE_DESCRIPTION + "=Role Render Condition Servlet",
                   "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                   "sling.servlet.paths=" + "/bin/pwc-madison/role-rendercondition" })
public class RoleRenderConditionServlet extends SlingSafeMethodsServlet {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private XSSAPI xssAPI;
    
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    @Override
    protected void doGet(@Nonnull final SlingHttpServletRequest request,
            @Nonnull final SlingHttpServletResponse response) throws ServletException, IOException {

        final PrintWriter writer = response.getWriter();

        final User currentUser = request.getResourceResolver().adaptTo(User.class);

        if (null == currentUser) {
            writer.write("false");
            writer.close();
            return;
        }

        final String allowAdminAccess = request.getParameter("allowAdmin");

        if (currentUser.isAdmin()) {
            final String res = Boolean.valueOf(allowAdminAccess) ? "true" : "false";
            writer.write(res);
            writer.close();
            return;
        }

        final ResourceResolver resourceResolver = MadisonUtil
                .getResourceResolver(resourceResolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());

        final UserManager userManager = resourceResolver.adaptTo(UserManager.class);

        if (null == userManager) {
            writer.write("false");
            writer.close();
            return;
        }

        final String[] roles = request.getParameterValues("roles");

        if (ArrayUtils.isEmpty(roles)) {
            writer.write("false");
            writer.close();
            return;
        }

        Group reviewers = null;

        try {
            reviewers = (Group) userManager.getAuthorizable(MadisonConstants.REVIEWERS_ROLE);
        } catch (final RepositoryException e) {
            LOG.error("Unable to get approvers and reviewers groups ", e);
        }

        if (null == reviewers) {
            writer.write("false");
            writer.close();
            return;
        }

        // check if the current user is a member of given roles
        for (final String role : roles) {
            try {
                final Group group = (Group) userManager.getAuthorizable(role);

                if (null == group) {
                    continue;
                }

                if (group.isMember(currentUser) && !(reviewers.isMember(currentUser))) {
                    writer.write("true");
                    writer.close();

                    if (resourceResolver.isLive()) {
                        resourceResolver.close();
                    }

                    return;
                }
            } catch (final RepositoryException e) {
                LOG.error("Unable to get role {}", xssAPI.encodeForHTML(role), e);
            }
        }

        if (resourceResolver.isLive()) {
            resourceResolver.close();
        }

        writer.write("false");
        writer.close();
    }
}
