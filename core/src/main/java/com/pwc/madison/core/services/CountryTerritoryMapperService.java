package com.pwc.madison.core.services;

import java.util.List;
import java.util.Map;

import com.pwc.madison.core.models.PodcastWidget;
import com.pwc.madison.core.models.Territory;
import org.apache.sling.api.SlingHttpServletRequest;

/**
 * The Interface CountryTerritoryMapperService Provides Country and territory {@link Map} extracted from the data in
 * territory reference nodes.
 */
public interface CountryTerritoryMapperService {

    /**
     * Gets the {@link Territory} for the given country code.
     *
     * @param countryCode
     *            {@link String} Code of the country
     * @return {@link Territory} returns null if no territory exists for the country code
     */
    public Territory getTerritoryByCountry(String countryCode);

    /**
     * Gets the country code to {@link Territory} {@link Map}.
     *
     * @return {@link Map}
     */
    public Map<String, Territory> getCountryToTerritoryMap();

    /**
     * Gets the country code to {@link Territory} {@link Map} JSON String.
     *
     * @return {@link String}
     */
    public String getCountryToTerritoryMapJson();

    /**
     * Gets the {@link Territory} for the given territory code.
     *
     * @param {@link
     *            String} Code of the territory
     * @return {@link Territory} returns null if no territory exists for the territory code
     */
    public Territory getTerritoryByTerritoryCode(String territoryCode);

    /**
     * Gets the territory code to {@link Territory} {@link Map}.
     *
     * @return {@link Map}
     */
    public Map<String, Territory> getTerritoryCodeToTerritoryMap();

    /**
     * Gets the territory code to {@link Territory} {@link Map} JSON String.
     *
     * @return {@link String}
     */
    public String getTerritoryCodeToTerritoryMapJson();

    /**
     * Gets the {@link Territory} for the given country code. If no territory is present for the given country, the
     * {@link Territory} for the given default territory code will be returned and if no territory exists for the
     * default territory code, null is returned.
     *
     * @param countryCode
     *            {@link String} Code of the country
     * @param defaultTerritoryCode
     *            {@link String} Code of the territory
     * @return {@link Territory}
     */
    public Territory getTerritoryByCountry(String countryCode, String defaultTerritoryCode);

    /**
     * Gets the {@link Territory} for the given territory code. If no territory is present for the given territory, the
     * {@link Territory} for the given default territory code will be returned and if no territory exists for the
     * default territory code, null is returned.
     *
     * @param territoryCode
     *            {@link String} Code of the territory
     * @param defaultTerritoryCode
     *            {@link String} Code of the territory
     * @return {@link Territory}
     */
    public Territory getTerritoryByTerritoryCode(String territoryCode, String defaultTerritoryCode);

    /**
     * Returns the default locale for the given country code.
     *
     * @param countryCode
     *            {@link String} Code of the country
     * @return {@link String}
     */
    public String getLocaleFromCountry(final String countryCode);

    /**
     * Returns the default locale code.
     *
     * @return {@link String}
     */
    public String getDefaultLocale();

    /**
     * Returns the default territory code.
     *
     * @return {@link Code}
     */
    public String getDefaultTerritoryCode();

    /**
     * Returns the {@ Territory} mapped to the given country code or the default territory if returnDefault is true,
     * null otherwise.
     *
     * @param countryCode
     *            {@link String} Code of the country
     * @param returnDefault
     *            {@link boolean}
     * @return
     */
    public Territory getTerritoryByCountryCode(final String countryCode, final boolean returnDefault);

    /**
     * Returns the list of Podcast widgets which needs to be displayed per territory and locale.
     *
     * @param countryCode
     * @param localeCode
     */
    public List<PodcastWidget> getPodcastListByTerritoryLocale(final String countryCode, final String localeCode);

    /**
     * Returns the selected country name by primary locale based on country code
     *
     * @param request
     * @param countryCode
     */
    public String getSelectedCountryNameByDefaultLocale(final SlingHttpServletRequest request,String countryCode);
}
