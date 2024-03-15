package com.pwc.madison.core.models.impl;

import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.granite.crypto.CryptoSupport;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.Language;
import com.pwc.madison.core.models.Territory;
import com.pwc.madison.core.models.TerritoryLanguageMapper;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.models.User;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegPagesPathProvidesService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.util.LocaleUtils;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Model(
    adaptables = {SlingHttpServletRequest.class},
    adapters = {TerritoryLanguageMapper.class})
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class TerritoryLanguageMapperImpl implements TerritoryLanguageMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerritoryLanguageMapper.class);
    public static final String INTERNAL_ONLY = "Internal Only";
    private Map<String, String> territoryLanguageToHomePageMap = new LinkedHashMap<>();
    private String pagePath;

    private String currentTerritoryLanguageCode;
    private static final String DEFAULT_URL = "#";
    
    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private SlingHttpServletResponse slingResponse;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private ResourceResolver resolver;

    @OSGiService
    private CountryTerritoryMapperService countryTerritoryMapperService;
    
    @OSGiService
    private UserRegPagesPathProvidesService userRegPagesPathProvidesService;
    
    @OSGiService
    private XSSAPI xssApi;

    @OSGiService
    private UserRegRestService userRegRestService;

    @OSGiService
    private CryptoSupport cryptoSupport;

    @OSGiService
    private UserPreferencesProviderService userPreferencesProviderService;

    @OSGiService
    private XSSAPI xssapi;

    @PostConstruct
    private void init() {
       
        String currentPageLanguageCode; 
        String currentPageTerritoryCode;
        pagePath = currentPage.getPath();
        if (pagePath.startsWith(userRegPagesPathProvidesService.getBaseUserregPath())) {
            currentPageLanguageCode = LocaleUtils.getLanguageFromLocale(request.getParameter(MadisonConstants.LOCALE_QUERY_PARAM));
            currentPageTerritoryCode = request.getParameter(MadisonConstants.TERRITORY_QUERY_PARAM);
        } else if (pagePath.matches(MadisonConstants.MADISON_PAGE_HIERARCHY)) {
            currentPageLanguageCode = LocaleUtils.getLanguageFromLocale(MadisonUtil.getLocaleForPath(pagePath));
            currentPageTerritoryCode = MadisonUtil.getTerritoryCodeForPath(pagePath);
        } else{
            currentPageLanguageCode = LocaleUtils.getLanguageFromLocale(countryTerritoryMapperService.getDefaultLocale());
            currentPageTerritoryCode = countryTerritoryMapperService.getDefaultTerritoryCode();
        }
        Territory currentPageTerritory = countryTerritoryMapperService
            .getTerritoryByTerritoryCode(currentPageTerritoryCode);

        if(StringUtils.isNotBlank(currentPageLanguageCode) && StringUtils.isNotBlank(currentPageTerritoryCode)){
            currentTerritoryLanguageCode = currentPageTerritoryCode.toUpperCase() + " \\ " + currentPageLanguageCode.toUpperCase();
        }
        LOGGER.debug("Current page language code: {}, Current page territory object: {}", xssApi.encodeForHTML(currentPageLanguageCode),
            currentPageTerritory);

        Map<String, Territory> territoryCodeToTerritoryMap = countryTerritoryMapperService.getTerritoryCodeToTerritoryMap();
        LOGGER.debug("Territory code to territory map: {}", territoryCodeToTerritoryMap);

        List<Language> languageList = new ArrayList<>();

        final User user = UserInformationUtil.getUser(request, false, userRegRestService, cryptoSupport,
                slingResponse, true, countryTerritoryMapperService, userPreferencesProviderService, false, false, xssapi);
        boolean isInternalUser = user.isUserLoggedIn() ? user.getUserProfile().getIsInternalUser() : Boolean.FALSE;

        Map<String, Territory> filteredTerritoryMap = territoryCodeToTerritoryMap;
        if(!isInternalUser) {
            filteredTerritoryMap = territoryCodeToTerritoryMap.entrySet().stream().filter(entry ->
                    !entry.getValue().getDesignation().equalsIgnoreCase(INTERNAL_ONLY)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        for (Map.Entry<String, Territory> territoryCodeToTerritoryEntry : filteredTerritoryMap.entrySet()) {
            Map<String, Language> territoryLanguages = territoryCodeToTerritoryEntry.getValue().getLocaleToLanguageMap();
            for (Language language : territoryLanguages.values()) {
    			languageList.add(language);
        	 }
        }

        Collections.sort(languageList);
        
        for(Language language : languageList) {
        	String homePage = MadisonUtil.getHomePagePathFromLocale(language.getLocale());
            homePage = Objects.nonNull(resolver.getResource(homePage)) ? homePage + MadisonConstants.HTML_EXTN : DEFAULT_URL;
            String refDataPath = getPageURLFromContentPath(language.getHomePagePath());
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
    }

    /**
     * Add {@value MadisonConstants#HTML_EXTN} if the path exists in the {@value MadisonConstants#SLASH_CONTENT} hierarchy
     * and doesn't have {@value MadisonConstants#HTML_EXTN}.
     * @param path {@link String}
     * @return {@code path} in case of external URL, {@value DEFAULT_URL} if the path doesn't exist or valid path with
     * {@value MadisonConstants#HTML_EXTN} extension.
     */
    private String getPageURLFromContentPath(String path){
        if(StringUtils.isBlank(path)){
            return null;
        }
        if(path.startsWith(MadisonConstants.SLASH_CONTENT) && !path.endsWith(MadisonConstants.HTML_EXTN)){
            path = Objects.nonNull(resolver.getResource(path)) ? path + MadisonConstants.HTML_EXTN : DEFAULT_URL;
        }
        return path;
    }

    @Override
    public Map<String, String> getTerritoryLanguageToHomePageMap() {
        return territoryLanguageToHomePageMap;
    }


    @Override
    public String getCurrentTerritoryLanguageCode() {
        return currentTerritoryLanguageCode;
    }

    @Override
    public String getComponentName() {
        return MadisonConstants.ANALYTICS_HEADER_COMPONENT_NAME;
    }
}
