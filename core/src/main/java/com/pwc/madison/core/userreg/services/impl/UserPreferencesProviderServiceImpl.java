package com.pwc.madison.core.userreg.services.impl;

import java.util.*;

import org.apache.commons.lang.StringUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.i18n.I18n;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.Preference;
import com.pwc.madison.core.userreg.enums.UserPreferencesType;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.impl.UserPreferencesProviderServiceImpl.MadisonPreferenceConfiguration;
import com.pwc.madison.core.util.MadisonUtil;

@Component(
        service = { UserPreferencesProviderService.class, EventHandler.class },
        immediate = true,
        property = { EventConstants.EVENT_TOPIC + "=org/apache/sling/api/resource/Resource/*",
                EventConstants.EVENT_FILTER + "=(path=/content/pwc-madison/global/user-preferences/*)" })
@Designate(ocd = MadisonPreferenceConfiguration.class)
public class UserPreferencesProviderServiceImpl implements UserPreferencesProviderService, EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserPreferencesProviderService.class);

    private final String EVENT_PATH = "path";
    
    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Reference
    private XSSAPI xssAPI;

    private Map<UserPreferencesType, String> userPreferencesBasePaths;
    private Map<UserPreferencesType, Map<String, Map<String, Preference>>> territoryToPreferencesMaps;
    private Map<UserPreferencesType, Map<String, Preference>> allPreferencesMaps;

    @Activate
    @Modified
    protected void Activate(final MadisonPreferenceConfiguration madisonPreferenceConfiguration) {
        LOGGER.info("UserPreferencesProviderService : Entered Activate/Modify");
        userPreferencesBasePaths = new HashMap<>();
        userPreferencesBasePaths.put(UserPreferencesType.GAAS,
                madisonPreferenceConfiguration.madison_gaas_preferences_base_path());
        userPreferencesBasePaths.put(UserPreferencesType.GAAP,
                madisonPreferenceConfiguration.madison_gaap_preferences_base_path());
        userPreferencesBasePaths.put(UserPreferencesType.INDUSTRY,
                madisonPreferenceConfiguration.madison_industry_preferences_base_path());
        userPreferencesBasePaths.put(UserPreferencesType.TOPIC,
                madisonPreferenceConfiguration.madison_topic_preferences_base_path());
        userPreferencesBasePaths.put(UserPreferencesType.TITLE_EXTERNAL,
                madisonPreferenceConfiguration.madison_external_title_preferences_base_path());
        userPreferencesBasePaths.put(UserPreferencesType.TITLE_INTERNAL,
                madisonPreferenceConfiguration.madison_internal_title_preferences_base_path());
        LOGGER.debug("UserPreferencesProviderService Activate() User Preferences Base Paths : {}",
                userPreferencesBasePaths);
        territoryToPreferencesMaps = new HashMap<>();
        allPreferencesMaps = new HashMap<>();
        createUserPreferencesMap(null);
    }

    @ObjectClassDefinition(name = "PwC Viewpoint User Preferences Configuration")
    public @interface MadisonPreferenceConfiguration {

        @AttributeDefinition(
                name = "UserReg User GAAP Preferences Base Path",
                description = "Content path under which user GAAP preferences's data is stored")
        String madison_gaap_preferences_base_path() default "/content/pwc-madison/global/user-preferences/gaap";

        @AttributeDefinition(
                name = "UserReg User GAAS Preferences Base Path",
                description = "Content path under which user GAAS preferences's data is stored")
        String madison_gaas_preferences_base_path() default "/content/pwc-madison/global/user-preferences/gaas";

        @AttributeDefinition(
                name = "UserReg User Industry Preferences Base Path",
                description = "Content path under which user Industry preferences's data is stored")
        String madison_industry_preferences_base_path() default "/content/pwc-madison/global/user-preferences/industry";

        @AttributeDefinition(
                name = "UserReg User Topic Preferences Base Path",
                description = "Content path under which user Topic preferences's data is stored")

        String madison_topic_preferences_base_path() default "/content/pwc-madison/global/user-preferences/topic";

        @AttributeDefinition(
                name = "UserReg External User Title Preferences Base Path",
                description = "Content path under which external user Title preferences's data is stored")
        String madison_external_title_preferences_base_path() default "/content/pwc-madison/global/user-preferences/title-external";

        @AttributeDefinition(
                name = "UserReg Internal User Title Preferences Base Path",
                description = "Content path under which internal user Title preferences's data is stored")
        String madison_internal_title_preferences_base_path() default "/content/pwc-madison/global/user-preferences/title-internal";

    }

    /**
     * Creates the Preferences {@link Map}.
     * 
     * @param changedResourcePath
     *            {@link String} resource path that is changed, the path defines which type of preference {@Map} is to
     *            be created. If null, all the preference type {@link Preference} Maps are created.
     */
    private void createUserPreferencesMap(final String changedResourcePath) {
        LOGGER.info("UserPreferencesProviderService : Entered createUserPreferencesMap");
        ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                MadisonConstants.MADISON_READ_SUB_SERVICE);
        if (null != resourceResolver) {
            for (Map.Entry<UserPreferencesType, String> userPreferencePathEntry : userPreferencesBasePaths.entrySet()) {
                if (null == changedResourcePath || changedResourcePath.startsWith(userPreferencePathEntry.getValue())) {
                    territoryToPreferencesMaps.put(userPreferencePathEntry.getKey(),
                            new HashMap<String, Map<String, Preference>>());
                    allPreferencesMaps.put(userPreferencePathEntry.getKey(), new HashMap<String, Preference>());
                    updateTerritoryToPreferenceMap(userPreferencePathEntry.getValue(), resourceResolver,
                            territoryToPreferencesMaps.get(userPreferencePathEntry.getKey()),
                            allPreferencesMaps.get(userPreferencePathEntry.getKey()));
                }
            }
            resourceResolver.close();
        }
        LOGGER.debug("UserPreferencesProviderService createUserPreferencesMap() : Territory To User Preferences Map {}",
                territoryToPreferencesMaps);
    };

    /**
     * Updates the given territoryToPreferencesMap and allPreferencesMap with the {@link Preference} at the given base
     * path.
     * 
     * @param path
     *            {@link String} the base path of the preferences.
     * @param resourceResolver
     *            {@link ResourceResolver}
     * @param territoryToPreferencesMap
     *            {@link Map} which is to be updated.
     * @param allPreferencesMap
     *            {@link Map} which is to be updated.
     */
    private void updateTerritoryToPreferenceMap(final String path, final ResourceResolver resourceResolver,
            Map<String, Map<String, Preference>> territoryToPreferencesMap, Map<String, Preference> allPreferencesMap) {
        LOGGER.debug(
                "UserPreferencesProviderService updateTerritoryToPreferenceMap() : Updating Preference Map for path {}",
                path);
        Resource preferenceBasePathResource = resourceResolver.getResource(path);
        if (null != preferenceBasePathResource) {
            for (Resource territoryResource : preferenceBasePathResource.getChildren()) {
                for (Resource preferenceResource : territoryResource.getChildren()) {
                    Preference preference = preferenceResource.adaptTo(Preference.class);
                    if (!territoryToPreferencesMap.containsKey(preference.getMappedTerritory())) {
                        territoryToPreferencesMap.put(preference.getMappedTerritory(),
                                new HashMap<String, Preference>());
                    }
                    territoryToPreferencesMap.get(preference.getMappedTerritory()).put(preference.getId(), preference);
                    allPreferencesMap.put(preference.getId(), preference);
                }
            }
        }
    }

    @Override
    public void handleEvent(Event event) {
        LOGGER.info("UserPreferencesProviderService : Entered handleEvent");
        createUserPreferencesMap((String) event.getProperty(EVENT_PATH));
    }

    /**
     * Sets the translated title for the given {@link Map} of {@link Preference} for given locale.
     * 
     * @param preferencesMap
     *            {@link Preference}
     * @param locale
     *            {@link String}
     * @param i18n
     *            {@link I18n}
     */
    private void setTranslatedTitle(final Map<String, Preference> preferencesMap, final String locale,
            final I18n i18n) {
        if (null != preferencesMap && null != i18n && null != locale) {
            for (Map.Entry<String, Preference> entry : preferencesMap.entrySet()) {
                setTranslatedTitle(entry.getValue(), locale, i18n);
            }
        }
    }

    /**
     * Sets the translated title for the given {@link Map} of {@link Map} of {@link Preference} for given locale.
     * 
     * @param preferencesMap
     *            {@link Preference}
     * @param locale
     *            {@link String}
     * @param i18n
     *            {@link I18n}
     */
    private void setTranslatedTitleForMap(Map<String, Map<String, Preference>> preferencesMap, final String locale,
            final I18n i18n) {
        if (null != preferencesMap && null != i18n && null != locale) {
            for (Map.Entry<String, Map<String, Preference>> entry : preferencesMap.entrySet()) {
                setTranslatedTitle(entry.getValue(), locale, i18n);
            }
        }
    }

    /**
     * Sets the translated title for the given {@link Preference} for given locale.
     * 
     * @param preference
     *            {@link Preference}
     * @param locale
     *            {@link String}
     * @param i18n
     *            {@link I18n}
     */
    private void setTranslatedTitle(final Preference preference, final String locale, final I18n i18n) {
        if (null != preference && null != i18n && null != locale) {
            String translatedTitle = i18n.get(preference.getI18nKey());
            translatedTitle = translatedTitle.equals(preference.getI18nKey()) ? preference.getTitle() : translatedTitle;
            preference.getTranslatedTitles().put(locale, translatedTitle);
        }
    }

    @Override
    public List<String> getTagsByPath(String path) {
        Preference preference = null;
        if (null != path) {
            for (Map.Entry<UserPreferencesType, String> userPreferencePathEntry : userPreferencesBasePaths.entrySet()) {
                if (path.startsWith(userPreferencePathEntry.getValue())) {
                    preference = allPreferencesMaps.get(userPreferencePathEntry.getKey()).get(path);
                }
            }
        }
        return (preference != null && preference.getTags() != null) ? Arrays.asList(preference.getTags())
                : new ArrayList<String>();
    }

    @Override
    public List<String> getTagsByPath(final List<String> paths) {
        List<String> tagList = new ArrayList<>();
        if (null != paths && paths.size() > 0) {
            for (Map.Entry<UserPreferencesType, String> userPreferencePathEntry : userPreferencesBasePaths.entrySet()) {
                if (paths.get(0).startsWith(userPreferencePathEntry.getValue())) {
                    tagList = getTags(paths, allPreferencesMaps.get(userPreferencePathEntry.getKey()));
                }
            }
        }
        final Set<String> tagSet = new HashSet<>(tagList);
        tagList.clear();
        tagList.addAll(tagSet);
        return tagList;
    }

    /**
     * Returns a list of all the tags for the given paths in the preferenceMap.
     * 
     * @param paths
     *            {@link List} of paths for which tags are to be fetched
     * @param preferenceMap
     *            {@link Map} preference map from which tags are to be fetched
     * @return
     */
    private List<String> getTags(final List<String> paths, final Map<String, Preference> preferenceMap) {
        final Set<String> tagSet = new HashSet<>();
        for (final String path : paths) {
            final Preference preference = preferenceMap.get(path);
            if (preference != null && preference.getTags() != null) {
                tagSet.addAll(Arrays.asList(preference.getTags()));
            }
        }
        return new ArrayList<>(tagSet);
    }

    @Override
    public Map<String, Map<String, Preference>> getTerritoryToPreferencesMap(
            final UserPreferencesType userPreferencesType, final String locale, final I18n i18n) {
        if (null != territoryToPreferencesMaps && null != territoryToPreferencesMaps.get(userPreferencesType)) {
            setTranslatedTitleForMap(territoryToPreferencesMaps.get(userPreferencesType), locale, i18n);
        }
        LOGGER.debug("UserPreferencesProviderService getTerritoryToPreferencesMap() : Returning Preference Map {}",
                territoryToPreferencesMaps.get(userPreferencesType));
        return territoryToPreferencesMaps.get(userPreferencesType);
    }

    @Override
    public Map<String, Preference> getAllPreferencesMap(final UserPreferencesType userPreferencesType,
            final String locale, final I18n i18n) {
        if (null != allPreferencesMaps && null != allPreferencesMaps.get(userPreferencesType)) {
            setTranslatedTitle(allPreferencesMaps.get(userPreferencesType), locale, i18n);
        }
        LOGGER.debug("UserPreferencesProviderService getAllPreferencesMap() : Returning Preference Map {}",
                allPreferencesMaps.get(userPreferencesType));
        return allPreferencesMaps.get(userPreferencesType);
    }

    @Override
    public Map<String, Preference> getPreferencesByTerritory(final UserPreferencesType userPreferencesType,
            final String territory, final String locale, final I18n i18n) {
        Map<String, Preference> preferenceMap = null;
        if (null != territory && null != territoryToPreferencesMaps
                && null != territoryToPreferencesMaps.get(userPreferencesType)) {
            preferenceMap = territoryToPreferencesMaps.get(userPreferencesType).get(territory.toLowerCase());
            setTranslatedTitle(preferenceMap, locale, i18n);
        }
        LOGGER.debug(
                "UserPreferencesProviderService getPreferencesByTerritory() : Returning Preference Map {} for given territory {}",
                preferenceMap, xssAPI.encodeForHTML(territory));
        return preferenceMap;
    }

    @Override
    public Preference getPreferenceByPath(final UserPreferencesType userPreferencesType, final String path,
            final String locale, final I18n i18n) {
        Preference preference = null;
        if (null != path && null != allPreferencesMaps && null != allPreferencesMaps.get(userPreferencesType)) {
            preference = allPreferencesMaps.get(userPreferencesType).get(path);
            setTranslatedTitle(preference, locale, i18n);
        }
        LOGGER.debug("UserPreferencesProviderService getPreferenceByPath() : Returning Preference {} for given path {}",
                preference, path);
        return preference;
    }

    @Override
    public String getTitleByPath(String path) {
        Preference preference = null;
            if (null != path) {
                for (Map.Entry<UserPreferencesType, String> userPreferencePathEntry : userPreferencesBasePaths.entrySet()) {
                    if (path.startsWith(userPreferencePathEntry.getValue())) {
                        preference = allPreferencesMaps.get(userPreferencePathEntry.getKey()).get(path);
                    }
                }
            }

        return (preference != null && StringUtils.isNotBlank(preference.getTitle())) ? preference.getTitle() : StringUtils.EMPTY;
    }

    @Override
    public List<String> getTitlesByPath(List<String> paths){
        List<String> titlesList = new ArrayList<>();
        if (null != paths && paths.size() > 0) {
            for (Map.Entry<UserPreferencesType, String> userPreferencePathEntry : userPreferencesBasePaths.entrySet()) {
                if (paths.get(0).startsWith(userPreferencePathEntry.getValue())) {
                    titlesList = getTitles(paths, allPreferencesMaps.get(userPreferencePathEntry.getKey()));
                }
            }
        }
        return titlesList;
    }

    /**
     * Returns a list of all the  titles for the given paths in the preferenceMap.
     *
     * @param paths
     *            {@link List} of paths for which tags are to be fetched
     * @param preferenceMap
     *            {@link Map} preference map from which tags are to be fetched
     * @return
     */
    private List<String> getTitles(final List<String> paths, final Map<String, Preference> preferenceMap) {
        final List<String> titleList = new ArrayList<>();
        for (final String path : paths) {
            final Preference preference = preferenceMap.get(path);
            if (preference != null && preference.getTitle() != null) {
                titleList.add(preference.getTitle());
            }
        }
        return titleList;
    }
}
