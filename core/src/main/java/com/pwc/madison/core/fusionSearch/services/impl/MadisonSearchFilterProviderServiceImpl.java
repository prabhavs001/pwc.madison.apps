package com.pwc.madison.core.fusionSearch.services.impl;

import com.day.cq.i18n.I18n;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.fusionSearch.enums.SearchFilterType;
import com.pwc.madison.core.fusionSearch.models.SearchFilter;
import com.pwc.madison.core.fusionSearch.services.MadisonSearchFilterProviderService;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import com.pwc.madison.core.fusionSearch.services.impl.MadisonSearchFilterProviderServiceImpl.MadisonSearchFilterConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(
        service = { MadisonSearchFilterProviderService.class, EventHandler.class },
        immediate = true,
        property = { EventConstants.EVENT_TOPIC + "=org/apache/sling/api/resource/Resource/*",
                EventConstants.EVENT_FILTER + "=(path=/content/pwc-madison/global/reference-data/fusion-search/*)" })
@Designate(ocd = MadisonSearchFilterConfiguration.class)
public class MadisonSearchFilterProviderServiceImpl implements MadisonSearchFilterProviderService , EventHandler{

    private static final Logger LOGGER = LoggerFactory.getLogger(MadisonSearchFilterProviderService.class);

    private final String EVENT_PATH = "path";

    private final String SEARCH_FILTER_TITLE_KEY = "title";

    private final String SEARCH_FILTER_ORDER_KEY = "order";

    private final String US_TERRITORY_CODE = "us";

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Reference
    private XSSAPI xssAPI;

    private Map<SearchFilterType, String> searchFilterBasePaths;

    private Map<SearchFilterType, Map<String, SearchFilter>> allSearchFilterMaps;
    
    private int signalStayTimeframe;

    @Activate
    @Modified
    protected void Activate(final MadisonSearchFilterConfiguration madisonSearchFilterConfiguration) {
        LOGGER.info("MadisonSearchFilterProviderService : Entered Activate/Modify");
        searchFilterBasePaths = new HashMap<>();
        searchFilterBasePaths.put(SearchFilterType.FILTER_SORT,
                madisonSearchFilterConfiguration.madison_sort_search_filter_base_path());
        searchFilterBasePaths.put(SearchFilterType.FILTER_FACETS,
                madisonSearchFilterConfiguration.madison_facets_search_filter_base_path());
        signalStayTimeframe = madisonSearchFilterConfiguration.madison_search_signal_stay_timeframe();
        allSearchFilterMaps = new HashMap<>();
        createSearchFilterMap(null);
    }

    @ObjectClassDefinition(name = "PwC Viewpoint Search Filter Configuration")
    public @interface MadisonSearchFilterConfiguration {

        @AttributeDefinition(
                name = "Search Filter SORT Base Path",
                description = "Content path under which search SORT filter's data is stored")
        String madison_sort_search_filter_base_path() default "/content/pwc-madison/global/reference-data/fusion-search/filter-sort";

        @AttributeDefinition(
                name = "Search Filter FACETS Base Path",
                description = "Content path under which search FACETS filter's data is stored")
        String madison_facets_search_filter_base_path() default "/content/pwc-madison/global/reference-data/fusion-search/filter-facets";

        @AttributeDefinition(
                name = "Search Signal Page/Preview Stay Timeframe",
                description = "Timeframe in milliseconds after which the LW signal should be sent if user stags on searched page/preview")
        int madison_search_signal_stay_timeframe() default 30000;
}

