package com.pwc.madison.core.userreg.services;

import org.apache.sling.api.SlingHttpServletRequest;

/**
 * The service provides user favorite list configurations.
 *
 */
public interface FavoriteListService {

    /**
     * Returns true if favorite list feature is enabled otherwise false.
     * 
     * @return {@link Boolean}
     */
    public boolean isFavoriteListEnabled();

    /**
     * Returns favorite list limit.
     * 
     * @return {@link Integer}
     */
    public int getFavoriteListLimit();

    /**
     * Returns favorite list panel limit. This represents the number of items in default list that needs to be shown in
     * panel on each page.
     * 
     * @return {@link Integer}
     */
    public int getFavoritePanelListLimit();

    /**
     * Returns favorite list items that should be shown per pagination page in favorite list page.
     * 
     * @return {@link Integer}
     */
    public int getFavoriteListLimitPerPage();

    /**
     * Returns favorite list cache time in local storage. the list will be updated if cache older than given minutes.
     * 
     * @return {@link int}
     */
    public int getLocalStorageCacheTimeInMinutes();

    /**
     * Returns favorite list page for the given page path and request.
     * 
     * @param SlingHttpServletRequest
     *            {@link SlingHttpServletRequest}
     * @param pagePath
     *            {@link String}
     * @return {@link String}
     */
    public String getFavoriteListPage(final SlingHttpServletRequest request, final String pagePath);

}
