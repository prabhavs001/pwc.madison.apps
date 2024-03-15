package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.google.gson.Gson;
import com.pwc.madison.core.models.RecentlyViewedItem;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.services.RecentlyViewedService;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.userreg.utils.UserRegUtil;

/**
 * Servlet to get recently viewed items
 *
 */
@Component(
        service = Servlet.class,
        name = "PwC Viewpoint Get Recently Viewed Servlet",
        property = { Constants.SERVICE_DESCRIPTION + "= PwC Viewpoint Get Recently Viewed Servlet",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                "sling.servlet.paths=" + "/bin/pwc-madison/getrecentlyviewed" })
public class GetRecentlyViewedServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(GetRecentlyViewedServlet.class);

    @Reference
    private transient CountryTerritoryMapperService countryTerritoryMapperService;

    @Reference
    private transient CryptoSupport cryptoSupport;
    
    @Reference
    private transient UserPreferencesProviderService userPreferencesProviderService;

    @Reference
    private RecentlyViewedService recentlyViewedService;

    @Inject
    private transient UserRegRestService userregRestService;


    private List<RecentlyViewedItem> recentlyViewedItemsList;
    
    private ResourceResolver resourceResolver;
    
    @Reference
    private transient XSSAPI xssapi;
        
    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException, ServletException {
        try {
            final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
            if (madisonCookie != null) {
                final UserProfile userProfile = UserInformationUtil.getUserProfile(request, cryptoSupport, true,
                    userregRestService, countryTerritoryMapperService, response, userPreferencesProviderService, false, true, xssapi);
                if(null != userProfile && StringUtils.isNotBlank(madisonCookie.getValue())){
                	resourceResolver = request.getResourceResolver();
                    recentlyViewedItemsList = recentlyViewedService.getRecentlyViewedItem(request, userProfile,
                        madisonCookie.getValue(), resourceResolver);
                    LOGGER.debug("Recently viewd items: {}", recentlyViewedItemsList);
                    final String responseString = new Gson().toJson(recentlyViewedItemsList);
                    response.setContentType(com.pwc.madison.core.userreg.Constants.CONTENT_TYPE_JSON);
                    response.setCharacterEncoding(com.pwc.madison.core.userreg.Constants.UTF_8_ENCODING);
                    if(null != responseString || StringUtils.isNotBlank(responseString)){
                        response.getWriter().write(responseString);
                    }else{
                        LOGGER.debug("No recently Viewed Items");
                    }
                }
            } else {
                LOGGER.debug("User is not logged in.");
            }
        } catch (final Exception e) {
            LOGGER.error("Error in doGet() method of RecentlyViewedServlet: {}", e);
        } finally {
    	   if(resourceResolver!=null)
    		   if(resourceResolver.isLive())
    				resourceResolver.close();
		}
    }
}
