package com.pwc.madison.core.userreg.servlets;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.Language;
import com.pwc.madison.core.models.Territory;
import com.pwc.madison.core.userreg.models.User;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.day.cq.i18n.I18n;
import com.google.gson.Gson;
import com.pwc.madison.core.models.Preference;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.Constants;
import com.pwc.madison.core.userreg.enums.UserPreferencesType;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.userreg.models.request.EditContentPreferencesRequest;
import com.pwc.madison.core.userreg.models.request.EditTerritoryLanguagePreferencesRequest;
import com.pwc.madison.core.userreg.models.response.EditPreferencesLanguageTerritoryResponse;
import com.pwc.madison.core.userreg.models.response.GetUserResponse;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.userreg.utils.UserRegUtil;
import com.sun.jersey.api.client.ClientResponse;

/**
 * 
 * Servlet to handle User edit Territory and Language preferences request from Viewpoint.
 * 
 */
@Component(
        service = Servlet.class,
        name = "PwC Viewpoint Edit Territory Language Preferences Servlet",
        property = {
                org.osgi.framework.Constants.SERVICE_DESCRIPTION
                        + "=PwC Viewpoint Edit Territory Language Preferences Servlet",
                "sling.servlet.paths=/bin/userreg/preferences/territory-lang",
                "sling.servlet.methods=" + HttpConstants.METHOD_PUT })
