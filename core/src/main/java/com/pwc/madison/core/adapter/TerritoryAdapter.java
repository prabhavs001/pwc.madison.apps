package com.pwc.madison.core.adapter;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.Language;
import com.pwc.madison.core.models.Territory;
import com.pwc.madison.core.util.LocaleUtils;

/**
 * Adapter to map the properties of a {@link Resource} to a {@link Territory}.
 */
public class TerritoryAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerritoryAdapter.class);

    public static final String TERRITORY_NAME_PROPERTY = "territoryName";
    public static final String COUNTRY_CODES_PROPERTY = "mappedCountry";
    public static final String DEFAULT_LOCALE_PROPERTY = "defaultLocale";
    public static final String TERRITORY_I18NKEY_PROPERTY = "territoryi18nKey";
    public static final String HELP_LINK = "helpLink";
    public static final String TERMS_AND_CONDITIONS_VERSION = "termsAndConditionsVersion";
    public static final String DATE_FORMAT = "dateFormat";

    /**
     * Returns a new {@link Territory} object after mapping the required properties of a Resource.
     *
     * @param territoryResource
     *            {@link Resource} The properties of this resource will be added to the Territory
     * @return {@link Territory}
     */
    public Territory adaptResourceToTerritory(final Resource territoryResource) {
        Territory territory = null;
        if (null != territoryResource) {
            final ValueMap territoryValueMap = territoryResource.getValueMap();
            territory = new Territory();
            territory.setCountries(territoryValueMap.get(COUNTRY_CODES_PROPERTY, String[].class));
            territory.setTerritoryCode(territoryResource.getName());
            territory.setDefaultLocale(territoryValueMap.get(DEFAULT_LOCALE_PROPERTY, String.class));
            territory.setTerritoryName(territoryValueMap.get(TERRITORY_NAME_PROPERTY, territoryResource.getName()));
            territory.setTerritoryI18nKey(territoryValueMap.get(TERRITORY_I18NKEY_PROPERTY, String.class));
            territory.setLocaleToLanguageMap(getLocaleToLanguageMapFromTerritoryResource(territoryResource));
            territory.setHelpLink(territoryValueMap.get(HELP_LINK, String.class));
            territory.setCookieEnabled(territoryValueMap.get(MadisonConstants.COOKIE_ENABLED, String.class));
            territory.setTwitterShare(territoryValueMap.get(MadisonConstants.PN_TWITTER_SHARE, String.class));
            territory.setLinkedinShare(territoryValueMap.get(MadisonConstants.PN_LINKEDIN_SHARE, String.class));
            territory.setTermsAndConditionsVersion(territoryValueMap.get(TERMS_AND_CONDITIONS_VERSION, String.class));
            territory.setDateFormat(
                    territoryValueMap.containsKey(DATE_FORMAT) ? territoryValueMap.get(DATE_FORMAT, String.class)
                            : StringUtils.EMPTY);
            territory.setDesignation(territoryValueMap.getOrDefault(MadisonConstants.PN_DESIGNATION, StringUtils.EMPTY).toString());
            territory.setContentTypeSortingEnabled(territoryValueMap.get(MadisonConstants.CONTENT_TYPE_SORTING_ENABLED, Boolean.class));
            LOGGER.debug("TerritoryAdapter adaptResourceToTerritory() : Adapting resource at path {} to Territory: {}",
                    territoryResource.getPath(), territory.toString());
        }
        return territory;
    }

    /**
     * Gets the locale to {@link Language} {@link Map} from territory {@link Resource}.
     *
     * @param territoryResource
     *            {@link Resource} of the territory resource
     * @return {@link Map}
     */
    private Map<String, Language> getLocaleToLanguageMapFromTerritoryResource(final Resource territoryResource) {
        final Map<String, Language> localeToLangMap = new HashMap<>();
        for (final Resource languageResource : territoryResource.getChildren()) {
            final String languageCode = languageResource.getName();
            localeToLangMap.put(LocaleUtils.getLocale(territoryResource.getName(), languageCode),
                    languageResource.adaptTo(Language.class));
        }
        return localeToLangMap;
    }

}
