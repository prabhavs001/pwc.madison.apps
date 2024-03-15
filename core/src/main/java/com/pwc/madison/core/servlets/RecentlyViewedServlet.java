package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.util.Objects;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.services.RecentlyViewedService;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.userreg.utils.UserRegUtil;

/**
 * Recently Viewed Servlet which is used to call the Add method of the service
 * only for the logged in user. 
 * In case of Anonymous User, the Servlet throws 501 as Response
 *
 */
@Component(
        immediate = true,
        service = { Servlet.class },
        enabled = true,
        property = { Constants.SERVICE_DESCRIPTION + "= Recently Viewed Servlet",
                "sling.servlet.methods=" + HttpConstants.METHOD_POST,
                "sling.servlet.paths=" + "/bin/pwc-madison/recentlyviewed" })
public class RecentlyViewedServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(RecentlyViewedServlet.class);
    
    @Reference
    private transient UserRegRestService userRegRestService;

    @Reference
    private transient CountryTerritoryMapperService countryTerritoryMapperService;

    @Reference
    private transient CryptoSupport cryptoSupport;
    
    @Reference
    private transient UserPreferencesProviderService userPreferencesProviderService;

    @Reference
    private RecentlyViewedService recentlyViewedService;
    
    @Reference
    private transient XSSAPI xssapi;
        
    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException, ServletException {
        boolean statusFlag = false;  
        final ResourceResolver resolver = request.getResourceResolver();
        String url = request.getParameter(MadisonConstants.ITEM_PATH) != null ? 
        		request.getParameter(MadisonConstants.ITEM_PATH).split(MadisonConstants.HTML_EXTN)[0] : StringUtils.EMPTY;
        String itemPath = resolver.resolve(url).getPath();
        LOGGER.debug("Resource Path: " + itemPath);
        try {
            final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
            if (!Objects.isNull(madisonCookie) && StringUtils.isNotBlank(itemPath)) {
                final UserProfile userProfile = UserInformationUtil.getUserProfile(request, cryptoSupport, true,
                        userRegRestService, countryTerritoryMapperService, response, userPreferencesProviderService, false, true, xssapi);
                statusFlag = recentlyViewedService.addRecentlyViewedItem(userProfile, itemPath, 
                		madisonCookie.getValue());
            } else {
            	if(statusFlag == true) {
                	LOGGER.debug("Status Flag: " + statusFlag);
                	response.setStatus(SlingHttpServletResponse.SC_OK);
                } else {
                	LOGGER.debug("Status Flag: " + statusFlag);
                    response.setStatus(SlingHttpServletResponse.SC_NOT_IMPLEMENTED);
                }
                response.flushBuffer();
            }
        } catch (final Exception e) {
            LOGGER.error("Error in doGet() method of RecentlyViewedServlet: {}", e);
        }
    }    
}
