package com.pwc.madison.core.services;

/**
 * The service provide domains for the AEM instance.
 */
public interface MadisonDomainsService {

    /**
     * Returns the default domain of Viewpoint.
     * 
     * @return {@link String}
     */
    public String getDefaultDomain();

    /**
     * Returns the publish instance domain.
     * 
     * @return {@link String}
     */
    public String getPublishDomain();

    /**
     * Returns the published URL of the given page. It takes care of URL shortening and adding of HTML extension to the
     * URL.
     * 
     * @param pagePath
     *            {@link String}
     * @param addExtension
     *            {@link boolean} specifies whether to add extension to final published URL or not.
     * @return {@link String}
     */
    public String getPublishedPageUrl(final String pagePath, final boolean addExtension);

}
