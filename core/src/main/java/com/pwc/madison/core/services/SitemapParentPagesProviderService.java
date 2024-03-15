package com.pwc.madison.core.services;

import java.util.List;
import java.util.Map;

import com.pwc.madison.core.beans.SitemapParent;

/**
 * 
 * The service provides the list of root paths in Viewpoint Site where the sitemap XML file is to be created.
 *
 */
public interface SitemapParentPagesProviderService {

    /**
     * Returns a {@link Map} where key is the root path where a sitemap XMl file is to be created and value is the
     * {@link List} of {@link SitemapParent} which provides list of paths whose child pages are to be includes in
     * sitemap file. Consider an example, where the sitemap file is to be created under us/en locale and it contains the
     * urls of the pages from us/en landing pages and ditaroot us/en pages.
     * 
     * @return {@link Map} where key is {@link String} and value is {@link List} of {@link SitemapParent}
     */
    public Map<String, List<SitemapParent>> getSitmapParentPagesMap();

    /**
     * Returns a {@link Map} where key is the language and value is the {@link Map} of which where key is the root path
     * where a sitemap XMl file is to be created and value is {@link List} of {@link SitemapParent} which provides list
     * of paths whose child pages are to be includes in incremental sitemap file. Consider an example, where the sitemap
     * file is to be created under us/en locale and it contains the urls of the pages from us/en landing pages and
     * ditaroot us/en pages.
     * 
     * @return {@link Map} where key is {@link String} and value is {@link Map} of which key is {@link String} and value
     *         is {@link SitemapParent}
     */
    public Map<String, Map<String, List<SitemapParent>>> getIncrementalSitmapParentPagesMap();

    /**
     * Returns a {@link Map} where key is the language and value is the {@link Map} of which where key is the root path
     * where a sitemap delete file is to be created and value is {@link List} of {@link SitemapParent} which provides list
     * of paths whose child pages are to be includes in delete sitemap file. Consider an example, where the sitemap
     * file is to be created under us/en locale and it contains the urls of the pages from us/en landing pages and
     * ditaroot us/en pages.
     * 
     * @return {@link Map} where key is {@link String} and value is {@link Map} of which key is {@link String} and value
     *         is {@link SitemapParent}
     */
    public Map<String, Map<String, List<SitemapParent>>> getDeleteSitmapParentPagesMap();
    
    /**
     * Returns list of replication agents IDs which should replicate sitemap files.
     * 
     * @return {@link List} of {@link String}
     */
    public List<String> getReplicationAgents();

}