    /**
     * Creates the Search Filter {@link Map}.
     *
     * @param changedResourcePath
     *            {@link String} resource path that is changed, the path defines which type of search filter {@Map} is to
     *            be created. If null, all the search filter type {@link SearchFilter} Maps are created.
     */
    private void createSearchFilterMap(final String changedResourcePath) {
        LOGGER.info("MadisonSearchFilterProviderService : Entered createSearchFilterMap");
        ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                MadisonConstants.MADISON_READ_SUB_SERVICE);
        if (null != resourceResolver) {
            for (Map.Entry<SearchFilterType, String> searchFilterPathEntry : searchFilterBasePaths.entrySet()) {
                if (null == changedResourcePath || changedResourcePath.startsWith(searchFilterPathEntry.getValue())) {
                    allSearchFilterMaps.put(searchFilterPathEntry.getKey(), new HashMap<String, SearchFilter>());
                    updateSearchFilterMap(searchFilterPathEntry.getValue(), resourceResolver,
                            allSearchFilterMaps.get(searchFilterPathEntry.getKey()));
                }
            }
            resourceResolver.close();
        }
        LOGGER.debug("MadisonSearchFilterProviderService createSearchFilterMap() : Search Filter Map {}",
                allSearchFilterMaps);
    };

    /**
     * Updates the given allSearchFilterMap with the {@link SearchFilter} at the given base
     * path.
     *
     * @param path
     *            {@link String} the base path of the search filter.
     * @param resourceResolver
     *            {@link ResourceResolver}
     * @param allSearchFilterMaps
     *            {@link Map} which is to be updated.
     */
    private void updateSearchFilterMap(final String path, final ResourceResolver resourceResolver,
                 Map<String,SearchFilter> allSearchFilterMaps) {
        LOGGER.debug(
                "MadisonSearchFilterProviderService updateSearchFilterMap() : Updating Search Filter Map for path {}",
                path);
        Resource searchFilterBasePathResource = resourceResolver.getResource(path);
        if (null != searchFilterBasePathResource) {
            for (Resource searchResource : searchFilterBasePathResource.getChildren()) {
                for (Resource searchFilterResource : searchResource.getChildren()) {
                    SearchFilter searchFilter = searchFilterResource.adaptTo(SearchFilter.class);
                    allSearchFilterMaps.put(searchFilter.getId(), searchFilter);
                }
            }
        }
    }

    @Override
    public void handleEvent(Event event) {
        LOGGER.info("MadisonSearchFilterProviderService : Entered handleEvent");
        createSearchFilterMap((String) event.getProperty(EVENT_PATH));
    }

    /**
     * Sets the translated title for the given {@link Map} of {@link SearchFilter} for given locale.
     *
     * @param searchFilterMap
     *            {@link SearchFilter}
     * @param locale
     *            {@link String}
     * @param i18n
     *            {@link I18n}
     */
    private void setTranslatedTitle(final Map<String, SearchFilter> searchFilterMap, final String locale,
                                    final I18n i18n) {
        if (null != searchFilterMap && null != i18n && null != locale) {
            for (Map.Entry<String, SearchFilter> entry : searchFilterMap.entrySet()) {
                setTranslatedTitle(entry.getValue(), locale, i18n);
            }
        }
    }

    /**
     * Sets the translated title for the given {@link SearchFilter} for given locale.
     *
     * @param searchFilter
     *            {@link SearchFilter}
     * @param locale
     *            {@link String}
     * @param i18n
     *            {@link I18n}
     */
    private void setTranslatedTitle(final SearchFilter searchFilter, final String locale, final I18n i18n) {
        if (null != searchFilter && null != i18n && null != locale) {
            String translatedTitle = i18n.get(searchFilter.getI18nKey());
            translatedTitle = translatedTitle.equals(searchFilter.getI18nKey()) ? searchFilter.getTitle() : translatedTitle;
            searchFilter.getTranslatedTitles().put(locale, translatedTitle);
        }
    }

    @Override
    public Map<String, SearchFilter> getAllSearchFilterMap(final SearchFilterType searchFilterType,
                                                        final String locale, final I18n i18n) {
        if (null != allSearchFilterMaps && null != allSearchFilterMaps.get(searchFilterType)) {
            setTranslatedTitle(allSearchFilterMaps.get(searchFilterType), locale, i18n);
        }
        LOGGER.debug("MadisonSearchFilterProviderService getAllSearchFilterMap() : Returning Search Filter Map {}",
                allSearchFilterMaps.get(searchFilterType));
        return allSearchFilterMaps.get(searchFilterType);
    }

    @Override
    public Map<String,Map<String,String>> getSearchFilterMapByLocale(final SearchFilterType searchFilterType, final I18n i18n, final String territoryCode) {
          Map<String,Map<String,String>> searchFilterMapByLocale = new HashMap<>();
          Map<String,String> searchFilterSubMap;
        if (null != allSearchFilterMaps && null != allSearchFilterMaps.get(searchFilterType)) {
            Map<String, SearchFilter> searchFilterMap = allSearchFilterMaps.get(searchFilterType);
            if (null != searchFilterMap && null != i18n) {
                for (Map.Entry<String, SearchFilter> entry : searchFilterMap.entrySet()) {
                    SearchFilter searchFilter = entry.getValue();
                    searchFilterSubMap = new HashMap<>();
                    if (null != searchFilter) {
                        String translatedTitle = i18n.get(searchFilter.getI18nKey());
                        translatedTitle = translatedTitle.equals(searchFilter.getI18nKey()) ? searchFilter.getTitle() : translatedTitle;
                        String order = territoryCode.equals(US_TERRITORY_CODE) ? searchFilter.getUsOrder() : searchFilter.getOrder();
                        searchFilterSubMap.put(SEARCH_FILTER_TITLE_KEY, translatedTitle);
                        searchFilterSubMap.put(SEARCH_FILTER_ORDER_KEY,order);
                    }
                    searchFilterMapByLocale.put(searchFilter.getValue(), searchFilterSubMap);
                }
            }
        }
        return searchFilterMapByLocale;
    }

    @Override
    public int getSearchSignalStayTimeframe() {
        return signalStayTimeframe;
    }
}
