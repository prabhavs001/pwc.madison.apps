package com.pwc.madison.core.userreg.services;

import java.util.List;
import java.util.Map;

import com.day.cq.i18n.I18n;
import com.pwc.madison.core.models.Preference;
import com.pwc.madison.core.models.Territory;
import com.pwc.madison.core.userreg.enums.UserPreferencesType;

/**
 * The service provides the User Preferences.
 *
 */
public interface UserPreferencesProviderService {

    /**
     * Returns {@link Map} that contains {@link String} territory code as key and {@link Map} as value. The inner
     * {@link Map} contains key as {@link String} ID/Path of the {@link UserPreferencesType} Preferences and value as
     * {@link Preference}.
     * 
     * @param userPreferencesType
     *            {@link UserPreferencesType} defines the type of preference
     * @param locale
     *            {@link String} required in case the localized title for {@link Preference} is required
     * @param i18n
     *            {@link I18n} required in case the localized title for {@link Preference} is required. If any of the
     *            localeString or I18n is null, the localized title is not computed
     * @return {@link Map} of {@link UserPreferencesType} Preferences
     */
    public Map<String, Map<String, Preference>> getTerritoryToPreferencesMap(
            final UserPreferencesType userPreferencesType, final String locale, final I18n i18n);

    /**
     * Returns {@link Map} contains key as {@link String} ID/Path of the {@link UserPreferencesType} Preferences and
     * value as {@link Preference}.
     * 
     * @param userPreferencesType
     *            {@link UserPreferencesType} defines the type of preference
     * @param locale
     *            {@link String} required in case the localized title for {@link Preference} is required
     * @param i18n
     *            {@link I18n} required in case the localized title for {@link Preference} is required. If any of the
     *            localeString or I18n is null, the localized title is not computed
     * @return {@link Map} of {@link UserPreferencesType} Preferences
     */
    public Map<String, Preference> getAllPreferencesMap(final UserPreferencesType userPreferencesType,
            final String locale, final I18n i18n);

    /**
     * Returns {@link Map} contains key as {@link String} ID/Path of the {@link UserPreferencesType} Preferences and
     * value as {@link Preference}. The {@link Map} contains all the {@link UserPreferencesType} {@link Preference}
     * mapped to given territory.
     * 
     * @param userPreferencesType
     *            {@link UserPreferencesType} defines the type of preference
     * @param territory
     *            {@link Territory} for which the {@link Map<String, Preference>} is to be returned
     * @param locale
     *            {@link String} required in case the localized title for {@link Preference} is required
     * @param i18n
     *            {@link I18n} required in case the localized title for {@link Preference} is required. If any of the
     *            localeString or I18n is null, the localized title is not computed
     * @return {@link Map} of {@link UserPreferencesType} Preferences mapped to given territory
     */
    public Map<String, Preference> getPreferencesByTerritory(final UserPreferencesType userPreferencesType,
            final String territory, final String locale, final I18n i18n);

    /**
     * Returns {@link Preference} of the given path/ID of the {@link UserPreferencesType} Preference.
     * 
     * @param userPreferencesType
     *            {@link UserPreferencesType} defines the type of preference
     * @param path
     *            {@link String} the path/ID to the preference
     * @param locale
     *            {@link String} required in case the localized title for {@link Preference} is required
     * @param i18n
     *            {@link I18n} required in case the localized title for {@link Preference} is required. If any of the
     *            localeString or I18n is null, the localized title is not computed
     * @return {@link Preference}
     */
    public Preference getPreferenceByPath(final UserPreferencesType userPreferencesType, final String path,
            final String locale, final I18n i18n);

    /**
     * Returns {@link List} of tags for the preference at the given path.
     * 
     * @param path
     *            {@link String} the path to the preference
     * @return {@link List}
     */
    public List<String> getTagsByPath(final String paths);

    /**
     * Returns {@link List} of tags for the preferences at the given path.
     * 
     * @param paths
     *            {@link List} the list of paths of preferences
     * @return
     */
    public List<String> getTagsByPath(final List<String> paths);

    /**
     * Returns {@link String} of title for the preferences at the given path.
     *
     * @param path
     *         {@link String} the path of preference
     * @return
     */
     public String getTitleByPath(String path);

    /**
     * Returns {@link List} of titles for the preferences at the given path.
     *
     * @param paths
     *            {@link List} the list of paths of preferences
     * @return
     */
     public List<String> getTitlesByPath(List<String> paths);

}
