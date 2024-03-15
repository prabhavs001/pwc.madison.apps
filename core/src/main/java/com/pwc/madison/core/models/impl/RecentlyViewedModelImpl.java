package com.pwc.madison.core.models.impl;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.Cookie;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.RecentlyViewedItem;
import com.pwc.madison.core.models.RecentlyViewedModel;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.services.RecentlyViewedService;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.userreg.utils.UserRegUtil;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Recently viewed component model Implementation
 *
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = RecentlyViewedModel.class)
public class RecentlyViewedModelImpl implements RecentlyViewedModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecentlyViewedModelImpl.class);

    @Inject
    SlingHttpServletRequest request;

    @Inject
    SlingHttpServletResponse response;

    @Inject
    private transient UserRegRestService userregRestService;

    @Inject
    private transient CryptoSupport cryptoSupport;

    @Inject
    private transient CountryTerritoryMapperService countryTerritoryMapperService;
    
    @Inject
    private transient UserPreferencesProviderService userPreferencesProviderService;

    @OSGiService
    private RecentlyViewedService recentlyViewedService;
    
    @OSGiService
    private transient XSSAPI xssapi;

    private List<RecentlyViewedItem> recentlyViewedItemsList;
    
    private boolean isUserLoggedIn = false;
    
    @OSGiService
    private transient ResourceResolverFactory resourceResolverFactory;
    
    @SlingObject
    private ResourceResolver resourceResolver;

    @PostConstruct
    protected void init() {
       try {
    	   final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
           if (madisonCookie != null) {
               final UserProfile userProfile = UserInformationUtil.getUserProfile(request, cryptoSupport, true,
                       userregRestService, countryTerritoryMapperService, response, userPreferencesProviderService, false, false, xssapi);
               resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory, MadisonConstants.MADISON_READ_SUB_SERVICE);
               recentlyViewedItemsList = recentlyViewedService.getRecentlyViewedItem(request, userProfile,
                       madisonCookie.getValue(), resourceResolver);
               isUserLoggedIn = true;
               
           } else {
               LOGGER.debug("User is Anonymous");
           }
       } catch(Exception e) {
    	   LOGGER.error("Error in init() method of RecentlyViewedModelImpl: {}", e);
       }
    }
    
    @Override
    public List<RecentlyViewedItem> getRecentlyViewedItemsList() {
        return recentlyViewedItemsList;
    }

    @Override
	public boolean isUserLoggedIn() {
		return isUserLoggedIn;
	}

	@Override
    public String getComponentName(){
        return MadisonConstants.RECENTLY_VIEWED_COMPONENT_NAME;
    }
}
