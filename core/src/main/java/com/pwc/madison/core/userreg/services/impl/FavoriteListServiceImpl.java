package com.pwc.madison.core.userreg.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.services.FavoriteListService;
import com.pwc.madison.core.userreg.services.UserRegPagesPathProvidesService;
import com.pwc.madison.core.userreg.services.impl.FavoriteListServiceImpl.FavoriteListConfiguration;
import com.pwc.madison.core.util.LocaleUtils;
import com.pwc.madison.core.util.MadisonUtil;

@Component(service = FavoriteListService.class, immediate = true)
@Designate(ocd = FavoriteListConfiguration.class)
public class FavoriteListServiceImpl implements FavoriteListService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FavoriteListService.class);

    private static final String TERRITORY_PLACEHOLDER = "<territory>";
    private static final String LANGUAGE_PLACEHOLDER = "<language>";

    @Reference
    private UserRegPagesPathProvidesService userRegPagesPathProvidesService;

    @Reference
    private CountryTerritoryMapperService countryTerritoryMapperService;

    private boolean isFavoriteListEnabled;
    private int favoriteListLimit;
    private int favoritePanelListLimit;
    private int favoriteListLimitPerPage;
    private int favoriteListSessionStotageCacheTime;
    private String favoriteListPageUrl;

    @Activate
    @Modified
    protected void Activate(final FavoriteListConfiguration favoriteListConfiguration) {
        isFavoriteListEnabled = favoriteListConfiguration.favorite_list_enable();
        favoriteListLimit = favoriteListConfiguration.favorite_list_limit();
        favoritePanelListLimit = favoriteListConfiguration.favorite_list_panel_limit();
        favoriteListLimitPerPage = favoriteListConfiguration.favorite_list_page_limit();
        favoriteListSessionStotageCacheTime = favoriteListConfiguration.favorite_list_cache_time();
        favoriteListPageUrl = favoriteListConfiguration.favorite_list_page_url();

        LOGGER.debug("FavoriteListService Activate() Is Favorite List Functionality Enabled : {}",
                isFavoriteListEnabled);
        LOGGER.debug("FavoriteListService Activate() Favorite List Limit : {}", favoriteListLimit);
        LOGGER.debug("FavoriteListService Activate() Favorite List Panel Limit : {}", favoritePanelListLimit);
        LOGGER.debug("FavoriteListService Activate() Favorite List Limit Per Page : {}", favoriteListLimitPerPage);
        LOGGER.debug("FavoriteListService Activate() Favorite List Cahe Time : {}",
                favoriteListSessionStotageCacheTime);
        LOGGER.debug("FavoriteListService Activate() Favorite List Page URL Pattern : {}", favoriteListPageUrl);
    }

    @ObjectClassDefinition(name = "PwC Viewpoint Core Favorite List Configuration")
    public @interface FavoriteListConfiguration {

        @AttributeDefinition(name = "Enable Favorite List", description = "Enable favorite list functionality")
        boolean favorite_list_enable() default true;

        @AttributeDefinition(
                name = "Favorite List Limit",
                description = "Number of pages that user can add to its favorite list")
        int favorite_list_limit() default 25;

        @AttributeDefinition(
                name = "Favorite Panel List Limit",
                description = "Number of favorite items in default list that can be shown to user in the favorite list panel")
        int favorite_list_panel_limit() default 10;

        @AttributeDefinition(
                name = "Favorite Limit List Per Page",
                description = "Number of favorite items in default list that can be shown to user in the favorite list panel")
        int favorite_list_page_limit() default 10;

        @AttributeDefinition(
                name = "Favorite List Cache Time",
                description = "Favorite List local storage cache time in minutes, The list will be fetched/refreshed if cache is configured number of minutes old that current time.")
        int favorite_list_cache_time() default 10;

        @AttributeDefinition(
                name = "Favorite List Page URL Pattern",
                description = "Favorite List page URL with placeholders <territory> and <language>.")
        String favorite_list_page_url();

    }

    @Override
    public boolean isFavoriteListEnabled() {
        return isFavoriteListEnabled;
    }

    @Override
    public int getFavoriteListLimit() {
        return favoriteListLimit;
    }

    @Override
    public int getFavoritePanelListLimit() {
        return favoritePanelListLimit;
    }

    @Override
    public int getFavoriteListLimitPerPage() {
        return favoriteListLimitPerPage;
    }

    @Override
    public int getLocalStorageCacheTimeInMinutes() {
        return favoriteListSessionStotageCacheTime;
    }

    @Override
    public String getFavoriteListPage(final SlingHttpServletRequest request, final String pagePath) {
        String territory = StringUtils.EMPTY;
        String language = StringUtils.EMPTY;
        String locale = StringUtils.EMPTY;
        if (pagePath.startsWith(userRegPagesPathProvidesService.getBaseUserregPath())) {
            locale = request.getParameter(MadisonConstants.LOCALE_QUERY_PARAM);
            language = LocaleUtils.getLanguageFromLocale(locale);
            territory = LocaleUtils.getTerritoryFromLocale(locale);
        } else if (pagePath.matches(MadisonConstants.MADISON_PAGE_HIERARCHY)) {
            territory = MadisonUtil.getTerritoryCodeForPath(pagePath);
            language = MadisonUtil.getLanguageCodeForPath(pagePath);
        } else {
            locale = countryTerritoryMapperService.getDefaultLocale();
            language = LocaleUtils.getLanguageFromLocale(locale);
            territory = LocaleUtils.getTerritoryFromLocale(locale);
        }
        return favoriteListPageUrl.replace(TERRITORY_PLACEHOLDER, territory).replace(LANGUAGE_PLACEHOLDER, language);
    }

}
