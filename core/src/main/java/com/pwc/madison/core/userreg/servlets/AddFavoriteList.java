package com.pwc.madison.core.userreg.servlets;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.Constants;
import com.pwc.madison.core.userreg.models.request.AddFavoriteListRequest;
import com.pwc.madison.core.userreg.services.FavoriteListService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserRegUtil;
import com.sun.jersey.api.client.ClientResponse;

/**
 * 
 * Servlet to handle Add Favorite List request from Viewpoint.
 * 
 */
@Component(
        service = Servlet.class,
        name = "PwC Viewpoint Add Favorite List Servlet",
        property = { org.osgi.framework.Constants.SERVICE_DESCRIPTION + "=PwC Viewpoint Add Favorite List Servlet",
                "sling.servlet.paths=/bin/userreg/addfavoritelist",
                "sling.servlet.methods=" + HttpConstants.METHOD_POST })
public class AddFavoriteList extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(AddFavoriteList.class);

    @Reference
    private transient UserRegRestService userRegRestService;

    @Reference
    private transient FavoriteListService favoriteListService;
    
    @Reference
    private transient CountryTerritoryMapperService countryTerritoryMapperService;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
        if (null != madisonCookie) {
            final Object requestObject = UserRegUtil.getObjectFromRequest(request, AddFavoriteListRequest.class);
            if (null != requestObject) {
                final AddFavoriteListRequest addFavoriteListRequest = (AddFavoriteListRequest) requestObject;
                final ClientResponse clientResponse = userRegRestService.addFavoriteList(addFavoriteListRequest,
                        madisonCookie.getValue());
                String responseString = clientResponse.getEntity(String.class);
                if (clientResponse.getStatus() == SlingHttpServletResponse.SC_OK) {
                    responseString = UserRegUtil.updateFavoriteListFolderResponse(responseString, request,
                            addFavoriteListRequest.getUpdatePagesData(), null,
                            favoriteListService.getFavoritePanelListLimit(), madisonCookie.getValue(),
                            userRegRestService, countryTerritoryMapperService);
                } else if (clientResponse.getStatus() == SlingHttpServletResponse.SC_UNAUTHORIZED) {
                    UserRegUtil.removeUserRegMadisonCookie(request, response);
                }
                LOGGER.debug("AddFavoriteList doPost() : Returning response {}", responseString);
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

}
