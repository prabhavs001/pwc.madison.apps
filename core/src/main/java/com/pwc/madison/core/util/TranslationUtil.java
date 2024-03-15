package com.pwc.madison.core.util;

import java.util.Collections;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.constants.MadisonConstants;

public class TranslationUtil {
    private static final Logger LOG = LoggerFactory.getLogger(TranslationUtil.class);

    private TranslationUtil() {
    }

    /**
     * Get territory groups/users from the given folder
     *
     * @param resolver
     * @param destResource
     * @param principalName
     * @param includeMembers
     */
    public static Set<Authorizable> getTerritoryGroups(final ResourceResolver resolver, final Resource destResource,
            final String principalName, final Boolean includeMembers) throws RepositoryException {
        if (null == resolver || null == destResource || null == principalName || null == includeMembers) {
            return Collections.emptySet();
        }

        final UserManager userManager = resolver.adaptTo(UserManager.class);
        final Authorizable authorGroup = userManager.getAuthorizable(principalName);
        // Get all users/groups with Editor Role for this subscriber
        Set<Authorizable> notificationGroups = MadisonUtil
                .checkIfGroupHasPermissionOnFolder(destResource, userManager, authorGroup, resolver,
                        MadisonConstants.PN_EDITOR, includeMembers);

        if (notificationGroups.isEmpty() && !destResource.getName().equals(MadisonConstants.MADISON_DAM_ROOT)) {
            final Resource parentPath = destResource.getParent();
            notificationGroups = getTerritoryGroups(resolver, parentPath, principalName, includeMembers);
        }

        return notificationGroups;
    }

}
