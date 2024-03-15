package com.pwc.madison.core.userreg.services;

import com.pwc.madison.core.userreg.models.request.AddRedirectionsRequest;
import com.pwc.madison.core.userreg.models.request.RecentViewRequest;
import com.pwc.madison.core.userreg.models.request.UserRegRequest;
import com.sun.jersey.api.client.ClientResponse;

/**
 *
 * Service that allows to connect to UserReg rest APIs and provide related configuration.
 *
 */
public interface UserRegRestService {

    /**
     * returns {@link ClientResponse} after calling UMS Login API.
     *
     * @param token
     *            {@link String}
     * @return {@link ClientResponse}
     */
    public ClientResponse getUser(final String token);

    /**
     * returns Viewpoint authentication single seat license cookie expiry time in hours.
     *
     * @return {@link int}
     */
    public int getAuthenticationSingleSeatLicenseCookieExpiryHours();

    /**
     * returns Viewpoint authentication concurrent license cookie expiry time in hours.
     *
     * @return {@link int}
     */
    public int getAuthenticationConcurrentLicenseCookieExpiryHours();

    /**
     * returns Viewpoint extend external user authentication concurrent license session timeout in hours.
     *
     * @return {@link int}
     */
    public int getExtendConcurrentLicenseSessionTimeoutInHours();


    /**
     * returns {@link ClientResponse} after calling UMS Edit Profile API.
     *
     * @param userProfile
     * @param token
     *            {@link String}
     * @return {@link ClientResponse}
     */
    public <T extends UserRegRequest> ClientResponse editProfile(final T userProfile, final String token);

    public ClientResponse logout(final String token);

    /**
     * @return global logout URL for SAMl endpoint. Caller has to add the redirectUrl in the query param
     */
    String getSamlLogoutEndPoint();

    /**
     * returns User Login Link.
     *
     * @return {@link String}
     */
    public String getUserLoginLink();

    /**
     * returns User Register Link.
     *
     * @return {@link String}
     */
    public String getUserRegisterLink();

    /**
     * returns User default redirection path.
     *
     * @return {@link String}
     */
    public String getUserDefaultRedirectionPath();

    /**
     * Returns User Authentication API URL.
     *
     * @return {@link String}
     */
    public String getAuthenticationApiUrl();

    /**
     * Returns array of domain patterns that are not allowed as permissible domains for external user.
     *
     * @return {@link String[]}
     */
    public String[] getBlacklistedDomains();

    /**
     * returns {@link ClientResponse} after calling Edit Territory Language User Preferences Request API.
     *
     * @param editTerritoryLanguagePreferencesRequest
     * @param token
     *            {@link String} to authenticate request
     * @return {@link ClientResponse}
     */
    public <T extends UserRegRequest> ClientResponse editTerritoryLanguagePreferences(
            final T editTerritoryLanguagePreferencesRequest, final String token);

    /**
     * returns {@link ClientResponse} after calling Edit Content User Preferences Request API.
     *
     * @param editContentPreferencesRequest
     * @param token
     *            {@link String} to authenticate request
     * @return {@link ClientResponse}
     */
    public <T extends UserRegRequest> ClientResponse editContentPreferences(final T editContentPreferencesRequest,
            final String token);

    /**
     * returns {@link ClientResponse} after calling Accept Terms and Conditions Request API.
     *
     * @param acceptTncRequest
     * @param token
     *            {@link String} to authenticate request
     * @return {@link ClientResponse}
     */
    public <T extends UserRegRequest> ClientResponse acceptTnc(final T acceptTncRequest, final String token);

    /**
     * Returns {@link ClientResponse} after calling Add Recently Viewed Request API.
     *
     * @param recentViewRequest
     * @param token
     *            {@link String} to authenticate request
     * @return {@link ClientResponse}
     */
    public <T extends UserRegRequest> ClientResponse addRecentlyViewed(final T recentViewRequest, final String token);

