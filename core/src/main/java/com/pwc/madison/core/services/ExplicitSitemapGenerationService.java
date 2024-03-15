package com.pwc.madison.core.services;

import java.io.PrintWriter;

/**
 *
 * The service generates sitemap files explicitly for Viewpoint site.
 *
 */
public interface ExplicitSitemapGenerationService {

    /**
     * Generic method for creating sitemap explicitly
     * @param territoryOrLang String
     *                  for territory specific sitemap generation
     * @param printWriter PrintWriter
     *                    to print messages in response
    */
    public void generateExplicitSitemap(final String territoryOrLang,PrintWriter printWriter);
}
