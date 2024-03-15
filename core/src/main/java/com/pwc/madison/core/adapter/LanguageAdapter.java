package com.pwc.madison.core.adapter;

import com.pwc.madison.core.models.Language;
import com.pwc.madison.core.util.LocaleUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter to map the properties of a {@link Resource} to a {@link Language}.
 */
public class LanguageAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageAdapter.class);
    private static final String PROP_LANGUAGE_TITLE = "languageTitle";
    private static final String SEARCH_AND_PROMOTE_ID = "searchAndPromoteID";
    private static final String DISPLAY_LANGUAGE = "displayLanguage";
    private static final String DISPLAY_LANGUAGE_I18N_KEY = "displayLanguageI18nKey";
    private static final String DISPLAY_ORDER = "displayOrder";
    private static final String HOME_PAGE_PATH_PN = "homePagePath";
    private static final String HIDE_LANGUAGE = "hideLanguage";

    /**
     * Returns a new {@link Language} object after mapping the required properties of a Resource.
     *
     * @param languageResource
     *            {@link Resource} The properties of this resource will be added to the Language
     * @return {@link Language}
     */
    public Language adaptResourceToLanguage(final Resource languageResource) {
        Language language = null;
        if (null != languageResource) {
            final ValueMap properties = languageResource.getValueMap();
            final String code = languageResource.getName();
            final String locale = getLocaleForLanguageResource(languageResource);
            final String displayLanguage = properties.get(DISPLAY_LANGUAGE,String.class);
            final String displayLanguageI18nKey = properties.get(DISPLAY_LANGUAGE_I18N_KEY,String.class);
            final String languageTitle = properties.get(PROP_LANGUAGE_TITLE, code);
            final Integer displayOrder = properties.get(DISPLAY_ORDER, Integer.class);
            final String homePagePath = properties.get(HOME_PAGE_PATH_PN, String.class);
            final Boolean hideLanguage = properties.get(HIDE_LANGUAGE, false);
            /* SearchAndPromoteID is different for different language, so this property is kept under language */
            String searchAndPromoteID = properties.get(SEARCH_AND_PROMOTE_ID, "");
            language = new Language(languageTitle, code, locale, searchAndPromoteID,displayLanguage,displayLanguageI18nKey,
                displayOrder, homePagePath, hideLanguage);
            LOGGER.debug("LanguageAdapter adaptResourceToLanguage() : Adapting resource at path {} to Language: {}",
                    languageResource.getPath(), language.toString());
        }
        return language;
    }

    /**
     * Returns Locale by appending the names of provided Language resource and its parent Territory resource.
     * 
     * @param languageResource
     *            {@link String}
     * @return {@link String}
     */
    private String getLocaleForLanguageResource(Resource languageResource) {
        String languageCode = languageResource.getName();
        String territoryCode = languageResource.getParent().getName();
        return LocaleUtils.getLocale(territoryCode, languageCode);
    }

}
