package com.pwc.madison.core.userreg.services.impl;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.pwc.madison.core.services.MadisonDomainsService;
import com.pwc.madison.core.userreg.models.request.UserRegRequest;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.services.impl.UserRegRestServiceImpl.UserRegRestBaseUrlConfiguration;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;

@Component(service = UserRegRestService.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = UserRegRestBaseUrlConfiguration.class)
public class UserRegRestServiceImpl implements UserRegRestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRegRestService.class);

    @Reference
    private MadisonDomainsService madisonDomainsService;

    private static final String POST = "POST";
    private static final String GET = "GET";
    private static final String PUT = "PUT";
    private static final String HEAD = "HEAD";
    private static final String DELETE = "DELETE";

    private String userregRestApiBaseUrl;
    private String userregRestGetuserApiEnd;
    private String userregRestLogoutApiEnd;
    private String userregRestSamlLogoutApiEnd;
    private int authenticationSingleSeatLicenseCookieExpiryHours;
    private int authenticationConcurrentLicenseCookieExpiryHours;
    private int extendConcurrentLicenseSessionTimeoutInHours;
    private String userregRestEditProfileApiEnd;
    private String userLoginLink;
    private String userRegisterLink;
    private String userDefaultRedirectionPath;
    private String userAuthenticationApiPath;
    private String[] userregBlacklistedDomains;
    private String userregRestEditTerritoryLangPreferencesApiEnd;
    private String userregRestEditContentPreferencesApiEnd;
    private String userregAcceptTncApiEnd;
    private String userregAddRecentlyViewedApiEnd;
    private String userregGetRecentlyViewedApiEnd;
    private String userregRemoveRecentlyViewedApiEnd;
    private String userregInserRecentlyViewedApiEnd;
    private String userregValidateTokenApiEnd;
    private String errorPageUrl;
    private int internalUserauthenticationCookieExpiryHours;
    private String userregRestUpdateLoginApiEnd;
    private String userregAddFavoriteListApiEnd;
    private String userregDeleteFavoriteListApiEnd;
    private String userregGetFavoriteListApiEnd;
    private String userregGetContentAccessInfoApiEnd;
    private String userregExtendConcurrentUserSessionTimeoutApiEnd;
    private String vpGetRedirectPathApiEnd;
    private String vpAddRedirectionsApiEnd;
    private String fdKey;

    @Activate
    @Modified
    protected void Activate(final UserRegRestBaseUrlConfiguration baseUrlConfiguration) {
        userregRestApiBaseUrl = baseUrlConfiguration.madison_userreg_api_base_url();
        userregRestGetuserApiEnd = baseUrlConfiguration.madison_userreg_api_getuser_end();
        authenticationSingleSeatLicenseCookieExpiryHours = baseUrlConfiguration.madison_userreg_single_seat_license_cookie_expiry_time();
        authenticationConcurrentLicenseCookieExpiryHours = baseUrlConfiguration.madison_userreg_concurrent_license_cookie_expiry_time();
        extendConcurrentLicenseSessionTimeoutInHours = baseUrlConfiguration.madison_userreg_extend_concurrent_user_session_timeout();
        userregRestLogoutApiEnd = baseUrlConfiguration.madison_userreg_api_logout_end();
        userregRestSamlLogoutApiEnd = baseUrlConfiguration.madison_userreg_api_saml_logout_end();
        userregRestEditProfileApiEnd = baseUrlConfiguration.madison_userreg_api_editprofile_end();
        userLoginLink = baseUrlConfiguration.madison_user_login_link();
        userRegisterLink = baseUrlConfiguration.madison_user_register_link();
        userDefaultRedirectionPath = baseUrlConfiguration.madison_user_redirection_pagepath();
        userAuthenticationApiPath = baseUrlConfiguration.madison_user_api_authentication_path();
        userregBlacklistedDomains = baseUrlConfiguration.madison_userreg_blacklisted_domains();
        userregRestEditTerritoryLangPreferencesApiEnd = baseUrlConfiguration
                .madison_userreg_api_editpreference_territorylang_end();
        userregRestEditContentPreferencesApiEnd = baseUrlConfiguration.madison_userreg_api_editpreference_content_end();
        userregAcceptTncApiEnd = baseUrlConfiguration.madison_userreg_api_accept_tnc_end();
        userregAddRecentlyViewedApiEnd = baseUrlConfiguration.madison_userreg_api_add_recently_viewed_end();
        userregGetRecentlyViewedApiEnd = baseUrlConfiguration.madison_userreg_api_get_recently_viewed_end();
        userregRemoveRecentlyViewedApiEnd = baseUrlConfiguration.madison_userreg_api_remove_recently_viewed_end();
        userregInserRecentlyViewedApiEnd = baseUrlConfiguration.madison_userreg_api_insert_recently_viewed_end();
        userregValidateTokenApiEnd = baseUrlConfiguration.madison_userreg_api_validate_token_end();
        errorPageUrl = baseUrlConfiguration.madison_server_error_page_url();
        internalUserauthenticationCookieExpiryHours = baseUrlConfiguration
                .madison_userreg_internal_cookie_expiry_time();
        userregRestUpdateLoginApiEnd = baseUrlConfiguration.madison_userreg_api_update_login_end();
        userregAddFavoriteListApiEnd = baseUrlConfiguration.madison_userreg_api_add_favorite_list_end();
        userregDeleteFavoriteListApiEnd = baseUrlConfiguration.madison_userreg_api_delete_favorite_list_end();
        userregGetFavoriteListApiEnd = baseUrlConfiguration.madison_userreg_api_get_favorite_list_end();
        userregGetContentAccessInfoApiEnd = baseUrlConfiguration.madison_userreg_api_get_content_access_info_end();
        userregExtendConcurrentUserSessionTimeoutApiEnd = baseUrlConfiguration.madison_userreg_api_extend_concurrent_user_session_timeout_end();
        vpGetRedirectPathApiEnd = baseUrlConfiguration.madison_viewpoint_api_get_redirect_path_end();
        vpAddRedirectionsApiEnd = baseUrlConfiguration.madison_viewpoint_api_add_redirections_end();
        fdKey = baseUrlConfiguration.madison_viewpoint_fd_key();
        
        LOGGER.debug("UserRegRestService Activate() UserReg Rest API base URL : {}", userregRestApiBaseUrl);
        LOGGER.debug("UserRegRestService Activate() UserReg Rest API Get User End : {}", userregRestGetuserApiEnd);
        LOGGER.debug("UserRegRestService Activate() UserReg Authentication Single Seat License Cookie Expiry Time : {}",
                authenticationSingleSeatLicenseCookieExpiryHours);
        LOGGER.debug("UserRegRestService Activate() UserReg Authentication Concurrent License Cookie Expiry Time : {}",
                authenticationConcurrentLicenseCookieExpiryHours);
        LOGGER.debug("UserRegRestService Activate() UserReg Extend Concurrent License Session Timeout : {}",
                extendConcurrentLicenseSessionTimeoutInHours);
        LOGGER.debug("UserRegRestService Activate() UserReg Rest API Logout End : {}", userregRestLogoutApiEnd);
        LOGGER.debug("UserRegRestService Activate() UserReg Rest API SAML Logout End : {}",
                userregRestSamlLogoutApiEnd);
        LOGGER.debug("UserRegRestService Activate() UserReg Rest API Edit User Profile End : {}",
                userregRestEditProfileApiEnd);
        LOGGER.debug("UserRegRestService Activate() UserReg Rest API User Login Link : {}",
                userLoginLink);
        LOGGER.debug("UserRegRestService Activate() UserReg Rest API User Register Link : {}",
                userRegisterLink);
        LOGGER.debug("UserRegRestService Activate() UserReg User Default Page Path : {}",
                userDefaultRedirectionPath);
        LOGGER.debug("UserRegRestService Activate() UserReg User Authentication API Path : {}",
                userAuthenticationApiPath);
        LOGGER.debug("UserRegRestService Activate() UserReg Rest API Blacklisted Domains : {}",
                userregBlacklistedDomains);
        LOGGER.debug("UserRegRestService Activate() UserReg Rest API Edit Territory Language Preferences End : {}",
                userregRestEditTerritoryLangPreferencesApiEnd);
        LOGGER.debug("UserRegRestService Activate() UserReg Rest API Edit Content Preferences End : {}",
                userregRestEditContentPreferencesApiEnd);
        LOGGER.debug("UserRegRestService Activate() UserReg Rest API Accept Terms and Conditions End : {}",
                userregAcceptTncApiEnd);
        LOGGER.debug("UserRegRestService Activate() UserReg Rest Add recent review End : {}",
                userregAddRecentlyViewedApiEnd);
        LOGGER.debug("UserRegRestService Activate() UserReg Rest get recent review End : {}",
                userregGetRecentlyViewedApiEnd);
        LOGGER.debug("UserRegRestService Activate() UserReg Rest remove recent review End : {}",
                userregRemoveRecentlyViewedApiEnd);
        LOGGER.debug("UserRegRestService Activate() UserReg Rest insert recent review End : {}",
                userregInserRecentlyViewedApiEnd);
        LOGGER.debug("UserRegRestService Activate() UserReg Rest API Validate Token Request End : {}",
                userregValidateTokenApiEnd);
        LOGGER.debug("UserRegRestService Activate() UserReg Server Error Page Path Url: {}", errorPageUrl);
        LOGGER.debug("UserRegRestService Activate() UserReg Internal User Authentication Cookie Expiry Time : {}",
                internalUserauthenticationCookieExpiryHours);
        LOGGER.debug("UserRegRestService Activate() UserReg Rest Update Login Api End : {}",
                userregRestUpdateLoginApiEnd);
        LOGGER.debug("UserRegRestService Activate() UserReg Rest Add Favorite List Api End : {}",
                userregAddFavoriteListApiEnd);
        LOGGER.debug("UserRegRestService Activate() UserReg Rest Delete Favorite List Api End : {}",
                userregDeleteFavoriteListApiEnd);
        LOGGER.debug("UserRegRestService Activate() UserReg Rest Get All Favorite List Api End : {}",
                userregGetFavoriteListApiEnd);
        LOGGER.debug("UserRegRestService Activate() UserReg Get User Access Content Information Api End : {}",
                userregGetContentAccessInfoApiEnd);
        LOGGER.debug("UserRegRestService Activate() UserReg Extend Concurrent User Session Timeout Api End : {}",
                userregExtendConcurrentUserSessionTimeoutApiEnd);
        LOGGER.debug("UserRegRestService Activate() Vp Get Redirect Path For Old Path Api End : {}",
                userregGetContentAccessInfoApiEnd);
        LOGGER.debug("UserRegRestService Activate() Vp Add Redirections Api End : {}", vpAddRedirectionsApiEnd);
        LOGGER.debug("UserRegRestService Activate() FD Key : {}", fdKey);
    }

    /**
     * Returns {@link ClientResponse} for the given API URL for the given HTTP method.
     *
     * @param method
     *            {@link String}
     * @param apiUrl
     *            {@link String}
     * @param userRegRequestObject
     *            null in case no request data is to be send to rest API otherwise the {@Object} whose JSOn equivalent
     *            data is sent in API request
     * @param token
     *            {@String} authentication token, null in case no authentication token is required
     * @return {@link ClientResponse}
     */
    private <T extends UserRegRequest> ClientResponse getClientResponse(final String method, final String apiUrl,
            final T userRegRequestObject, final String token) {
        final Client client = Client.create();
        final WebResource webResource = client.resource(userregRestApiBaseUrl + apiUrl);
        final Builder builder = webResource.type(MediaType.APPLICATION_JSON);
        ClientResponse response = null;
        if (null != token) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (method.equals(GET)) {
            response = builder.get(ClientResponse.class);
        } else {
            String postDataString = null;
            if (null != userRegRequestObject) {
                postDataString = new Gson().toJson(userRegRequestObject);
            }
            if (StringUtils.isBlank(postDataString)) {
                postDataString = "{}";
            }
            if (method.equals(POST)) {
                response = builder.post(ClientResponse.class, postDataString);
            } else if (method.equals(PUT)) {
                response = builder.put(ClientResponse.class, postDataString);
            } else if (method.equals(HEAD)) {
                response = builder.head();
            } else if (method.equals(DELETE)) {
                response = builder.delete(ClientResponse.class, postDataString);
            }
        }
        return response;
    }

    @ObjectClassDefinition(name = "PwC Viewpoint Core UserReg Rest Configuration")
    public @interface UserRegRestBaseUrlConfiguration {

        @AttributeDefinition(
                name = "UserReg Rest API Base URL",
                description = "The Base URL of the UserReg Rest API that is of the form protocol://host:port(Eg: https://localhost:8101) or protocol://domain((Eg: https://ums-rest-api.com))")
        String madison_userreg_api_base_url();

        @AttributeDefinition(name = "UserReg Rest API Get User End", description = "UserReg Rest API Get User End")
        String madison_userreg_api_getuser_end();

        @AttributeDefinition(name = "UserReg Rest API Logout End", description = "UserReg Rest API Logout End")
        String madison_userreg_api_logout_end();

        @AttributeDefinition(
                name = "UserReg Rest API SAML Logout End",
                description = "UserReg Rest API SAML Logout End")
        String madison_userreg_api_saml_logout_end();

        @AttributeDefinition(
                name = "UserReg External User Authentication Single Seat License Cookie Expiry Time",
                description = "UserReg Authentication Single Seat License Cookie Expiry Time in hours. Specify the expiry time in hours and input must be an integer number",
                type = AttributeType.INTEGER,
                min = "1")
        int madison_userreg_single_seat_license_cookie_expiry_time() default 1;

        @AttributeDefinition(
                name = "UserReg External User Authentication Concurrent License Cookie Expiry Time",
                description = "UserReg Authentication Concurrent License Cookie Expiry Time in hours. Specify the expiry time in hours and input must be an integer number",
                type = AttributeType.INTEGER,
                min = "1")
        int madison_userreg_concurrent_license_cookie_expiry_time() default 1;

        @AttributeDefinition(
                name = "UserReg Extend External User Authentication Concurrent License Session Timeout",
                description = "UserReg Extend External User Authentication Concurrent License Session Timeout in hours. Specify the expiry time in hours and input must be an integer number",
                type = AttributeType.INTEGER,
                min = "1")
        int madison_userreg_extend_concurrent_user_session_timeout() default 1;

        @AttributeDefinition(
                name = "UserReg Rest API Edit User Profile End",
                description = "UserReg Rest API Edit User Profile End")
        String madison_userreg_api_editprofile_end();

        @AttributeDefinition(
                name = "UserReg Rest API Complete User Profile Request End",
                description = "UserReg Rest API Complete User Profile Request End")
        String madison_userreg_api_complete_profile_end();

        @AttributeDefinition(
                name = "UserReg User Login Link",
                description = "UserReg User Login Link")
        String madison_user_login_link();

        @AttributeDefinition(
                name = "UserReg User Register Link",
                description = "UserReg User Register Link")
        String madison_user_register_link();

        @AttributeDefinition(
                name = "Blacklisted Domains",
                description = "Patterns for domains that are not allowed as permissible domains for external users.",
                type = AttributeType.STRING)
        String[] madison_userreg_blacklisted_domains();

        @AttributeDefinition(
                name = "UserReg User Default Page Path",
                description = "The default Madison page path where user is redirected if no madison redirection cookie is found/present")
        String madison_user_redirection_pagepath();

        @AttributeDefinition(
                name = "UserReg User API Authentication Path",
                description = "UserReg User API Authentication Path")
        String madison_user_api_authentication_path();

        @AttributeDefinition(
                name = "UserReg Rest API Edit Territory Language Preferences Request End",
                description = "UserReg Rest API Edit Territory Language Preferences Request End")
        String madison_userreg_api_editpreference_territorylang_end();

        @AttributeDefinition(
                name = "UserReg Rest API Edit Content Preferences Request End",
                description = "UserReg Rest API Edit Content Preferences Request End")
        String madison_userreg_api_editpreference_content_end();

        @AttributeDefinition(
                name = "UserReg Rest API Accept Terms and Conditions Request End",
                description = "UserReg Rest API Accept Terms and Conditions Request End")
        String madison_userreg_api_accept_tnc_end();

        @AttributeDefinition(
                name = "UserReg Add Recently viewed Page Request End",
                description = "UserReg Add Recently viewed Page Request End",
                type = AttributeType.STRING)
        String madison_userreg_api_add_recently_viewed_end();

        @AttributeDefinition(
                name = "UserReg Get Recently viewed Page Request End",
                description = "UserReg Get Recently viewed Page Request End",
                type = AttributeType.STRING)
        String madison_userreg_api_get_recently_viewed_end();

        @AttributeDefinition(
                name = "UserReg Remove Recently viewed Page Request End",
                description = "UserReg Remove Recently viewed Page Request End",
                type = AttributeType.STRING)
        String madison_userreg_api_remove_recently_viewed_end();

        @AttributeDefinition(
                name = "UserReg Insert Recently viewed Page Request End",
                description = "UserReg Insert Recently viewed Page Request End",
                type = AttributeType.STRING)
        String madison_userreg_api_insert_recently_viewed_end();

        @AttributeDefinition(
                name = "UserReg Rest API Validate Token Request End",
                description = "UserReg Rest API Validate Token Request End",
                type = AttributeType.STRING)
        String madison_userreg_api_validate_token_end();

        @AttributeDefinition(
                name = "UserReg Server Error Page Path Url",
                description = "The page path where a user is redirected if some error occurs while SAML authentication. The URL can have placeholders like for <territory> and <language> code which can then be replaced with requested page territory code and language code.",
                type = AttributeType.STRING)
        String madison_server_error_page_url();

        @AttributeDefinition(
                name = "UserReg Internal user Authentication Cookie Expiry Time",
                description = "UserReg Internal User Authentication Cookie Expiry Time in hours. Specify the expiry time in hours and input must be an integer number",
                type = AttributeType.INTEGER,
                min = "1")
        int madison_userreg_internal_cookie_expiry_time() default 1;

        @AttributeDefinition(
                name = "UserReg Rest API Update Login Request End",
                description = "UserReg Rest API Update Login Request End",
                type = AttributeType.STRING)
        String madison_userreg_api_update_login_end();

        @AttributeDefinition(
                name = "UserReg Rest API Add Favorite List Request End",
                description = "UserReg Rest API Add Favorite List Request End",
                type = AttributeType.STRING)
        String madison_userreg_api_add_favorite_list_end();

        @AttributeDefinition(
                name = "UserReg Rest API Delete Favorite List Request End",
                description = "UserReg Rest API Delete Favorite List Request End",
                type = AttributeType.STRING)
        String madison_userreg_api_delete_favorite_list_end();

        @AttributeDefinition(
                name = "UserReg Rest API Get All Favorite List Request End",
                description = "UserReg Rest API Get All Favorite List Request End",
                type = AttributeType.STRING)
        String madison_userreg_api_get_favorite_list_end();

        @AttributeDefinition(
                name = "UserReg Rest API Get User Content Access Information End",
                description = "UserReg Rest API Get User Content Access Information End",
                type = AttributeType.STRING)
        String madison_userreg_api_get_content_access_info_end();

        @AttributeDefinition(
                name = "UserReg Rest API Extend Concurrent User Session Timeout End",
                description = "UserReg Rest API Extend Concurrent User Session Timeout End",
                type = AttributeType.STRING)
        String madison_userreg_api_extend_concurrent_user_session_timeout_end();

        @AttributeDefinition(
                name = "VP Redirection Rest API Get Redirect Path End",
                description = "VP Redirection Rest API Get Redirect Path End",
                type = AttributeType.STRING)
        String madison_viewpoint_api_get_redirect_path_end();

        @AttributeDefinition(
                name = "VP Redirection Rest API Add Redirections End",
                description = "VP Redirection Rest API Add Redirections End",
                type = AttributeType.STRING)
        String madison_viewpoint_api_add_redirections_end();
        
        @AttributeDefinition(
                name = "FD Key",
                description = "Secret key to establish mechanism of encryption and decryption between UMS API and Viewpoint",
                type = AttributeType.STRING)
        String madison_viewpoint_fd_key();


    }

    @Override
    public ClientResponse getUser(final String token) {
        return getClientResponse(GET, userregRestGetuserApiEnd, null, token);
    }

    @Override
    public ClientResponse logout(final String token) {
        return getClientResponse(POST, userregRestLogoutApiEnd, null, token);
    }

    @Override
    public String getSamlLogoutEndPoint() {
        return userregRestApiBaseUrl + userregRestSamlLogoutApiEnd;
    }

    @Override
    public int getAuthenticationSingleSeatLicenseCookieExpiryHours() {
        return authenticationSingleSeatLicenseCookieExpiryHours;
    }

    @Override
    public int getAuthenticationConcurrentLicenseCookieExpiryHours() {
        return authenticationConcurrentLicenseCookieExpiryHours;
    }

    @Override
    public int getExtendConcurrentLicenseSessionTimeoutInHours() {
        return extendConcurrentLicenseSessionTimeoutInHours;
    }

    @Override
    public <T extends UserRegRequest> ClientResponse editProfile(final T userProfile, final String token) {
        return getClientResponse(PUT, userregRestEditProfileApiEnd, userProfile, token);
    }

    @Override
    public String getUserLoginLink() {
        return userLoginLink;
    }

    @Override
    public String getUserRegisterLink() { return userRegisterLink; }

    @Override
    public String[] getBlacklistedDomains() {
        return userregBlacklistedDomains;
    }

    @Override
    public String getUserDefaultRedirectionPath() {
        return madisonDomainsService.getDefaultDomain() + userDefaultRedirectionPath;
    }

    @Override
    public String getAuthenticationApiUrl() {
        return userregRestApiBaseUrl + userAuthenticationApiPath;
    }

    @Override
    public <T extends UserRegRequest> ClientResponse editTerritoryLanguagePreferences(
            final T editTerritoryLanguagePreferencesRequest, final String token) {
        return getClientResponse(PUT, userregRestEditTerritoryLangPreferencesApiEnd,
                editTerritoryLanguagePreferencesRequest, token);
    }

    @Override
    public <T extends UserRegRequest> ClientResponse editContentPreferences(final T editContentPreferencesRequest,
            final String token) {
        return getClientResponse(PUT, userregRestEditContentPreferencesApiEnd, editContentPreferencesRequest, token);
    }

    @Override
    public <T extends UserRegRequest> ClientResponse acceptTnc(final T acceptTncRequest, final String token) {
        return getClientResponse(POST, userregAcceptTncApiEnd, acceptTncRequest, token);
    }

    @Override
    public <T extends UserRegRequest> ClientResponse addRecentlyViewed(final T recentViewRequest, final String token) {
        return getClientResponse(POST, userregAddRecentlyViewedApiEnd, recentViewRequest, token);
    }

    @Override
    public <T extends UserRegRequest> ClientResponse getRecentlyViewed(final String token) {
        return getClientResponse(GET, userregGetRecentlyViewedApiEnd, null, token);
    }

    @Override
    public <T extends UserRegRequest> ClientResponse insertRecentlyViewedItemInTempTable(final T recentViewRequest,
            String token) {
        return getClientResponse(POST, userregInserRecentlyViewedApiEnd, recentViewRequest, token);
    }

    @Override
    public <T extends UserRegRequest> ClientResponse removeRecentlyViewedItemFromTempTable(final T recentViewRequest,
            String token) {
        return getClientResponse(POST, userregRemoveRecentlyViewedApiEnd, recentViewRequest, token);
    }

    @Override
    public ClientResponse validateToken(String token) {
        return getClientResponse(HEAD, userregValidateTokenApiEnd, null, token);
    }

    @Override
    public String getServerErrorPageUrl() {
        return madisonDomainsService.getDefaultDomain() + errorPageUrl;
    }

    @Override
    public int getInternalUserCookieExpiryHours() {
        return internalUserauthenticationCookieExpiryHours;
    }

    @Override
    public ClientResponse updateLoginHistory(String token) {
        return getClientResponse(POST, userregRestUpdateLoginApiEnd, null, token);
    }

    @Override
    public <T extends UserRegRequest> ClientResponse addFavoriteList(T addFavoriteList, String token) {
        return getClientResponse(POST, userregAddFavoriteListApiEnd, addFavoriteList, token);
    }

    @Override
    public <T extends UserRegRequest> ClientResponse deleteFavoriteList(T deleteFavoriteList, String token) {
        return getClientResponse(DELETE, userregDeleteFavoriteListApiEnd, deleteFavoriteList, token);
    }

    @Override
    public ClientResponse getFavoriteList(String token) {
        return getClientResponse(GET, userregGetFavoriteListApiEnd, null, token);
    }

    @Override
    public ClientResponse getUserContentAccessInformation(String token) {
        return getClientResponse(GET, userregGetContentAccessInfoApiEnd, null, token);
    }

    @Override
    public ClientResponse getExtendConcurrentLicenseSessionTimeout(String token) {
        return getClientResponse(GET, userregExtendConcurrentUserSessionTimeoutApiEnd,null, token);
    }

    @Override
    public ClientResponse getRedirectPath(String path) {
        return getClientResponse(GET, vpGetRedirectPathApiEnd + path, null, null);
    }

    @Override
    public <T extends UserRegRequest> ClientResponse addRedirects(T addRedirectionsRequest, String token) {
        return getClientResponse(POST, vpAddRedirectionsApiEnd, addRedirectionsRequest, token);
    }

    @Override
    public String getfdKey() {
        return fdKey;
    }
}
