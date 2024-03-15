package com.pwc.madison.core.models.impl;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.xss.XSSAPI;

import com.adobe.granite.crypto.CryptoSupport;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.StructureComponentCaching;
import com.pwc.madison.core.services.CachingConfigurationProviderService;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.models.User;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegPagesPathProvidesService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.util.MadisonUtil;

@Model(
        adaptables = { SlingHttpServletRequest.class },
        adapters = { StructureComponentCaching.class },
        resourceType = { StructureComponentCachingImpl.GLOBAL_NAVIGATION_RESOURCE_TYPE,
                StructureComponentCachingImpl.HEADER_RESOURCE_TYPE,
                StructureComponentCachingImpl.FOOTER_RESOURCE_TYPE },
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class StructureComponentCachingImpl implements StructureComponentCaching {

    protected static final String GLOBAL_NAVIGATION_RESOURCE_TYPE = "pwc-madison/components/structure/global-navigation";
    protected static final String FOOTER_RESOURCE_TYPE = "pwc-madison/components/structure/header/v1/header";
    protected static final String HEADER_RESOURCE_TYPE = "pwc-madison/components/structure/footer/v1/footer";

    @Self
    private SlingHttpServletRequest slingRequest;

    @SlingObject
    private SlingHttpServletResponse slingResponse;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private ResourceResolver resolver;

    @OSGiService
    private UserRegRestService userRegRestService;

    @OSGiService
    private CryptoSupport cryptoSupport;

    @OSGiService
    private CountryTerritoryMapperService countryTerritoryMapperService;

    @OSGiService
    private UserPreferencesProviderService userPreferencesProviderService;

    @OSGiService
    private CachingConfigurationProviderService cachingConfigurationProviderService;

    @OSGiService
    private UserRegPagesPathProvidesService userRegPagesPathProvidesService;

    @OSGiService
    private XSSAPI xssApi;

    @Override
    public boolean isCachingEnabled() {
        return cachingConfigurationProviderService.isCachingEnabled();
    }

    @Override
    public boolean isInternalUser() {
        final User user = UserInformationUtil.getUser(slingRequest, false, userRegRestService, cryptoSupport,
                slingResponse, true, countryTerritoryMapperService, userPreferencesProviderService, false,
                false, xssApi);
        final boolean isInternalUser = user.isUserLoggedIn() ? user.getUserProfile().getIsInternalUser()
                : Boolean.FALSE;
        return isInternalUser;
    }

    /**
     * Returns the configured component resource path for the given component relative path. It takes care of the
     * inheritance.
     * 
     * @param ComponentRelativePath
     *            {@link String}
     * @return {@link String}
     */
    private String getComponentResourcePath(final String ComponentRelativePath) {
        final String pageLocale = MadisonUtil.getLocaleOfCurrentPage(userRegPagesPathProvidesService,
                countryTerritoryMapperService, slingRequest);
        final Resource componentResource = MadisonUtil.getConfiguredResource(currentPage, ComponentRelativePath,
                pageLocale, resolver, xssApi);
        final PageManager pageManager = resolver.adaptTo(PageManager.class);
        return componentResource == null ? currentPage.getPath()
                : pageManager.getContainingPage(componentResource).getPath();
    }

    @Override
    public String getGlobalNavigationResourcePath() {
        return getComponentResourcePath(MadisonConstants.GLOBAL_NAVIGATION_RELATIVE_PATH_FROM_PAGE);
    }

    @Override
    public String getFooterResourcePath() {
        return getComponentResourcePath(MadisonConstants.FOOTER_RELATIVE_PATH_FROM_PAGE);
    }

    @Override
    public String getHeaderResourcePath() {
        return getComponentResourcePath(MadisonConstants.HEADER_RELATIVE_PATH_FROM_PAGE);
    }

    @Override
    public String getHeaderHtmlPath() {
        return cachingConfigurationProviderService.getHeaderHtmlPath();
    }

    @Override
    public String getTerritoryMapperResourcePath() {
        return getComponentResourcePath(MadisonConstants.TERRITORY_MAPPING_RELATIVE_PATH_FROM_PAGE);
    }

}
