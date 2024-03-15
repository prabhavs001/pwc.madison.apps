package com.pwc.madison.core.services;

/**
 * 
 * Service to provide caching related configuration.
 * 
 */
public interface CachingConfigurationProviderService {

    /**
     * Returns true in case dispatcher caching is enabled with AEM instance.
     * 
     * @return {@link Boolean}
     */
    public boolean isCachingEnabled();

    /**
     * Returns the Header component HTML file path in repository.
     * 
     * @return {@link String}
     */
    public String getHeaderHtmlPath();

}
