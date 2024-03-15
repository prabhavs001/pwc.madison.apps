package com.pwc.madison.core.fusionSearch.adapter;

import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.fusionSearch.models.SearchFilter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter to map the properties of a {@link Resource} to a {@link SearchFilter}.
 */
public class SearchFilterAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchFilterAdapter.class);

    private static final String PROP_TITLE = "title";
    private static final String PROP_I18N_KEY = "i18nKey";
    private static final String PROP_VALUE = "value";

    private static final String PROP_ORDER = "order";
    private static final String PROP_NAVIGATIONLIST_VIEW_SPECIFIC = "navigationListViewSpecific";

    private static final String ALL_ORDER_KEY = "ALL";

    private static final String US_ORDER_KEY = "US";

    private Map<String,String> territoryOrderMap;

    /**
     * Returns a new {@link SearchFilter} object after mapping the required properties of a Resource.
     *
     * @param searchFilterResource
     *            {@link Resource} The properties of this resource will be added to the {@link SearchFilter}
     * @return {@link SearchFilter}
     */
    public SearchFilter adaptResourceToSearchFilter(final Resource searchFilterResource) {
        SearchFilter searchFilter = null;
        if (null != searchFilterResource) {
            final ValueMap properties = searchFilterResource.getValueMap();
            final String id = searchFilterResource.getPath();
            final String title = properties.get(PROP_TITLE, searchFilterResource.getName());
            final String value = properties.get(PROP_VALUE,String.class);
            getTerritoryOrderMap(properties.get(PROP_ORDER,String[].class));
            final String order = territoryOrderMap != null && territoryOrderMap.containsKey(ALL_ORDER_KEY) ? territoryOrderMap.get(ALL_ORDER_KEY) : "";
            final String usOrder = territoryOrderMap != null && territoryOrderMap.containsKey(US_ORDER_KEY) ? territoryOrderMap.get(US_ORDER_KEY) : "";
            final String i18nKey = properties.get(PROP_I18N_KEY, String.class);
            final Boolean navigationListViewSpecific = properties.containsKey(PROP_NAVIGATIONLIST_VIEW_SPECIFIC) ? properties.get(PROP_NAVIGATIONLIST_VIEW_SPECIFIC,Boolean.class)  : false;
            searchFilter = new SearchFilter(id, title,value, i18nKey,order,usOrder,navigationListViewSpecific);
            LOGGER.debug("SearchFilter adaptResourceToSearchFilter() : Adapting resource at path {} to SearchFilter: {}",
                    searchFilterResource.getPath(), searchFilter.toString());
        }
        return searchFilter;
    }

    private void getTerritoryOrderMap(final String[] filterOrderList) {
        territoryOrderMap = new HashMap<>();
        for(String filterOrder : filterOrderList){
            String[] orderItem = filterOrder.split(MadisonConstants.COLON);
            territoryOrderMap.put(orderItem[0], orderItem[1]);
        }
    }

}
