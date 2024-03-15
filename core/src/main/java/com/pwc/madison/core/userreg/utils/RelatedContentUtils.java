package com.pwc.madison.core.userreg.utils;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.pwc.madison.core.authorization.models.AuthorizationInformation;
import com.pwc.madison.core.authorization.services.ContentAuthorizationService;
import com.pwc.madison.core.userreg.models.User;

public class RelatedContentUtils {
    private final static Logger LOG = LoggerFactory.getLogger(RelatedContentUtils.class);

    /**
     * 
     * @param contentAuthorizationService
     * @param resourceResolver
     * @param link
     * @param currentUser
     * @return
     */
    public static boolean isUserHasAccessToRelatedItem(ContentAuthorizationService contentAuthorizationService,
            PageManager pageManager, String link, User currentUser) {
        if (pageManager != null) {
            final Page page = pageManager.getContainingPage(link);
            final AuthorizationInformation pageAuthorizationInformation = isAllowed(contentAuthorizationService,currentUser, page);
            return pageAuthorizationInformation != null ? pageAuthorizationInformation.isAuthorized() : false;
        }
        return false;
    }

    /**
     * 
     * @param contentAuthorizationService
     * @param currentUser
     * @param page
     * @return
     */
    private static AuthorizationInformation isAllowed(ContentAuthorizationService contentAuthorizationService,
            User currentUser, final Page page) {
        AuthorizationInformation pageAuthorizationInformation = null;
        if (page != null) {
            if (Objects.isNull(contentAuthorizationService)) {
                pageAuthorizationInformation = new AuthorizationInformation(true, false, false);
                LOG.debug("contentAuthorizationService is null so setting the default Authorization");
            } else {
                pageAuthorizationInformation = contentAuthorizationService.getUserAuthorization(page, currentUser);
                LOG.debug("contentAuthorizationService is Authorizatized based on value {} ",
                        pageAuthorizationInformation.isAuthorized());
            }
        }
        return pageAuthorizationInformation;
    }
}
