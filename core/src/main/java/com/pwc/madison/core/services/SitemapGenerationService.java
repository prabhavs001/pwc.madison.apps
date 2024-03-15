package com.pwc.madison.core.services;

import com.pwc.madison.core.exception.NoDataFoundException;

import java.io.PrintWriter;

/**
 *
 * The service generates sitemap files for Madison site.
 *
 */
public interface SitemapGenerationService {

    /**
     * Generates Sitemap XML files with given filename for Viewpoint site under each of the homepage of the site like
     * /content/pwc-madison/<territory>/<language>.
     *  @param fileName
     *            {@link String} name of the sitemap file
     * @param totalSitemapFiles
     *            {@link Integer} number of Sitemap files per locale
     * @param urlsPerSitemapFiles
     * {@link Integer} number of URL entries per sitemap file
     * @param replicationMinutes
     * {@link Integer} if given incremental sitemap files are created. Can be null in case general sitemap
     * files are to be created. replication minutes defines the minutes taken from current time which is to
     * identify pages that got replicated in last replication minutes.
     * @param urlsPerLanguage
     * {@link Integer} number of URL entries per language
     * @param territory
     * {@link String} to create territory specific file
     * @param writer
     */
    public void generateSitemapXml(final String fileName, final int totalSitemapFiles, final int urlsPerSitemapFiles,
                                   final Integer replicationMinutes, final Integer urlsPerLanguage, final String territory, PrintWriter writer);

    /**
     * Generates delete Sitemap files with given filename for Viewpoint site under each of the homepage of the site like
     * /content/pwc-madison/<territory>/<language>.
     *
     * @param fileName
     *            {@link String} name of the sitemap file
     * @param replicationMinutes
     *            {@link Integer} replication minutes defines the minutes taken from current time which is to identify
     *            pages that got deleted/deactivated in last replication minutes.
     * @param urlsPerLanguage
     *            {@link Integer} number of URL entries per language
     *
     * @param language
     *            {@link String} to create language specific file
     * @param writer
     */
    public void generateDeleteSitemap(final String fileName, final Integer replicationMinutes,
            final Integer urlsPerLanguage,final String language,PrintWriter writer);

}
