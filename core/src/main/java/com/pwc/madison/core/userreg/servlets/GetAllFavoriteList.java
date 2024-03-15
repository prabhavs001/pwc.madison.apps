package com.pwc.madison.core.userreg.servlets;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.Constants;
import com.pwc.madison.core.userreg.services.FavoriteListService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserRegUtil;
import com.sun.jersey.api.client.ClientResponse;

/**
 * 
 * Servlet to handle Get Favorite List request from Viewpoint.
 * 
 */
@Component(
        service = Servlet.class,
        name = "PwC Viewpoint Get Favorite List Servlet",
        property = { org.osgi.framework.Constants.SERVICE_DESCRIPTION + "=PwC Viewpoint Get Favorite List Servlet",
                "sling.servlet.paths=/bin/userreg/getfavoritelist",
                "sling.servlet.methods=" + HttpConstants.METHOD_POST })
public class GetAllFavoriteList extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(GetAllFavoriteList.class);

    private static final String UPDATE_PAGES_QUERY_PARAMTER = "updatePages";

	private static final String CURRENT_PAGE_TERRITORY_CODE_QUERY_PARAMTER = "currentPageTerritoryCode";

    @Reference
    private transient UserRegRestService userRegRestService;

    @Reference
    private transient FavoriteListService favoriteListService;
    
    @Reference
    private transient CountryTerritoryMapperService countryTerritoryMapperService;
    
    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
        if (null != madisonCookie) {
            Boolean updatePagesData = Boolean.parseBoolean(request.getParameter(UPDATE_PAGES_QUERY_PARAMTER));
            String requestPageTerritoryCode = request.getParameter(CURRENT_PAGE_TERRITORY_CODE_QUERY_PARAMTER);
            final ClientResponse clientResponse = userRegRestService.getFavoriteList(madisonCookie.getValue());
            String responseString = clientResponse.getEntity(String.class);
            if (clientResponse.getStatus() == SlingHttpServletResponse.SC_OK) {
                responseString = UserRegUtil.updateFavoriteListFolderResponse(responseString, request, updatePagesData, requestPageTerritoryCode,
                        favoriteListService.getFavoritePanelListLimit(), madisonCookie.getValue(), userRegRestService, countryTerritoryMapperService);
            } else if (clientResponse.getStatus() == SlingHttpServletResponse.SC_UNAUTHORIZED) {
                UserRegUtil.removeUserRegMadisonCookie(request, response);
            }
            LOGGER.debug("getFavoriteList doPost() : Returning response {}", responseString);
            response.setContentType(Constants.CONTENT_TYPE_JSON);
            response.setCharacterEncoding(Constants.UTF_8_ENCODING);
            response.setStatus(clientResponse.getStatus());
            response.getWriter().write(responseString);
        } else {
            response.sendError(SlingHttpServletResponse.SC_UNAUTHORIZED);
        }
    }

}
