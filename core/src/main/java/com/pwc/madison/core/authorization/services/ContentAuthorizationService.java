package com.pwc.madison.core.authorization.services;

import java.io.IOException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.authorization.filters.PageAndAssetRequestFilter;
import com.pwc.madison.core.authorization.models.AuthorizationInformation;
import com.pwc.madison.core.authorization.servlets.ContentAuthorizationServlet;
import com.pwc.madison.core.userreg.models.User;

/**
 * Service that handles the content authorization for the request and the requested content. Currently It has been used
 * by {@link PageAndAssetRequestFilter} and {@link ContentAuthorizationServlet}. The service takes care of T&C. This service
 * also allows search bots to crawl through external user accessible content.
 * 
 */
public interface ContentAuthorizationService {

    /**
     * Returns URL (includes domain) of forbidden error page with territory and language placeholders.
     * @return {@link String}
     */
    String getForbiddenPageUrl();

    /**
     * Returns URL (includes domain) of forbidden error page for internal territories.
     * @return {@link String}
     */
    String getFullForbiddenPageUrlForInternalTerritories(String pagePath, String userType, String protectedReferrer,
                                                         final SlingHttpServletRequest slingHttpServletRequest,
                                                         final boolean isServletRequest);

    /**
     * Returns true if the user is authorized to access requested resource path. The user information is extracted from
     * passed {@link SlingHttpServletRequest}. The user will be redirected to forbidden page or gated content page if is
     * not authorized. It returns false if the user is redirected to accept T&C or is not authorized.
     * 
     * @param resourcePath
     *            {@link String} Should not be null
     * @param slingHttpServletRequest
     *            {@link SlingHttpServletRequest}
     * @param slingHttpServletResponse
     *            {@link SlingHttpServletResponse}
     * @param isServletRequest
     *            {@link Boolean} true if the authorized is to be performed using secured content servlet otherwise
     *            false
     * @return {@link Boolean}
     * @throws IOException
     *             {@link IOException}
     */
    public boolean performContentAuthorization(final String resourcePath,
            final SlingHttpServletRequest slingHttpServletRequest,
            final SlingHttpServletResponse slingHttpServletResponse, final boolean isServletRequest) throws IOException;

    /**
     * Returns {@link AuthorizationInformation} which contain information like if user is authorized to access the given
     * {@link Page} and whether the user is redirected to forbidden page or not.
     * 
     * The user is authorized only if :
     * <ul>
     * <li>User is logged in and content page belongs to private group and user belongs to this private group
     * <li>User is logged in and content page is internal only and user is internal user
     * <li>User is logged in and content is external only and free/premium and user is external user
     * <li>User is logged in and content is external only and is licensed and user is external user and has license
     * <li>User is logged in and content is external and internal only + free/premium
     * <li>User is logged in and content is external and internal only and is licensed and user is external and has
     * license to content
     * <li>User is not logged in and content is (external and internal + free) or (external only + free)
     * </ul>
     * 
     * In any other case, the user is not authorized to access page.
     * 
     * The user should be redirected to forbidden page if :
     * <ul>
     * <li>User is external/anonymous user and content is internal only
     * <li>User is external/anonymous user and content page belongs to private group
     * <li>User is external user and content is (external only) or (internal and external) and licensed and does not
     * have license to content
     * <li>User is internal user and content is external only and free/premium or is licensed
     * <li>User is internal user and content page belongs to private group and user does not belong to that private
     * group
     * <li>User is anonymous and content is external and internal only + premium
     * <li>User is anonymous and content is external and internal only and is licensed
     * </ul>
     * 
     * In any other case, the user is redirected to Gated content page.
     * 
     * 
     * @param page
     *            {@link Page}
     * @param user
     *            {@link User}
     * @return {@link AuthorizationInformation}
     */
    public AuthorizationInformation getUserAuthorization(final Page page, final User user);

}
