package com.pwc.madison.core.fusionSearch.services;

import com.day.cq.i18n.I18n;
import com.pwc.madison.core.fusionSearch.enums.SearchFilterType;
import com.pwc.madison.core.fusionSearch.models.SearchFilter;

import java.util.Map;

/**
 * The service provides the Search Filter Service.
 *
 */
public interface MadisonSearchFilterProviderService {

    /**
     * Returns {@link Map} contains key as {@link String} ID/Path of the {@link SearchFilterType} SearchFilter and
     * value as {@link SearchFilter}.
     *
     * @param searchFilterType
     *            {@link SearchFilterType} defines the type of search filter
     * @param locale
     *            {@link String} required in case the localized title for {@link SearchFilter} is required
     * @param i18n
     *            {@link I18n} required in case the localized title for {@link SearchFilter} is required. If any of the
     *            localeString or I18n is null, the localized title is not computed
     * @return {@link Map} of {@link SearchFilterType} SearchFilter
     */
    public Map<String, SearchFilter> getAllSearchFilterMap(final SearchFilterType searchFilterType,
                                                          final String locale, final I18n i18n);
    /**
     * Returns {@link Map} contains key as {@link String} Value of the {@link SearchFilterType} SearchFilter and
     * value as {@link String} Translated Title based on locale.
     *
     * @param searchFilterType
     *            {@link SearchFilterType} defines the type of search filter
     * @param i18n
     *            {@link I18n} required in case the localized title for {@link SearchFilter} is required. If any of the
     *            localeString or I18n is null, the localized title is not computed
     * @param territoryCode
     *           {@link String} defines the current page territory code based on page locale
     * @return {@link Map} of {@link String} SearchFilter
     */
    public Map<String,Map<String,String>> getSearchFilterMapByLocale(final SearchFilterType searchFilterType, final I18n i18n, final String territoryCode);
    
    /**
     * Returns timeframe in milliseconds after which the LW signal should be sent if user stags on searched page/preview.
     * 
     * @return {@link Integer} timeframe in milliseconds
     */
    public int getSearchSignalStayTimeframe();

}