public class EditTerritoryLanguagePreferences extends SlingAllMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditTerritoryLanguagePreferences.class);

    public static final String INTERNAL_ONLY = "Internal Only";
    private static final String DEFAULT_URL = "#";

    @Reference
    private transient UserRegRestService userregRestService;

    @Reference
    private transient ResourceResolverFactory resourceResolverFactory;

    @Reference
    private transient UserPreferencesProviderService userPreferencesProviderService;

    @Reference
    private transient CountryTerritoryMapperService countryTerritoryMapperService;

    @Reference
    private transient CryptoSupport cryptoSupport;

    @Reference
    private transient XSSAPI xssAPI;


    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
        try {

            Map<String, Territory> territoryCodeToTerritoryMap = countryTerritoryMapperService.getTerritoryCodeToTerritoryMap();
            LOGGER.debug("EditTerritoryLanguagePreferences :: Territory code to territory map: {}", territoryCodeToTerritoryMap);
            List<Language> languageList = new ArrayList<>();
            Map<String, String> territoryLanguageToHomePageMap = new LinkedHashMap<>();

            if (null != madisonCookie) {
                final User user = UserInformationUtil.getUser(request, false, userregRestService, cryptoSupport,
                        response, true, countryTerritoryMapperService, userPreferencesProviderService, false, false, xssAPI);

                boolean isExternalUser = Boolean.FALSE;
                if (user.isUserLoggedIn()) {
                    isExternalUser = !user.getUserProfile().getIsInternalUser();
                }

                Map<String, Territory> temporaryTerritoryCodeToTerritoryMap = territoryCodeToTerritoryMap;
                if (isExternalUser) {
                    territoryCodeToTerritoryMap = temporaryTerritoryCodeToTerritoryMap.entrySet().stream().filter(entry ->
                            !entry.getValue().getDesignation().equalsIgnoreCase(INTERNAL_ONLY)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                }
            }

            Map<String, Territory> temporaryTerritoryCodeToTerritoryMap = new HashMap<>();
            for (Map.Entry<String, Territory> territoryCodeToTerritoryEntry : territoryCodeToTerritoryMap.entrySet()) {
                Map<String, Language> territoryLanguages = territoryCodeToTerritoryEntry.getValue().getLocaleToLanguageMap();
                Map<String, Language> nonHiddenLanguageList = territoryLanguages.entrySet().stream().filter(e -> !e.getValue().getHideLanguage()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                if(nonHiddenLanguageList.size() >= 1) {
                    temporaryTerritoryCodeToTerritoryMap.put(territoryCodeToTerritoryEntry.getKey(), territoryCodeToTerritoryEntry.getValue());
                    for (Language language : territoryLanguages.values()) {
                        languageList.add(language);
                    }
                }
            }

            territoryCodeToTerritoryMap = temporaryTerritoryCodeToTerritoryMap;
            Collections.sort(languageList);

            for(Language language : languageList) {
                String homePage = MadisonUtil.getHomePagePathFromLocale(language.getLocale());
                ResourceResolver resolver = request.getResourceResolver();
                homePage = Objects.nonNull(resolver.getResource(homePage)) ? homePage + MadisonConstants.HTML_EXTN : DEFAULT_URL;
                String refDataPath = getPageURLFromContentPath(language.getHomePagePath(), resolver);
                homePage = StringUtils.isBlank(refDataPath) ? homePage : refDataPath;
                //don't show languages which have 'hideLanguage' property with value true on their node in refData.
                //'hideLanguage' default value is false
                if (language.getHideLanguage()) {
                    LOGGER.debug("{} is hidden", language);
                } else {
                    territoryLanguageToHomePageMap.put(language.getDisplayLanguageI18nKey(), homePage);
                }
            }
            LOGGER.debug("Territory language map: {}", territoryLanguageToHomePageMap);
            response.setContentType(Constants.CONTENT_TYPE_JSON);
            response.setCharacterEncoding(Constants.UTF_8_ENCODING);
            response.setStatus(SlingHttpServletResponse.SC_OK);
            response.getWriter().write(new Gson().toJson(territoryCodeToTerritoryMap));
        }catch (Exception e){
            LOGGER.error("Error while getting territory map", e);
        }
    }

    @Override
    protected void doPut(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
        if (null != madisonCookie) {
            final Object requestObject = UserRegUtil.getObjectFromRequest(request,
                    EditTerritoryLanguagePreferencesRequest.class);
            if (null != requestObject) {
                final EditTerritoryLanguagePreferencesRequest editTerritoryLanguagePreferencesRequest = (EditTerritoryLanguagePreferencesRequest) requestObject;
                final ClientResponse clientResponse = userregRestService.editTerritoryLanguagePreferences(
                        editTerritoryLanguagePreferencesRequest, madisonCookie.getValue());
                String responseString = clientResponse.getEntity(String.class);
                if (clientResponse.getStatus() == SlingHttpServletResponse.SC_OK) {
                    responseString = updateTerritoryLanguage(editTerritoryLanguagePreferencesRequest, responseString,
                            request, madisonCookie, response);
                } else if (clientResponse.getStatus() == SlingHttpServletResponse.SC_UNAUTHORIZED) {
                    UserRegUtil.removeUserRegMadisonCookie(request, response);
                }
                response.setContentType(Constants.CONTENT_TYPE_JSON);
                response.setCharacterEncoding(Constants.UTF_8_ENCODING);
                response.setStatus(clientResponse.getStatus());
                response.getWriter().write(responseString);

            } else {
                response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            response.sendError(SlingHttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    /**
     * Updates the territory and language preference list on AEM and perform other necessary tasks.
     * 
     * @param editTerritoryLanguagePreferencesRequest
     *            {@link EditTerritoryLanguagePreferencesRequest}
     * @param responseString
     *            {@link String}
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param madisonCookie
     *            {@link Cookie}
     * @param response
     *            {@link SlingHttpServletResponse}
     * @return {@link String}
     */
    private String updateTerritoryLanguage(
            final EditTerritoryLanguagePreferencesRequest editTerritoryLanguagePreferencesRequest,
            final String responseString, final SlingHttpServletRequest request, final Cookie madisonCookie,
            final SlingHttpServletResponse response) {
        EditPreferencesLanguageTerritoryResponse editPreferencesLanguageTerritoryResponse = new Gson()
                .fromJson(responseString, EditPreferencesLanguageTerritoryResponse.class);
        final Map<String, Preference> territoriesGaapMap = new TreeMap<String, Preference>();
        final Map<String, Preference> territoriesGaasMap = new TreeMap<String, Preference>();
        final Map<String, Preference> territoriesIndustryMap = new TreeMap<String, Preference>();
        final Map<String, Preference> territoriesTopicMap = new TreeMap<String, Preference>();
        String localeString = editTerritoryLanguagePreferencesRequest.getLocaleString();
        populatePreferenceMapByterritories(territoriesGaapMap, territoriesGaasMap,
                editPreferencesLanguageTerritoryResponse, localeString, request, territoriesIndustryMap,
                territoriesTopicMap);
        editPreferencesLanguageTerritoryResponse.getData()
                .setUserProfile(updateContentList(editPreferencesLanguageTerritoryResponse.getData().getUserProfile(),
                        territoriesGaasMap, territoriesGaapMap, madisonCookie, territoriesIndustryMap,
                        territoriesTopicMap));
        editPreferencesLanguageTerritoryResponse.setTerritoryToGaapPreferencesMap(territoriesGaapMap);
        editPreferencesLanguageTerritoryResponse.setTerritoryToGaasPreferencesMap(territoriesGaasMap);
        editPreferencesLanguageTerritoryResponse.setIndustryPreferencesMap(territoriesIndustryMap);
        editPreferencesLanguageTerritoryResponse.setTopicPreferencesMap(territoriesTopicMap);
        LOGGER.debug("EditTerritoryLanguagePreferences doPut() : Initaiting Profile creation/updation in AEM {}",
                xssAPI.encodeForHTML(editPreferencesLanguageTerritoryResponse.getData().getUserProfile().getEmail()));
        UserInformationUtil.updateMadisonUserProfileCookie(request, response,
                editPreferencesLanguageTerritoryResponse.getData().getUserProfile(), countryTerritoryMapperService,
                cryptoSupport, userPreferencesProviderService, false, xssAPI);
        return new Gson().toJson(editPreferencesLanguageTerritoryResponse);
    }

    /**
     * Updates the Content Preferences list in the given {@link UserProfile}.
     *
     * @param userProfile
     *            {@link UserProfile} current profile of user
     * @param gaasPreferencesMap
     *            {@link Map}
     * @param gaapPreferencesMap
     *            {@link Map}
     * @param madisonCookie
     *            {@link Cookie}
     * @return {@link UserProfile}
     */
    private UserProfile updateContentList(UserProfile userProfile, final Map<String, Preference> gaasPreferencesMap,
            final Map<String, Preference> gaapPreferencesMap, final Cookie madisonCookie,
            final Map<String, Preference> industryPreferencesMap, final Map<String, Preference> topicPreferencesMap) {
        final boolean isGaapPreferencesUpdated = updateList(userProfile.getPreferredGaap(), gaapPreferencesMap);
        final boolean isGaasPreferencesUpdated = updateList(userProfile.getPreferredGaas(), gaasPreferencesMap);
        final boolean isIndustryPreferencesUpdated = updateList(userProfile.getPreferredIndustry(),
                industryPreferencesMap);
        final boolean isTopicPreferencesUpdated = updateList(userProfile.getPreferredTopic(), topicPreferencesMap);
        if (isGaapPreferencesUpdated || isGaasPreferencesUpdated || isIndustryPreferencesUpdated
                || isTopicPreferencesUpdated) {
            final EditContentPreferencesRequest editContentPreferencesRequest = new EditContentPreferencesRequest(
                    userProfile.getPreferredGaas(), userProfile.getPreferredGaap(), userProfile.getPreferredTopic(),
                    userProfile.getPreferredIndustry());
            final ClientResponse clientResponse = userregRestService
                    .editContentPreferences(editContentPreferencesRequest, madisonCookie.getValue());
            final String responseString = clientResponse.getEntity(String.class);
            if (clientResponse.getStatus() == SlingHttpServletResponse.SC_OK) {
                userProfile = new Gson().fromJson(responseString, GetUserResponse.class).getData().getUserProfile();
                LOGGER.debug(
                        "EditTerritoryLanguagePreferences updateContentList() : Updated Content preferences after country/territory changes");
            }
        }
        return userProfile;
    }

    /**
     * Updates the preferenceList by removing Preferences that does not exist in given preferencesMap.
     * 
     * @param preferenceList
     *            {@link List} of Preferences IDs/Paths
     * @param preferencesMap
     *            {@link Map} where key is path/Id to the {@link Preference} and value is {@link Preference}
     * @return
     */
    private boolean updateList(final List<String> preferenceList, final Map<String, Preference> preferencesMap) {
        boolean isUpdated = false;
        ListIterator<String> preferenceIterator = preferenceList.listIterator();
        while (preferenceIterator.hasNext()) {
            if (!preferencesMap.containsKey(preferenceIterator.next())) {
                preferenceIterator.remove();
                isUpdated = true;
            }
        }
        return isUpdated;
    }

    /**
     * Populates the given {@link Map} territoriesGaapMap and {@link Map} territoriesGaasMap from Preferences mapped to
     * Preferred territories and primary territory.
     * 
     * @param territoriesGaapMap
     *            {@link Map} where key is path/Id to the GAAP {@link Preference} and value is GAAP {@link Preference}
     * @param territoriesGaasMap
     *            {@link Map} where key is path/Id to the GAAS {@link Preference} and value is GAAS {@link Preference}
     * @param getUserResponse
     *            {@link GetUserResponse}
     * @param localeString
     *            {@link string}
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param territoriesIndustryMap
     *            {@link Map} where key is path/Id to the Industry {@link Preference} and value is Industry
     *            {@link Preference}
     * @param territoriesTopicMap
     *            {@link Map} where key is path/Id to the Topic {@link Preference} and value is Topic {@link Preference}
     */
    private void populatePreferenceMapByterritories(final Map<String, Preference> territoriesGaapMap,
            final Map<String, Preference> territoriesGaasMap, final GetUserResponse getUserResponse,
            final String localeString, final SlingHttpServletRequest request,
            final Map<String, Preference> territoriesIndustryMap, final Map<String, Preference> territoriesTopicMap) {
        final Locale locale = new Locale(localeString);
        final ResourceBundle resourceBundle = request.getResourceBundle(locale);
        final I18n i18n = new I18n(resourceBundle);
        for (final String territory : getUserResponse.getData().getUserProfile().getPreferredTerritories()) {
            final Map<String, Preference> gaapPreferencesByTerritory = userPreferencesProviderService
                    .getPreferencesByTerritory(UserPreferencesType.GAAP, territory, localeString, i18n);
            if (null != gaapPreferencesByTerritory) {
                territoriesGaapMap.putAll(gaapPreferencesByTerritory);
            }
            final Map<String, Preference> gaasPreferencesByTerritory = userPreferencesProviderService
                    .getPreferencesByTerritory(UserPreferencesType.GAAS, territory, localeString, i18n);
            if (null != gaasPreferencesByTerritory) {
                territoriesGaasMap.putAll(gaasPreferencesByTerritory);
            }
        }
        final Map<String, Preference> gaapPreferencesByPrimaryTerritory = userPreferencesProviderService
                .getPreferencesByTerritory(UserPreferencesType.GAAP,
                        getUserResponse.getData().getUserProfile().getPrimaryTerritory(), localeString, i18n);
        if (null != gaapPreferencesByPrimaryTerritory) {
            territoriesGaapMap.putAll(gaapPreferencesByPrimaryTerritory);
        }
        final Map<String, Preference> gaasPreferencesByPrimaryTerritory = userPreferencesProviderService
                .getPreferencesByTerritory(UserPreferencesType.GAAS,
                        getUserResponse.getData().getUserProfile().getPrimaryTerritory(), localeString, i18n);
        if (null != gaasPreferencesByPrimaryTerritory) {
            territoriesGaasMap.putAll(gaasPreferencesByPrimaryTerritory);
        }
        final Map<String, Preference> gaapPreferencesFirmWide = userPreferencesProviderService
                .getPreferencesByTerritory(UserPreferencesType.GAAP, Constants.FIRM_WIDE_CODE, localeString, i18n);
        if (null != gaapPreferencesFirmWide) {
            territoriesGaapMap.putAll(gaapPreferencesFirmWide);
        }
        final Map<String, Preference> gaasPreferencesFirmWide = userPreferencesProviderService
                .getPreferencesByTerritory(UserPreferencesType.GAAS, Constants.FIRM_WIDE_CODE, localeString, i18n);
        if (null != gaasPreferencesFirmWide) {
            territoriesGaasMap.putAll(gaasPreferencesFirmWide);
        }
        final Map<String, Preference> industryPreferences = userPreferencesProviderService
                .getAllPreferencesMap(UserPreferencesType.INDUSTRY, localeString, i18n);
        if (null != industryPreferences) {
            territoriesIndustryMap.putAll(industryPreferences);
        }
        final Map<String, Preference> topicPreferences = userPreferencesProviderService
                .getAllPreferencesMap(UserPreferencesType.TOPIC, localeString, i18n);
        if (null != topicPreferences) {
            territoriesTopicMap.putAll(topicPreferences);
        }
    }

    /**
     * Add {@value MadisonConstants#HTML_EXTN} if the path exists in the {@value MadisonConstants#SLASH_CONTENT} hierarchy
     * and doesn't have {@value MadisonConstants#HTML_EXTN}.
     * @param path {@link String}
     * @return {@code path} in case of external URL, {@value DEFAULT_URL} if the path doesn't exist or valid path with
     * {@value MadisonConstants#HTML_EXTN} extension.
     */
    private String getPageURLFromContentPath(String path, ResourceResolver resolver){
        if(StringUtils.isBlank(path)){
            return null;
        }
        if(path.startsWith(MadisonConstants.SLASH_CONTENT) && !path.endsWith(MadisonConstants.HTML_EXTN)){
            path = Objects.nonNull(resolver.getResource(path)) ? path + MadisonConstants.HTML_EXTN : DEFAULT_URL;
        }
        return path;
    }
}
