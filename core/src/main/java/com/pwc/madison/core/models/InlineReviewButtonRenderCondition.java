package com.pwc.madison.core.models;

import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Render condition to determine whether the "inline Review" button needs to be shown on the workitem console
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class InlineReviewButtonRenderCondition {

    public static final Logger LOGGER = LoggerFactory.getLogger(InlineReviewButtonRenderCondition.class);

    @Self
    private SlingHttpServletRequest request;

    private ResourceResolver requestResolver;

    private ResourceResolver madisonServiceUserResolver;

    private boolean isRendered = false;

    @OSGiService
    private ResourceResolverFactory resolverFactory;

    @PostConstruct
    protected void init() {

        requestResolver = request.getResourceResolver();

        // get the service resource resolver for reading the users under /home etc
        madisonServiceUserResolver = MadisonUtil.getResourceResolver(resolverFactory, MadisonConstants.MADISON_GENERIC_SUB_SERVICE);


        // get logged in user's authorizable ID
        Session jcrSession = requestResolver.adaptTo(Session.class);
        String loggedInUserId = jcrSession.getUserID();


        UserManager userManager = madisonServiceUserResolver.adaptTo(UserManager.class);

        try {
            // get the Authorizable for the logged in USer ID
            Authorizable user = userManager.getAuthorizable(loggedInUserId);

            Authorizable autherGroup = ((Group) userManager.getAuthorizable(MadisonConstants.USER_GROUPS_DITA_AUTHORS));

            //Set the render condition as true for the admin user
            if (MadisonUtil.isAdmin(user, userManager)) {
                isRendered = true;
            }
            
            // make sure the user is part of the OOTB 'publishers' group first, the following code execute only for non admin users
            if (!isRendered && ((Group) autherGroup).isMember(user)) {
                isRendered = true;
            }
        } catch (RepositoryException e) {
            LOGGER.error("Exception In Render Condition::", e);
        } finally {
            // close the service user resolver
            if (madisonServiceUserResolver != null && madisonServiceUserResolver.isLive()) {
                madisonServiceUserResolver.close();
            }
        }

        // set the render condition appropriately
        request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(isRendered));
    }

}
