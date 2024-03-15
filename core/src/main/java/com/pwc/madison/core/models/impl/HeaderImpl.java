/*
 * Model class for populating the authorable header component fields.
 */
package com.pwc.madison.core.models.impl;

import java.util.*;

import javax.annotation.PostConstruct;

import com.adobe.granite.crypto.CryptoSupport;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.fusionSearch.enums.SearchFilterType;
import com.pwc.madison.core.fusionSearch.models.SearchFilter;
import com.pwc.madison.core.fusionSearch.services.MadisonSearchFilterProviderService;
import com.pwc.madison.core.models.*;
import com.pwc.madison.core.userreg.models.User;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.Page;
import com.google.gson.Gson;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.services.MadisonSearchConfigurationService;
import com.pwc.madison.core.userreg.enums.UserPreferencesType;
import com.pwc.madison.core.fusionSearch.models.SearchFilter.SortSearchFilterResultsByOrder;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegPagesPathProvidesService;
import com.pwc.madison.core.util.MadisonUtil;

@Model(
        adaptables = { SlingHttpServletRequest.class },
        adapters = { Header.class },
        resourceType = { HeaderImpl.RESOURCE_TYPE },
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)

@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class HeaderImpl implements Header {

    private static final Logger LOGGER = LoggerFactory.getLogger(Header.class);

    protected static final String RESOURCE_TYPE = "pwc-madison/components/structure/header";

    @Self
    private SlingHttpServletRequest request;

    private String logoURL;

    private String siteName;

    private String searchURL;

    private String helpLink;

    private String queryStr;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private ResourceResolver resolver;

    private Map<String, String> languages = new HashMap<>();

    private String currentLanguage;

    private String pagePath;

    private String pageLocale;

    private String internalTitleListJson;

    private String externalTitleListJson;
    
    private String industryListJson;

    private String pageTerritoryCode;

    private Resource headerResource;

    private String searchMaxSuggestions;

    private String minCharSuggestions;

    private String searchMaxSuggestedGuidance;
    
    private String searchMaxSuggestedGuidanceSlides;

    private String suggestedGuidanceTimeout;
    
    private String notificationMessage;
    
    private String localeDateFormat;

    private String fusionSearchFilterSortListJson;

    private String fusionSearchFilterFacetsListJson;

    @OSGiService
    private CountryTerritoryMapperService countryTerritoryMapperService;

    @OSGiService
    private UserPreferencesProviderService userPreferencesProviderService;

    @OSGiService
    private UserRegPagesPathProvidesService userRegPagesPathProvidesService;
    
    @OSGiService
    private XSSAPI xssapi;
    
    @OSGiService
    private MadisonSearchConfigurationService madisonSearchConfigurationService;

    @OSGiService
    private MadisonSearchFilterProviderService madisonSearchFilterProviderService;

    @OSGiService
    private UserRegRestService userRegRestService;

    @OSGiService
    private CryptoSupport cryptoSupport;

    @SlingObject
    private SlingHttpServletResponse slingResponse;

    @PostConstruct
    protected void init() {

        pagePath = request.getRequestURI();
        Resource pageResource = request.getResourceResolver().resolve(pagePath);
        pagePath = pageResource != null ? pageResource.getPath() : pagePath;
        if (pagePath.startsWith(userRegPagesPathProvidesService.getBaseUserregPath())) {
            pageLocale = request.getParameter(MadisonConstants.LOCALE_QUERY_PARAM);
            pageTerritoryCode = request.getParameter(MadisonConstants.TERRITORY_QUERY_PARAM);
        } else if (pagePath.matches(MadisonConstants.MADISON_PAGE_HIERARCHY)) {
            pageLocale = MadisonUtil.getLocaleForPath(pagePath);
            languages = getLanguagesFromPagePath(currentPage.getPath());
            pageTerritoryCode = MadisonUtil.getTerritoryCodeForPath(pagePath);
        } else{
            pageLocale = countryTerritoryMapperService.getDefaultLocale();
            pageTerritoryCode = countryTerritoryMapperService.getDefaultTerritoryCode();
        }
        this.localeDateFormat = countryTerritoryMapperService.getTerritoryByTerritoryCode(pageTerritoryCode).getDateFormat();
        final Territory territory = countryTerritoryMapperService.getTerritoryByTerritoryCode(pageTerritoryCode);
        if (territory != null) {
            helpLink = territory.getHelpLink();
        }
        createPreferencesListJson();
        createFusionSearchFilterListJson();
        pagePath += MadisonUtil.isLinkInternal(pagePath) ? MadisonConstants.HTML_EXTN : "";

        // populate header properties
        setHeaderResource();
        if (Objects.nonNull(headerResource)) {
            ValueMap headerValueMap = headerResource.getValueMap();
            logoURL = headerValueMap.get(LOGO_URL_PROPERTY_NAME, String.class);
            siteName = headerValueMap.get(SITE_NAME_PROPERTY_NAME, String.class);
            searchURL = headerValueMap.get(SEARCH_URL_PROPERTY_NAME, String.class);
            searchMaxSuggestions = headerValueMap.get(SEARCH_MAX_SUGGESTION_PROPERTY_NAME, String.class);
            minCharSuggestions = headerValueMap.get(SEARCH_MIN_CHAR_SUGGESTION, String.class);
            searchMaxSuggestedGuidance = headerValueMap.get(SEARCH_MAX_SEARCH_GUIDANCE, String.class);
            searchMaxSuggestedGuidanceSlides = headerValueMap.get(SEARCH_MAX_SEARCH_GUIDANCE_SLIDES, String.class);
            suggestedGuidanceTimeout = headerValueMap.get(SUGGESTED_GUIDANCE_TIMEOUT, String.class);
            notificationMessage = headerValueMap.get(NOTIFICATION_MESSAGE, String.class);
        }
        
        LOGGER.debug("Inside the init() of HeaderImpl");
    }

    private void setHeaderResource() {
        headerResource = MadisonUtil.getConfiguredResource(currentPage, MadisonConstants.HEADER_RELATIVE_PATH_FROM_PAGE, pageLocale, resolver, xssapi);
    }

    @Override
    public String getLogoURL() {
        return logoURL;
    }

    @Override
    public String getSiteName() {
        return siteName;
    }

    @Override
    public String getSearchURL() {
        return searchURL;
    }

    @Override
    public String getHelpLink() {
        return helpLink;
    }

    @Override
    public String getQueryStr() {
        queryStr = request.getParameter("q");
        return queryStr;
    }

    public Map<String, String> getLanguages() {
        return languages;
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * Returns the languages.
     *
     * @param pagePath
     *            {@link String}
     * @return {@link Map}
     */
    private Map<String, String> getLanguagesFromPagePath(final String pagePath) {
        String currentPageLanguageCode = currentPage.getLanguage().getLanguage();
        String currentPageTerritoryCode = MadisonUtil.getTerritoryCodeFromPagePath(pagePath);
        Territory currentPageTerritory = countryTerritoryMapperService
                .getTerritoryByTerritoryCode(currentPageTerritoryCode);
        LOGGER.debug("Current page language code: {}, Current page territory object: {}", currentPageLanguageCode,
                currentPageTerritory);

        if (Objects.nonNull(currentPageTerritory)) {
            final Map<String, Language> localeToLanguageMap = currentPageTerritory.getLocaleToLanguageMap();
            if (Objects.nonNull(localeToLanguageMap)) {
                for (Map.Entry<String, Language> entry : localeToLanguageMap.entrySet()) {
                    String otherLanguageCode = entry.getValue().getCode();
                    if (currentPageLanguageCode.equals(otherLanguageCode)) {
                        currentLanguage = entry.getValue().getLangTitle();
                    } else {
                        String path = pagePath.replace(MadisonConstants.FORWARD_SLASH + currentPageLanguageCode,
                                MadisonConstants.FORWARD_SLASH + otherLanguageCode);
                        Resource otherLanguagePageResource = resolver.getResource(path);
                        if (Objects.isNull(otherLanguagePageResource)) {
                            LOGGER.debug("Adding home page for {} language as {} does not exist", currentLanguage,
                                    path);
                            languages.put(entry.getValue().getLangTitle(),
                                    MadisonUtil.getHomePageFromLocale(entry.getValue().getLocale()));
                        } else {
                            languages.put(entry.getValue().getLangTitle(), path);
                        }
                    }
                }
            }
        }

        LOGGER.debug("Other languages map: {}", languages);

        return languages;
    }

    @Override
    public String getPageTerritoryCode() {
        return pageTerritoryCode;
    }

    @Override
    public String getPageLocale() {
        return pageLocale;
    }
    
    @Override
    public String getAudience() {
        return MadisonConstants.AUDIENCE;
    }

    @Override
    public String getCc() {
        return MadisonConstants.CC;
    }

    @Override
    public String getPage() {
        return MadisonConstants.PAGE;
    }

    /**
     * Create the json string for title preferences lists.
     */
    private void createPreferencesListJson() {
        final Locale locale = new Locale(pageLocale);
        final ResourceBundle resourceBundle = request.getResourceBundle(locale);
        final I18n i18n = new I18n(resourceBundle);
        final Map<String, Preference> internalTitlePreferencesMap = userPreferencesProviderService
                .getAllPreferencesMap(UserPreferencesType.TITLE_INTERNAL, pageLocale, i18n);
        List<Preference> internalTitleList = new ArrayList<Preference>(internalTitlePreferencesMap.values());
        internalTitleListJson = new Gson().toJson(internalTitleList);
        final Map<String, Preference> externalTitlePreferencesMap = userPreferencesProviderService
                .getAllPreferencesMap(UserPreferencesType.TITLE_EXTERNAL, pageLocale, i18n);
        List<Preference> externalTitleList = new ArrayList<Preference>(externalTitlePreferencesMap.values());
        externalTitleListJson = new Gson().toJson(externalTitleList);
        final User user = UserInformationUtil.getUser(request, false, userRegRestService, cryptoSupport,
                slingResponse, true, countryTerritoryMapperService, userPreferencesProviderService, false, false, xssapi);
        UserProfile userProfile = user.getUserProfile();
        String territoryCode = pageTerritoryCode;
        if (userProfile != null && (Objects.nonNull(userProfile.getTerritoryCode()) && StringUtils.isNotEmpty(userProfile.getTerritoryCode()))) {
            territoryCode = userProfile.getTerritoryCode();
        }
        String localeString = countryTerritoryMapperService.getLocaleFromCountry(territoryCode);
        Map<String, Preference> industryPreferencesMap = userPreferencesProviderService.getPreferencesByTerritory(UserPreferencesType.INDUSTRY, territoryCode , localeString, i18n);
        if(industryPreferencesMap == null) {
            industryPreferencesMap = userPreferencesProviderService.getAllPreferencesMap(UserPreferencesType.INDUSTRY, pageLocale, i18n);
        }
        List<Preference> industryList = new ArrayList<Preference>(industryPreferencesMap.values());
        industryListJson = new Gson().toJson(industryList);
    }

    /**
     * Create the json string for title search filter lists.
     */
    private void createFusionSearchFilterListJson(){
        final Locale locale = new Locale(pageLocale);
        final ResourceBundle resourceBundle = request.getResourceBundle(locale);
        final I18n i18n = new I18n(resourceBundle);
        Comparator<SearchFilter> searchFilterComparator = new SortSearchFilterResultsByOrder();
        final Map<String, SearchFilter> searchFilterSortMap = madisonSearchFilterProviderService
                .getAllSearchFilterMap(SearchFilterType.FILTER_SORT, pageLocale, i18n);
        List<SearchFilter>  searchFilterSortList= new ArrayList<SearchFilter>(searchFilterSortMap.values());
        Collections.sort(searchFilterSortList,searchFilterComparator);
        fusionSearchFilterSortListJson = new Gson().toJson(searchFilterSortList);
        final Map<String, Map<String,String>> searchFilterFacetsMap = madisonSearchFilterProviderService
                .getSearchFilterMapByLocale(SearchFilterType.FILTER_FACETS,i18n,MadisonUtil.getTerritoryCodeFromPagePath(pagePath));
        fusionSearchFilterFacetsListJson = new Gson().toJson(searchFilterFacetsMap);
    }

    public String getInternalTitleListJson() {
        return internalTitleListJson;
    }

    public String getExternalTitleListJson() {
        return externalTitleListJson;
    }

    public String getFusionSearchFilterSortListJson() {
        return fusionSearchFilterSortListJson;
    }

    public String getFusionSearchFilterFacetsListJson() {
        return fusionSearchFilterFacetsListJson;
    }

    public String getIndustryListJson() {
        return industryListJson;
    }

    @Override
    public String getSearchMaxSuggestions() {
        return searchMaxSuggestions;
    }

    @Override
    public String getMinCharSuggestions() {
        return minCharSuggestions;
    }


    @Override
    public String getPwcSourceValue(){ return DITAConstants.PWC_SOURCE_VALUE; }

    @Override
    public String getSearchMaxSuggestedGuidance() {
        return searchMaxSuggestedGuidance;
    }

    @Override
    public String getSuggestedGuidanceTimeout() {
        return suggestedGuidanceTimeout;
    }

    @Override
    public String getComponentName() {
        return MadisonConstants.ANALYTICS_HEADER_COMPONENT_NAME;
    }

    @Override
    public String getNotificationMessage() {
        return notificationMessage;
    }
    
    public String getLocaleDateFormat() {
        return localeDateFormat;
    }
    
    @Override
    public String getSearchMaxSuggestedGuidanceSlides() {
        return searchMaxSuggestedGuidanceSlides;
    }

}
