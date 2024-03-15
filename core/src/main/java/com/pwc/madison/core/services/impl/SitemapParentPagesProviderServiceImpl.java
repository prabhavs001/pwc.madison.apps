package com.pwc.madison.core.services.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.beans.SitemapParent;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.SitemapParentPagesProviderService;
import com.pwc.madison.core.services.impl.SitemapParentPagesProviderServiceImpl.SitemapRootPathConfig;
import com.pwc.madison.core.util.MadisonUtil;

@Component(
        service = SitemapParentPagesProviderService.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
@Designate(ocd = SitemapRootPathConfig.class)
public class SitemapParentPagesProviderServiceImpl implements SitemapParentPagesProviderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SitemapParentPagesProviderServiceImpl.class);

    private String[] sitemapPagesRootPaths;
    private String[] incrementalPagesRootPaths;
    private String[] deletePagesRootPaths;
    private List<String> allowedReplicationAgents;

    private Map<String, List<SitemapParent>> sitemapParentPagesMap;
    private Map<String, Map<String, List<SitemapParent>>> incrementalSitemapParentPagesMap;
    private Map<String, Map<String, List<SitemapParent>>> deleteSitemapParentPagesMap;

    @Activate
    @Modified
    protected void Activate(final SitemapRootPathConfig sitemapRootPathConfig) {
        LOGGER.info("SitemapParentPagesProviderServiceImpl : Entered Activate/Modify");
        sitemapPagesRootPaths = sitemapRootPathConfig.madison_sitemap_root_paths();
        incrementalPagesRootPaths = sitemapRootPathConfig.madison_incremental_sitemap_root_paths();
        deletePagesRootPaths = sitemapRootPathConfig.madison_delete_sitemap_root_paths();
        String[] allowedAgents = sitemapRootPathConfig.madison_allowed_replication_agents();
        allowedReplicationAgents = Arrays.asList(allowedAgents != null ? allowedAgents : new String[] {});
        LOGGER.debug("SitemapParentPagesProviderServiceImpl Activate() Sitemap Viewpoint Pages Root Paths : {}",
                sitemapPagesRootPaths);
        LOGGER.debug(
                "SitemapParentPagesProviderServiceImpl Activate() Incremental Sitemap Viewpoint Pages Language Root Paths : {}",
                incrementalPagesRootPaths);
        LOGGER.debug(
                "SitemapParentPagesProviderServiceImpl Activate() Delete Sitemap Viewpoint Pages Language Root Paths : {}",
                deletePagesRootPaths);
        LOGGER.debug(
                "SitemapParentPagesProviderServiceImpl Activate() Allowed Replication Agents : {}",
                allowedReplicationAgents);
        createParentPagesMap();
    }

    @ObjectClassDefinition(name = "PwC Viewpoint Sitemap Paths Configuration")
    public @interface SitemapRootPathConfig {

        @AttributeDefinition(
                name = "Sitemap Viewpoint Pages Language Root Paths",
                description = "Viewpoint pages root path from where it starts searching for parent roots for which sitemap XML files are to be created.",
                type = AttributeType.STRING,
                cardinality = Integer.MAX_VALUE)
        String[] madison_sitemap_root_paths();

        @AttributeDefinition(
                name = "Incremental Sitemap Viewpoint Pages Language Root Paths",
                description = "Viewpoint pages root path from where it starts searching for parent roots for which sitemap XML files are to be created. The format of the path should be language:path like en:/content/pwc-madison/us/en. The language is required in order to apply the sitemap entries per language limit required by SnP",
                type = AttributeType.STRING,
                cardinality = Integer.MAX_VALUE)
        String[] madison_incremental_sitemap_root_paths();

        @AttributeDefinition(
                name = "Delete Sitemap Viewpoint Pages Language Root Paths",
                description = "Viewpoint pages root path from where it starts searching for parent roots for which delete sitemap files are to be created. The format of the path should be language:path like en:/content/pwc-madison/us/en. The language is required in order to apply the sitemap entries per language limit required by SnP",
                type = AttributeType.STRING,
                cardinality = Integer.MAX_VALUE)
        String[] madison_delete_sitemap_root_paths();

        @AttributeDefinition(
                name = "Allowed Replication Agents ID",
                description = "Replication Agent IDs of all replication agents that must replicate sitemap files",
                type = AttributeType.STRING,
                cardinality = Integer.MAX_VALUE)
        String[] madison_allowed_replication_agents();
    }

    /**
     * Create Parent Pages Map for sitemap and incremental sitemap.
     */
    private void createParentPagesMap() {
        sitemapParentPagesMap = new HashMap<String, List<SitemapParent>>();
        if (sitemapPagesRootPaths != null && sitemapPagesRootPaths.length > 0) {
            addToMap(sitemapParentPagesMap, sitemapPagesRootPaths);
        }
        incrementalSitemapParentPagesMap = new HashMap<String, Map<String, List<SitemapParent>>>();
        if (incrementalPagesRootPaths != null && incrementalPagesRootPaths.length > 0) {
            addToLanguageMap(incrementalSitemapParentPagesMap, incrementalPagesRootPaths);
        }
        deleteSitemapParentPagesMap = new HashMap<String, Map<String, List<SitemapParent>>>();
        if (deletePagesRootPaths != null && deletePagesRootPaths.length > 0) {
            addToLanguageMap(deleteSitemapParentPagesMap, deletePagesRootPaths);
        }
        LOGGER.info("SitemapParentPagesProviderServiceImpl createParentPagesMap() : Sitemap Map created is {}",
                sitemapParentPagesMap);
        LOGGER.info(
                "SitemapParentPagesProviderServiceImpl createParentPagesMap() : Incremental Sitemap Map created is {}",
                incrementalSitemapParentPagesMap);
        LOGGER.info("SitemapParentPagesProviderServiceImpl createParentPagesMap() : Delete Sitemap Map created is {}",
                deleteSitemapParentPagesMap);
    }

    private void addToLanguageMap(final Map<String, Map<String, List<SitemapParent>>> incrementalSitemapParentPagesMap,
            final String[] rootPaths) {
        for (final String rootPath : rootPaths) {
            int colonIndex = rootPath.indexOf(":");
            if (colonIndex > 0) {
                String language = rootPath.substring(0, colonIndex);
                String path = rootPath.substring(colonIndex + 1, rootPath.length());
                if (!incrementalSitemapParentPagesMap.containsKey(language)) {
                    LOGGER.debug(
                            "SitemapParentPagesProviderServiceImpl  addToLanguageMap() : Adding Language {} in Incremental Parent Pages Map",
                            language);
                    incrementalSitemapParentPagesMap.put(language, new LinkedHashMap<String, List<SitemapParent>>());
                }
                addToMap(incrementalSitemapParentPagesMap.get(language), path);
            }
        }
    }

    /**
     * Iterate over the rootPath to get the list of parent paths where the sitemap files are to be created.
     * 
     * @param parentPagesMap
     *            {@link Map} where key is {@link String} and value is {@link List} of {@link SitemapParent}
     * @param rootPaths
     *            {@link String[]}
     */
    private void addToMap(final Map<String, List<SitemapParent>> parentPagesMap, final String[] rootPaths) {
        for (final String rootPath : rootPaths) {
            addToMap(parentPagesMap, rootPath);
        }
    }

    /**
     * Create {@link SitemapParent} out of given path and add it to the given {@link Map}. For dita page root path, it
     * checks if its home page path is already present in {@link Map} otherwise do not add it to {@link Map}.
     * 
     * @param parentPagesMap
     *            {@link Map} where key is {@link String} and value is {@link List} of {@link SitemapParent}
     * @param path
     *            {@link String}
     */
    private void addToMap(final Map<String, List<SitemapParent>> parentPagesMap, final String path) {
        LOGGER.debug(
                "SitemapParentPagesProviderServiceImpl  addToMap() : Language Page {} calling mathod  addToMap() to decided if can to be added to map",
                path);
        boolean isDitaPage = path.startsWith(MadisonConstants.MADISON_SITES_ROOT);
        String homePagePath = getHomePage(parentPagesMap, path, isDitaPage);
        if (null != homePagePath) {
            LOGGER.debug("SitemapParentPagesProviderServiceImpl  addToMap() : Language Page {} is added to map", path);
            final SitemapParent sitemapParent = new SitemapParent(path, !isDitaPage);
            if (!parentPagesMap.containsKey(homePagePath)) {
                parentPagesMap.put(homePagePath, new ArrayList<>());
            }
            parentPagesMap.get(homePagePath).add(sitemapParent);
        }
    }

    /**
     * Returns the home page path of the path provided where the sitemap file is to be created. Returns null if the page
     * path is dita page path and its landing home page path does not exists.
     * 
     * @param pagePath
     *            {@link String}
     * @param isDitaPage
     *            {@link Boolean} whether pagePath is dita page path or not
     * @return {@link String} home page path of the page path provided or null.
     */
    private String getHomePage(final Map<String, List<SitemapParent>> parentPagesMap, final String pagePath,
            final boolean isDitaPage) {
        if (isDitaPage) {
            final String landingHomePagePath = MadisonUtil
                    .getHomePagePathFromLocale(MadisonUtil.getLocaleForPath(pagePath));
            if (parentPagesMap.containsKey(landingHomePagePath)) {
                return landingHomePagePath;
            } else {
                LOGGER.debug(
                        "SitemapParentPagesProviderServiceImpl  getHomePage() : Returning home page null for dita page '{}' as its landing homepage in map does not exists",
                        pagePath);
                return null;
            }
        }
        return pagePath;
    }

    @Override
    public Map<String, List<SitemapParent>> getSitmapParentPagesMap() {
        return sitemapParentPagesMap;
    }

    @Override
    public Map<String, Map<String, List<SitemapParent>>> getIncrementalSitmapParentPagesMap() {
        return incrementalSitemapParentPagesMap;
    }

    @Override
    public Map<String, Map<String, List<SitemapParent>>> getDeleteSitmapParentPagesMap() {
        return deleteSitemapParentPagesMap;
    }

    @Override
    public List<String> getReplicationAgents() {
        return allowedReplicationAgents;
    }

}