    /**
     * Returns {@link ClientResponse} after calling Get Recently Viewed Item Request API.
     *
     * @param token
     *            {@link String} to authenticate request
     * @return {@link ClientResponse}
     */
    public <T extends UserRegRequest> ClientResponse getRecentlyViewed(final String token);

    /**
     * Returns {@link ClientResponse} after calling Remove Recently Viewed Item Request API.
     * 
     * @param recentViewRequest
     *            {@link RecentViewRequest}
     * 
     * @return {@link ClientResponse}
     */
    public <T extends UserRegRequest> ClientResponse insertRecentlyViewedItemInTempTable(final T recentViewRequest,
            String token);

    /**
     * Returns {@link ClientResponse} after calling Remove Recently Viewed Item Request API.
     * 
     * @param recentViewRequest
     *            {@link RecentViewRequest}
     * 
     * @return {@link ClientResponse}
     */
    public <T extends UserRegRequest> ClientResponse removeRecentlyViewedItemFromTempTable(final T recentViewRequest,
            String token);

    /**
     * Returns {@link ClientResponse} after calling Validate Token Request API.
     *
     * @param token
     *            {@link String} to authenticate request
     * @return {@link ClientResponse}
     */
    public ClientResponse validateToken(final String token);

    /**
     * 
     * @return {@link String} url of error page
     */
    public String getServerErrorPageUrl();

    /**
     * returns Viewpoint authentication cookie expiry time in hours for internal user.
     *
     * @return {@link int}
     */
    public int getInternalUserCookieExpiryHours();

    /**
     * Returns {@link ClientResponse} after calling Update Login Request API.
     *
     * @return {@link ClientResponse}
     */
    public ClientResponse updateLoginHistory(String token);

    /**
     * returns {@link ClientResponse} after calling UMS Add Favorite List API.
     *
     * @param addFavoriteList
     * @param token
     *            {@link String}
     * @return {@link ClientResponse}
     */
    public <T extends UserRegRequest> ClientResponse addFavoriteList(final T addFavoriteList, final String token);

    /**
     * returns {@link ClientResponse} after calling UMS Delete Favorite List API.
     *
     * @param deleteFavoriteList
     * @param token
     *            {@link String}
     * @return {@link ClientResponse}
     */
    public <T extends UserRegRequest> ClientResponse deleteFavoriteList(final T deleteFavoriteList, final String token);

    /**
     * returns {@link ClientResponse} after calling UMS Favorite All List API.
     *
     * @param token
     *            {@link String}
     * @return {@link ClientResponse}
     */
    public ClientResponse getFavoriteList(final String token);

    /**
     * returns {@link ClientResponse} after calling UMS Content Access Information API.
     *
     * @param token
     *            {@link String}
     * @return {@link ClientResponse}
     */
    public ClientResponse getUserContentAccessInformation(final String token);

    /**
     * returns {@link ClientResponse} after calling UMS Extend Concurrent License Session Timeout API.
     *
     * @param token
     *            {@link String}
     * @return {@link ClientResponse}
     */
    public ClientResponse getExtendConcurrentLicenseSessionTimeout(final String token);

    /**
     * returns {@link ClientResponse} after calling VP Redirection Get Redirect Path for Old Path API.
     *
     * @param path
     *            {@link String}
     * @return {@link ClientResponse}
     */
    public ClientResponse getRedirectPath(final String path);

    /**
     * returns {@link ClientResponse} after calling VP Redirection Add Redirect Paths API.
     *
     * @param addRedirectionsRequest
     *            {@link AddRedirectionsRequest}
     * @param token
     *            {@link String}
     * @return {@link ClientResponse}
     */
    public <T extends UserRegRequest> ClientResponse addRedirects(final T addRedirectionsRequest, final String token);
    
    /**
     * Returns Secret key for UMS services and AEM encryption/decryption.
     * @return {@link String}
     */
    public String getfdKey();

}
