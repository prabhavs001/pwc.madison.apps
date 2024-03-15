package com.pwc.madison.core.userreg.servlets;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.google.gson.Gson;
import com.pwc.madison.core.models.Territory;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.Constants;
import com.pwc.madison.core.userreg.models.request.RegisterRequest;
import com.pwc.madison.core.userreg.models.request.UserAcceptTncRequest;
import com.pwc.madison.core.userreg.models.response.CompleteProfileAnalyticResponse;
import com.pwc.madison.core.userreg.models.response.EditProfileAndChangePasswordResponse;
import com.pwc.madison.core.userreg.models.response.EditProfileResponse;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.userreg.utils.UserRegUtil;
import com.sun.jersey.api.client.ClientResponse;

/**
 * 
 * Servlet to handle User edit profile request from Viewpoint.
 * 
 */
@Component(
        service = Servlet.class,
        name = "PwC Viewpoint Edit User Profile Servlet",
        property = {
                org.osgi.framework.Constants.SERVICE_DESCRIPTION
                        + "=PwC Viewpoint Edit User Profile And Change Password Servlet",
                "sling.servlet.paths=/bin/userreg/editprofile", "sling.servlet.methods=" + HttpConstants.METHOD_PUT })
public class EditProfile extends SlingAllMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditProfile.class);

    @Reference
    private transient UserRegRestService userRegRestService;

    @Reference
    private transient ResourceResolverFactory resourceResolverFactory;

    @Reference
    private transient CountryTerritoryMapperService countryTerritoryMapperService;

    @Reference
    private transient UserPreferencesProviderService userPreferencesProviderService;

    @Reference
    private transient CryptoSupport cryptoSupport;
    
    @Reference
    private XSSAPI xssAPI;


    @Override
    protected void doPut(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
        if (null != madisonCookie) {
            final Object requestObject = UserRegUtil.getObjectFromRequest(request, RegisterRequest.class);
            if (null != requestObject) {
                final RegisterRequest editProfileRequest = (RegisterRequest) requestObject;
                
				if (StringUtils.isNotBlank(editProfileRequest.getIsCompleteProfile())) {
					final UserAcceptTncRequest userAcceptTncRequest = editProfileRequest.getUserAcceptTnc();
					userAcceptTncRequest.setVersionAccepted(countryTerritoryMapperService.getTerritoryCodeToTerritoryMap()
							.get(userAcceptTncRequest.getTerritoryCode()).getTermsAndConditionsVersion());
				}else {
                	// do not send tnc for edit profile
                	editProfileRequest.setUserAcceptTnc(null);
                }
                final EditProfileAndChangePasswordResponse editProfileAndChangePasswordResponse = new EditProfileAndChangePasswordResponse();
                editProfile(editProfileRequest, madisonCookie, editProfileAndChangePasswordResponse, response, request);
                if (editProfileAndChangePasswordResponse
                        .getEditProfileStatus() == SlingHttpServletResponse.SC_UNAUTHORIZED) {
                    UserRegUtil.removeUserRegMadisonCookie(request, response);
                    response.sendError(SlingHttpServletResponse.SC_UNAUTHORIZED);
                } else {
                	// if it is complete profile set fields required for analytics
					if (StringUtils.isNotBlank(editProfileRequest.getIsCompleteProfile())) {
						CompleteProfileAnalyticResponse completeProfileAnalyticResponse = new CompleteProfileAnalyticResponse();
						completeProfileAnalyticResponse.setIndustryTags(
								userPreferencesProviderService.getTagsByPath(editProfileRequest.getIndustry()));
						completeProfileAnalyticResponse.setTitleTags(
								userPreferencesProviderService.getTagsByPath(editProfileRequest.getTitle()));
						completeProfileAnalyticResponse.setFunctionalRoleTitle(
								userPreferencesProviderService.getTitleByPath(editProfileRequest.getTitle()));
						completeProfileAnalyticResponse.setIndustryTitle(
								userPreferencesProviderService.getTitleByPath(editProfileRequest.getIndustry()));

						editProfileAndChangePasswordResponse
								.setCompleteProfileAnalyticResponse(completeProfileAnalyticResponse);
					}
                    final String responseString = new Gson().toJson(editProfileAndChangePasswordResponse);
                    response.setContentType(Constants.CONTENT_TYPE_JSON);
                    response.setCharacterEncoding(Constants.UTF_8_ENCODING);
                    response.getWriter().write(responseString);
                }
            } else {
                response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            response.sendError(SlingHttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    /**
     * Perform the edit Profile operation.
     * 
     * @param editProfileRequest
     *            {@link EditProfileRequest}
     * @param madisonCookie
     *            {@link Cookie}
     * @param editProfileAndChangePasswordResponse
     *            {@link EditProfileAndChangePasswordResponse}
     * @param response
     *            {@link SlingHttpServletResponse}
     * @param request
     *            {@link SlingHttpServletRequest}
     */
    private void editProfile(final RegisterRequest editProfileRequest, final Cookie madisonCookie,
            final EditProfileAndChangePasswordResponse editProfileAndChangePasswordResponse,
            final SlingHttpServletResponse response, final SlingHttpServletRequest request) {
        final Territory territory = countryTerritoryMapperService
                .getTerritoryByCountryCode(editProfileRequest.getCountry(), true);
        final String locale = countryTerritoryMapperService.getLocaleFromCountry(editProfileRequest.getCountry());
        editProfileRequest.setLanguage(locale);
        editProfileRequest.setTerritoryCode(territory.getTerritoryCode());
        editProfileRequest.setPrimaryTerritory(territory.getTerritoryCode());
        final ClientResponse clientResponse = userRegRestService.editProfile(editProfileRequest,
                madisonCookie.getValue());
        final String responseString = clientResponse.getEntity(String.class);
        final EditProfileResponse editProfileResponse = new Gson().fromJson(responseString, EditProfileResponse.class);
        if (clientResponse.getStatus() == SlingHttpServletResponse.SC_OK) {
            LOGGER.debug(
                    "EditProfile editProfile() : Initaiting Profile creation/updation in AEM {}",
                    xssAPI.encodeForHTML(editProfileResponse.getData().getUserProfile().getEmail()));
            UserInformationUtil.updateMadisonUserProfileCookie(request, response,
                    editProfileResponse.getData().getUserProfile(), countryTerritoryMapperService, cryptoSupport,
                    userPreferencesProviderService, false, xssAPI);
        }
        editProfileAndChangePasswordResponse.setEditProfileStatus(clientResponse.getStatus());
        editProfileAndChangePasswordResponse.setEditProfileResponse(editProfileResponse);
    }

}
