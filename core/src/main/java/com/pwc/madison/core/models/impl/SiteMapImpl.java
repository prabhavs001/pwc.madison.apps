/*
 * Model class for populating the authorable footer component fields.
 */
package com.pwc.madison.core.models.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.xss.XSSAPI;

import com.adobe.granite.crypto.CryptoSupport;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.SiteMap;
import com.pwc.madison.core.models.SiteMapGroup;
import com.pwc.madison.core.models.SiteMapLink;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.models.User;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.util.MadisonUtil;

@Model(
        adaptables = { SlingHttpServletRequest.class },
        adapters = { SiteMap.class },
        resourceType = { SiteMapImpl.RESOURCE_TYPE },
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class SiteMapImpl implements SiteMap {

    protected static final String RESOURCE_TYPE = "pwc-madison/components/structure/sitemap";
    
    private static final String LINKS_NODE = "links";
    
    @ValueMapValue(name = "sitemapLabel")
    private String sitemapLabel;
    
    @ChildResource(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Resource groups;
    
    @Self
    private SlingHttpServletRequest slingRequest;
    
    @OSGiService
    private UserRegRestService userRegRestService;
    
    @SlingObject
    private SlingHttpServletResponse slingResponse;
    
    @OSGiService
    private CryptoSupport cryptoSupport;
    
    @OSGiService
    private CountryTerritoryMapperService countryTerritoryMapperService;
    
    @OSGiService
    private UserPreferencesProviderService userPreferencesProviderService;
    
    @OSGiService
    private transient XSSAPI xssapi;

    private List<List<SiteMapGroup>> siteMapItems = new ArrayList<>();
        
    private Boolean isInternalUser;
    
    private Boolean isAuthorMode;
    
    private String pageLocale;
    
    @PostConstruct
    protected void init() {
        final User user = UserInformationUtil.getUser(slingRequest, false, userRegRestService, cryptoSupport, slingResponse, true, countryTerritoryMapperService, userPreferencesProviderService, false, false, xssapi);
        final UserProfile userProfile = user.getUserProfile();
        isInternalUser = user.isUserLoggedIn() ? userProfile.getIsInternalUser(): Boolean.FALSE;
        isAuthorMode = !MadisonUtil.isPublishMode(slingRequest);
        pageLocale = MadisonUtil.getLocaleForPath(slingRequest.getRequestURI());
        generateSiteMap();
        siteMapItems.removeIf(p -> p.isEmpty());
    }

    private void generateSiteMap() {
        if (groups != null) {
            for (int i = 0; i < 6; i++) {
                List<SiteMapGroup> columnsData = new ArrayList<>();
                siteMapItems.add(i, columnsData);
            }
            for (Resource group : groups.getChildren()) {
                generateSiteMapGroups(group);
            }
        }
    }

    private void generateSiteMapGroups(Resource value) {

        
        SiteMapGroup group = Objects.requireNonNull(value.adaptTo(SiteMapGroup.class));
        Boolean hideGroup = false;
        String useColumn = group.getUseColumn();
        String stackOn = group.getStackOn();
        String groupPath = group.getGroupPath();
        if (StringUtils.isNotBlank(groupPath) && groupPath.startsWith(MadisonConstants.SLASH_CONTENT)) {
            String processedlinkPath = MadisonUtil.getUrlForPageResource(groupPath);
            group.setGroupPath(processedlinkPath);
            String groupLocale = MadisonUtil.getLocaleForPath(groupPath);
            if(groupLocale == null || !groupLocale.equals(pageLocale)) {
                hideGroup = true;
            }
        }
        if(isAuthorMode || (!hideGroup && MadisonUtil.isGroupsOrGroupLinksVisible(group.getUserType(), isInternalUser))) {
            int columnNumber = StringUtils.isNotEmpty(useColumn)
                    ? Integer.parseInt(useColumn.substring(useColumn.length() - 1))
                    : Integer.parseInt(stackOn.substring(stackOn.length() - 1));
            Resource links = value.getChild(LINKS_NODE);
            group.setMultiFieldSitemapLinksBeanList(generateSiteMapLinks(links));
            addToColumn(group, useColumn, stackOn, columnNumber);
        }
    }

    private void addToColumn(SiteMapGroup menuItem, String useColumn, String stackOn, int columnNumber) {    
        if (siteMapItems.get(columnNumber - 1) != null && StringUtils.isNotEmpty(useColumn)) {
            siteMapItems.get(columnNumber - 1).add(0, menuItem);
        } else if (siteMapItems.get(columnNumber - 1) != null && StringUtils.isNotEmpty(stackOn)) {
            siteMapItems.get(columnNumber - 1).add(menuItem);
        }
    }

    
    private List<SiteMapLink> generateSiteMapLinks(Resource links) {
        List<SiteMapLink> sitemapLink = new ArrayList<>();
        if(links != null) {
            for (Resource link : links.getChildren()) {
                SiteMapLink groupLinksItem = Objects.requireNonNull(link.adaptTo(SiteMapLink.class));
                String navigationURL = groupLinksItem.getNavigationURL();
                Boolean hideItem = false;
                if (StringUtils.isNotBlank(navigationURL) && navigationURL.startsWith(MadisonConstants.SLASH_CONTENT)) {
                    String processedlinkPath = MadisonUtil.getUrlForPageResource(navigationURL);
                    groupLinksItem.setNavigationURL(processedlinkPath);
                    String linkLocale = MadisonUtil.getLocaleForPath(navigationURL);
                    if(linkLocale == null || !linkLocale.equals(pageLocale)) {
                        hideItem = true;
                    }
                }
                if(isAuthorMode || (!hideItem && MadisonUtil.isGroupsOrGroupLinksVisible(groupLinksItem.getUserType(), isInternalUser))) {
                    sitemapLink.add(groupLinksItem);
                }
            }
        }
        return sitemapLink;
    }

    @Override
    public List<List<SiteMapGroup>> getSiteMapItems() {
        return siteMapItems;
    }

    @Override
    public String getSiteMapLabel() {
        return sitemapLabel;
    }
}
