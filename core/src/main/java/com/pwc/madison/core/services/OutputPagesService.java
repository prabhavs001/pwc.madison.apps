/**
 *
 */
package com.pwc.madison.core.services;

import org.apache.sling.api.resource.ResourceResolver;

public interface OutputPagesService {

    /**
     * Method to get number of output pages for input ditamap
     *
     * @param resolver
     *            Resource Resolver
     * @param assetPath
     *            asset path
     * @return number of pages
     */
    Object getNumberOfPages(ResourceResolver resolver, String assetPath);

    /**
     * Method to get number of topics for input ditamap
     *
     * @param resolver
     *            Resource Resolver
     * @param assetPath
     *            asset path
     * @return topics count
     */
    Object getTopicsCount(ResourceResolver resolver, String assetPath);

}
