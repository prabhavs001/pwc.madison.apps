package com.pwc.madison.core.userreg.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.Constants;
import com.pwc.madison.core.userreg.models.ContentAccessInfo;
import com.pwc.madison.core.userreg.models.License;
import com.pwc.madison.core.userreg.models.User;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.userreg.models.response.ContentAccessInfoResponse;
import com.pwc.madison.core.userreg.models.response.GetUserResponse;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.util.LocaleUtils;
import com.sun.jersey.api.client.ClientResponse;

/**
 * The UserInformationUtil is used to get the user information like whether user is logged in or not or user's profile
 * information from cookie. It also provides method to update or create user profile cookie.
 */
public final class UserInformationUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserInformationUtil.class);

    private static final String ACCOUNT_TYPE_INTERNAL = "internal";
    private static final String ACCOUNT_TYPE_EXTERNAL_REGULAR = "external-regular";
    private static final String ACCOUNT_TYPE_EXTERNAL_LICENSED = "external-licensed";
    private static final int USER_INFO_COOKIE_LIMIT = 3000;

    /**
     * Gets the {@link User} from the given {@link SlingHttpServletRequest}.
     * 
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param userRegRestService
     *            {@link UserRegRestService} required to validate token in cookie
     * @param cryptoSupport
     *            {@link CryptoSupport}
     * @param response
     *            {@link SlingHttpServletResponse}
     * @param fallback
     *            {@link boolean} true if fallback is required to get {@link UserProfile} from rest API
     * @param countryTerritoryMapperService
     *            {@link CountryTerritoryMapperService} only required in case fallback is true otherwise can be null
     * @param userPreferencesProviderService
     *            {@link UserPreferencesProviderService} only required in case fallback is true or requirePreferenceTags
     *            is true otherwise can be null
     * @param requirePreferenceTags
     *            {@link Boolean} true if the preferences tags are required from user profile otherwise false. The check
     *            is present to save computation to add preferences tags to user profile if its not required.
     * @param saveLoginHistory
     *            {@link Boolean} true if login time needs to be updated in profile
     * @param xssAPI
     *            {@link XSSAPI} required for secure logging
     * @return {@link User}
     */
    public static User getUser(final SlingHttpServletRequest request, final boolean performTokenValidation,
            final UserRegRestService userRegRestService, final CryptoSupport cryptoSupport,
            final SlingHttpServletResponse response, final boolean fallback,
            final CountryTerritoryMapperService countryTerritoryMapperService,
            final UserPreferencesProviderService userPreferencesProviderService, final boolean requirePreferenceTags,
            final boolean saveLoginHistory, final XSSAPI xssAPI) {
        final boolean isUserLoggedIn = UserInformationUtil.isUserLoggedIn(request, userRegRestService, response,
                performTokenValidation);
        UserProfile userProfile = null;
        if (isUserLoggedIn) {
            userProfile = getUserProfile(request, cryptoSupport, fallback, userRegRestService,
                    countryTerritoryMapperService, response, userPreferencesProviderService, requirePreferenceTags,
                    false, xssAPI);
            if (saveLoginHistory) {
                updateLoginHistory(userProfile, userRegRestService, request, response, countryTerritoryMapperService,
                        userPreferencesProviderService, cryptoSupport, xssAPI);
            }
        }
        return new User(isUserLoggedIn, userProfile);
    }

    /**
     * Checks if is user is logged in or not.
     *
     * @param slingHttpServletRequest
     *            {@link SlingHttpServletRequest}
     * @param userRegRestService
     *            {@link UserRegRestService} required to validate token in cookie
     * @param slingHttpServletResponse
     *            {@link SlingHttpServletResponse
     * @return true, if is user logged-in
     */
    public static boolean isUserLoggedIn(final SlingHttpServletRequest slingHttpServletRequest,
            final UserRegRestService userRegRestService, final SlingHttpServletResponse slingHttpServletResponse,
            final boolean performTokenValidation) {
        final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(slingHttpServletRequest);
        if (null != madisonCookie) {
            if (performTokenValidation) {
                final ClientResponse clientResponse = userRegRestService.validateToken(madisonCookie.getValue());
                if (clientResponse.getStatus() == SlingHttpServletResponse.SC_OK) {
                    return true;
                } else {
                    UserRegUtil.removeUserRegMadisonCookie(slingHttpServletRequest, slingHttpServletResponse);
                }
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@link UserProfile} from {@link SlingHttpServletRequest} present in
     * {@value Constants#MADISON_USER_PROFILE_COOKIE_NAME} cookie.
     * 
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param cryptoSupport
     *            {@link SlingHttpServletResponse}
     * @param fallback
     *            {@link boolean} true if fallback is required to get {@link UserProfile} from rest API
     * @param userRegRestService
     *            {@link UserRegRestService} only required in case fallback is true otherwise can be null
     * @param countryTerritoryMapperService
     *            {@link CountryTerritoryMapperService} only required in case fallback is true otherwise can be null
     * @param response
     *            {@link SlingHttpServletResponse} only required in case fallback is true otherwise can be null
     * @param userPreferencesProviderService
     *            {@link UserPreferencesProviderService} only required in case fallback is true or requirePreferenceTags
     *            is true otherwise can be null
     * @param requirePreferenceTags
     *            {@link Boolean} true if the preferences tags are required from user profile otherwise false. The check
     *            is present to save computation to add preferences tags to user profile if its not required.
     * @param saveCookie
     *            {@link Boolean true if cookie needs to be created if not exist
     * @param xssAPI
     *            {@link XSSAPI} required for secure logging
     * @return {@link UserProfile}
     */
    public static UserProfile getUserProfile(final SlingHttpServletRequest request, final CryptoSupport cryptoSupport,
            final boolean fallback, final UserRegRestService userRegRestService,
            final CountryTerritoryMapperService countryTerritoryMapperService, final SlingHttpServletResponse response,
            final UserPreferencesProviderService userPreferencesProviderService, final boolean requirePreferenceTags,
            final boolean saveCookie, final XSSAPI xssAPI) {
        final Cookie madisonUserCookie = UserInformationUtil.getMadisonUserProfileCookie(request);
        UserProfile userProfile = null;
        if (null != madisonUserCookie) {
            final String userProfileDecodedString = madisonUserCookie.getValue();
            try {
                String userProfileString = cryptoSupport.unprotect(userProfileDecodedString);
                userProfile = new Gson().fromJson(userProfileString, UserProfile.class);
            } catch (CryptoException cryptoException) {
                LOGGER.error(
                        "UserInformationUtil :  getUserFromRequest() : Crypto Exception occured while decrypting user profile {}",
                        cryptoException);
            }
        }
        if (fallback && null == userProfile) {
            final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
            if (null != madisonCookie) {
                final ClientResponse clientResponse = userRegRestService.getUser(madisonCookie.getValue());
                final String responseString = clientResponse.getEntity(String.class);
                if (clientResponse.getStatus() == HttpServletResponse.SC_OK) {
                    final GetUserResponse getUserResponse = new Gson().fromJson(responseString, GetUserResponse.class);
                    userProfile = getUserResponse.getData().getUserProfile();
                    if (saveCookie) {
                    	ContentAccessInfo contentAccessInfo = getMadisonUserContentAccessInfo(request, cryptoSupport);
                    	List<License> licenses = null != contentAccessInfo ? contentAccessInfo.getLicenses() : null;
                        boolean isCaiLicensesAvailable = null != licenses && !licenses.isEmpty();
                        UserInformationUtil.setMadisonUserProfileCookie(request, response,
                                getUserResponse.getData().getUserProfile(), countryTerritoryMapperService, 1,
                                cryptoSupport, userPreferencesProviderService, null, xssAPI, isCaiLicensesAvailable, contentAccessInfo);
                    }
                }
            }
        }
        if (null != userProfile && requirePreferenceTags) {
            userProfile.setIndustryTags(userPreferencesProviderService
                    .getTagsByPath(UserRegUtil.getFullPathPreferences(userProfile.getPreferredIndustry())));
            userProfile.setTopicTags(userPreferencesProviderService
                    .getTagsByPath(UserRegUtil.getFullPathPreferences(userProfile.getPreferredTopic())));
            userProfile.setTitleTags(userPreferencesProviderService.getTagsByPath(userProfile.getTitle()));
        }
        if (null != userProfile) {
            userProfile
                    .setContentAccessInfo(UserInformationUtil.getMadisonUserContentAccessInfo(request, cryptoSupport));
        }
        return userProfile;
    }

    /**
     * Gets the {@value Constants#MADISON_USER_PROFILE_COOKIE_NAME} {@link Cookie}. Returns null if no cookie is found
     * or httpRequest is null.
     * 
     * @param request
     *            {@link SlingHttpServletRequest}
     * @return {@link Cookie}
     */
    public static Cookie getMadisonUserProfileCookie(final SlingHttpServletRequest request) {
        return UserRegUtil.getCookieByName(request, Constants.MADISON_USER_PROFILE_COOKIE_NAME);
    }

    /**
     * Set the {@value Constants#MADISON_USER_PROFILE_COOKIE_NAME} cookie with given {@link UserProfile} for given
     * cookie expiry hours.
     *
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param response
     *            {@link SlingHttpServletResponse}
     * @param userProfile
     *            {@link UserProfile}
     * @param countryTerritoryMapperService
     *            {@link CountryTerritoryMapperService}
     * @param cookieExpiryHours
     *            {@link int}
     * @param cryptoSupport
     *            {@link CryptoSupport}
     * @param userPreferencesProviderService
     *            {@link UserPreferencesProviderService}
     * @param xssAPI
     *            {@link XSSAPI} required for secure logging
     */
    public static void setMadisonUserProfileCookie(final SlingHttpServletRequest request,
            final SlingHttpServletResponse response, final UserProfile userProfile,
            final CountryTerritoryMapperService countryTerritoryMapperService, final int cookieExpiryHours,
            final CryptoSupport cryptoSupport, final UserPreferencesProviderService userPreferencesProviderService,
            final LocalDate lastLoginDate, final XSSAPI xssAPI, boolean isCaiLicensesAvailable, final ContentAccessInfo contentAccessInfo) {
        final UserProfile cookieUserProfile = new UserProfile();
        final String countryCode = userProfile.getCountry();
        final String locale = countryTerritoryMapperService.getLocaleFromCountry(countryCode);
        final String territoryCode = LocaleUtils.getTerritoryFromLocale(locale);
        cookieUserProfile.setCountry(countryCode);
        cookieUserProfile.setTerritoryCode(territoryCode);
        cookieUserProfile.setLanguageCode(LocaleUtils.getLanguageFromLocale(locale));
        cookieUserProfile.setUserProfileCookieExpiry(
                System.currentTimeMillis() + cookieExpiryHours * MadisonConstants.HOURS_TO_MILLIS);
        cookieUserProfile.setIsInternalUser(userProfile.getIsInternalUser());
        cookieUserProfile.setEmail(userProfile.getEmail());
        cookieUserProfile.setAcceptedTerritoryCodeAndVersion(userProfile.getAcceptedTerritoryCodeAndVersion());
        cookieUserProfile.setLastLoginDate(lastLoginDate);
        cookieUserProfile.setId(userProfile.getId());
        cookieUserProfile
                .setPreferredIndustry(UserRegUtil.getShortenedPathPreferences(userProfile.getPreferredIndustry()));cookieUserProfile.setPreferredTopic(UserRegUtil.getShortenedPathPreferences(userProfile.getPreferredTopic()));
        cookieUserProfile.setTitle(userProfile.getTitle());
        String encodedProfile;
        try {
            encodedProfile = cryptoSupport.protect(new Gson().toJson(cookieUserProfile));
            boolean isHttps = request.getProtocol().contains(Constants.HTTPS);
            UserRegUtil.setCookie(response, Constants.MADISON_USER_PROFILE_COOKIE_NAME, encodedProfile,
                    cookieExpiryHours, isHttps, true, null);
            UserRegUtil.setCookie(response, Constants.MADISON_USER_PROFILE_CLIENT_COOKIE_NAME, "1", cookieExpiryHours,
                    isHttps, false, null);
            LOGGER.debug("UserInformationUtil : setMadisonUserProfileCookie() : Madison Profile cookie, {}",
                    encodedProfile);
        } catch (CryptoException cryptoException) {
            LOGGER.error(
                    "UserInformationUtil :  setMadisonUserProfileCookie() : Crypto Exception occured while encrypting user profile {}",
                    cryptoException);
        }
        setMadisonUserInfoCookie(request, response, userProfile, cookieExpiryHours, userPreferencesProviderService,
                xssAPI,countryTerritoryMapperService, isCaiLicensesAvailable, contentAccessInfo);
    }

    /**
     * Set the {@value Constants#MADISON_USER_PROFILE_USERINFO_COOKIE_NAME} cookie with given {@link UserProfile} for
     * given cookie expiry hours. This cookie is used by data analytics and also contain some information frequently
     * required at front-end side.
     * 
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param response
     *            {@link SlingHttpServletResponse}
     * @param userProfile
     *            {@link UserProfile}
     * @param cookieExpiryHours
     *            {@link int}
     * @param userPreferencesProviderService
     *            {@link UserPreferencesProviderService}
     * @param xssAPI
     *            {@link XSSAPI} required for secure logging
     * @param isCaiLicensesAvailable 
     */
    public static void setMadisonUserInfoCookie(final SlingHttpServletRequest request,
            final SlingHttpServletResponse response, final UserProfile userProfile, final int cookieExpiryHours,
            final UserPreferencesProviderService userPreferencesProviderService, final XSSAPI xssAPI,final CountryTerritoryMapperService countryTerritoryMapperService, boolean isCaiLicensesAvailable, final ContentAccessInfo contentAccessInfo) {
        final UserProfile cookieUserProfile = new UserProfile();
        cookieUserProfile.setPrimaryTerritory(userProfile.getPrimaryTerritory());
        cookieUserProfile.setPrimaryLanguage(userProfile.getPrimaryLanguage());
        try {
            cookieUserProfile.setFirstName(
                    Base64.getEncoder().encodeToString(userProfile.getFirstName().getBytes(MadisonConstants.UTF_8)));
        } catch (UnsupportedEncodingException unsupportedEncodingException) {
            LOGGER.error(
                    "UserInformationUtil setMadisonUserInfoCookie() : UnsupportedEncodingException Occured {} while encoding value {}",
                    unsupportedEncodingException, xssAPI.encodeForHTML(userProfile.getFirstName()));
        }
        cookieUserProfile.setIsInternalUser(userProfile.getIsInternalUser());
        cookieUserProfile.setGaapTags(userPreferencesProviderService.getTagsByPath(userProfile.getPreferredGaap()));
        cookieUserProfile.setGaasTags(userPreferencesProviderService.getTagsByPath(userProfile.getPreferredGaas()));
        cookieUserProfile.setPreferredTerritories(userProfile.getPreferredTerritories());
        cookieUserProfile.setPreferredLanguages(userProfile.getPreferredLanguages());
        cookieUserProfile.setCompany(userProfile.getCompany());
        cookieUserProfile.setCountry(countryTerritoryMapperService.getSelectedCountryNameByDefaultLocale(request,userProfile.getCountry()));
        cookieUserProfile.setIndustryTags(userPreferencesProviderService.getTagsByPath(userProfile.getPreferredIndustry()));
        cookieUserProfile.setTopicTags(userPreferencesProviderService.getTagsByPath(userProfile.getPreferredTopic()));
        cookieUserProfile.setTitleTags(userPreferencesProviderService.getTagsByPath(userProfile.getTitle()));
        cookieUserProfile.setFunctionalRoleTitle(userPreferencesProviderService.getTitleByPath(userProfile.getTitle()));
        cookieUserProfile.setIndustryTitles(userPreferencesProviderService.getTitlesByPath(userProfile.getPreferredIndustry()));
        cookieUserProfile.setUserAccountType(getUserAccountType(userProfile, isCaiLicensesAvailable));
        cookieUserProfile.setPwcId(SecurityUtils.encode(userProfile.getEmail().toString()));
        cookieUserProfile.setPwcId(cookieUserProfile.getPwcId().toLowerCase());
        cookieUserProfile.setCountryCode(userProfile.getCountry());
        cookieUserProfile.setContentAccessInfo(contentAccessInfo);
        String userProfileString = new Gson().toJson(cookieUserProfile);
        setMadisonUserInfoCookie(request, response, cookieExpiryHours, userProfileString);
        LOGGER.debug("UserInformationUtil : setMadisonUserInfoCookie() : Madison User Info cookie, {}",
                userProfileString);
    }

    /**
     * Set the {@value Constants#MADISON_USER_PROFILE_USERINFO_COOKIE_NAME} cookie with given value of userProfileString
     * for given cookie expiry hours. This cookie is used by data analytics and also contain some information frequently
     * required at front-end side.
     * 
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param response
     *            {@link SlingHttpServletResponse}
     * @param cookieExpiryHours
     *            {@link int}
     * @param userProfileString
     *            {@link String}
     */
    private static void setMadisonUserInfoCookie(final SlingHttpServletRequest request,
            final SlingHttpServletResponse response, final int cookieExpiryHours, String userProfileString) {
        UserRegUtil.removeUserRegUserInfoCookie(request, response);
        Iterable<String> userInfoPartsIterable = Splitter.fixedLength(USER_INFO_COOKIE_LIMIT).split(userProfileString);
        String[] userInfoParts = Iterables.toArray(userInfoPartsIterable, String.class);
        String userInfoCookieValue = StringUtils.EMPTY;
        boolean isHttps = request.getProtocol().contains(Constants.HTTPS);
        for (int index = 0; index < userInfoParts.length; index++) {
            try {
                userInfoParts[index] = URLEncoder.encode(userInfoParts[index], MadisonConstants.UTF_8);
            } catch (UnsupportedEncodingException unsupportedEncodingException) {
                LOGGER.error(
                        "UserInformationUtil setMadisonUserInfoCookie() : UnsupportedEncodingException Occured {} while encoding value {}",
                        unsupportedEncodingException, userInfoParts[index]);
            }
            UserRegUtil.setCookie(response, Constants.MADISON_USER_PROFILE_USERINFO_COOKIE_NAME_PREFIX + index,
                    userInfoParts[index], cookieExpiryHours, isHttps, false, null);
            userInfoCookieValue = userInfoCookieValue
                    + (index != 0 ? Constants.MADISON_USER_PROFILE_USERINFO_COOKIE_SEPERATOR : StringUtils.EMPTY)
                    + index;
        }
        UserRegUtil.setCookie(response, Constants.MADISON_USER_PROFILE_USERINFO_COOKIE_NAME, userInfoCookieValue,
                cookieExpiryHours, isHttps, false, null);
    }

    /**
     * Return Account type of the user required by data analytics.
     * 
     * @param userProfile
     *            {@link UserProfile}
     * @param isCaiLicensesAvailable 
     * @return {@link String}
     */
    private static String getUserAccountType(final UserProfile userProfile, boolean isCaiLicensesAvailable) {
        if (userProfile.getIsInternalUser()) {
            return ACCOUNT_TYPE_INTERNAL;
        } else {
            return (null != userProfile.getContentAccessInfo()
                    && null != userProfile.getContentAccessInfo().getLicenses()
                    && userProfile.getContentAccessInfo().getLicenses().size() > 0) || isCaiLicensesAvailable ? ACCOUNT_TYPE_EXTERNAL_LICENSED
                            : ACCOUNT_TYPE_EXTERNAL_REGULAR;
        }
    }

    /**
     * Update the {@value Constants#MADISON_USER_PROFILE_COOKIE_NAME} cookie with updated {@link UserProfile}.
     * 
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param response
     *            {@link SlingHttpServletResponse}
     * @param userProfile
     *            {@link UserProfile}
     * @param countryTerritoryMapperService
     *            {@link CountryTerritoryMapperService}
     * @param cryptoSupport
     *            {@link CryptoSupport}
     * @param userPreferencesProviderService
     *            {@link UserPreferencesProviderService}
     * @param xssAPI
     *            {@link XSSAPI} required for secure logging
     */
    public static void updateMadisonUserProfileCookie(final SlingHttpServletRequest request,
            final SlingHttpServletResponse response, final UserProfile userProfile,
            final CountryTerritoryMapperService countryTerritoryMapperService, final CryptoSupport cryptoSupport,
            final UserPreferencesProviderService userPreferenceProviderService, final boolean updateLastLoginDate,
            final XSSAPI xssAPI) {
        final UserProfile userSavedProfile = UserInformationUtil.getUserProfile(request, cryptoSupport, false, null,
                null, null, null, false, false, xssAPI);
        final int cookieExpiryHours = userSavedProfile == null ? 1
                : UserRegUtil.getLinkExpiryHours(userSavedProfile.getUserProfileCookieExpiry());
        ContentAccessInfo contentAccessInfo = getMadisonUserContentAccessInfo(request, cryptoSupport);
    	List<License> licenses = null != contentAccessInfo ? contentAccessInfo.getLicenses() : null;
        boolean isCaiLicensesAvailable = null != licenses && !licenses.isEmpty();
        UserInformationUtil.setMadisonUserProfileCookie(request, response, userProfile, countryTerritoryMapperService,
                cookieExpiryHours, cryptoSupport, userPreferenceProviderService, updateLastLoginDate ? LocalDate.now()
                        : (userSavedProfile == null ? null : userSavedProfile.getLastLoginDate()),
                xssAPI, isCaiLicensesAvailable, contentAccessInfo);
    }

    /**
     * Update the last login date if the user last logged in earlier than today
     *
     * @param userProfile
     * @param userRegRestService
     * @param request
     * @param response
     * @param countryTerritoryMapperService
     * @param userPreferencesProviderService
     * @param cryptoSupport
     * @param xssAPI
     *            {@link XSSAPI} required for secure logging
     */
    public static void updateLoginHistory(final UserProfile userProfile, final UserRegRestService userRegRestService,
            final SlingHttpServletRequest request, final SlingHttpServletResponse response,
            final CountryTerritoryMapperService countryTerritoryMapperService,
            final UserPreferencesProviderService userPreferencesProviderService, final CryptoSupport cryptoSupport,
            final XSSAPI xssAPI) {
        final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
        if (madisonCookie != null && !LocalDate.now().equals(userProfile.getLastLoginDate())) {
            final ClientResponse clientResponse = userRegRestService.updateLoginHistory(madisonCookie.getValue());
            if (clientResponse.getStatus() == SlingHttpServletResponse.SC_OK) {
                final ClientResponse getProfileClientResponse = userRegRestService.getUser(madisonCookie.getValue());
                if (getProfileClientResponse.getStatus() == HttpServletResponse.SC_OK) {
                    final String responseString = getProfileClientResponse.getEntity(String.class);
                    final GetUserResponse getUserResponse = new Gson().fromJson(responseString, GetUserResponse.class);
                    updateMadisonUserProfileCookie(request, response, getUserResponse.getData().getUserProfile(),
                            countryTerritoryMapperService, cryptoSupport, userPreferencesProviderService, true, xssAPI);
                } else if (getProfileClientResponse.getStatus() == SlingHttpServletResponse.SC_UNAUTHORIZED) {
                    UserRegUtil.removeUserRegMadisonCookie(request, response);
                }
            } else if (clientResponse.getStatus() == SlingHttpServletResponse.SC_UNAUTHORIZED) {
                UserRegUtil.removeUserRegMadisonCookie(request, response);
            }
        }
    }

    /**
     * Set User's {@link ContentAccessInfoResponse} cookie with name
     * {@value Constants#MADISON_USER_CONTENT_ACCESS_INFO_COOKIE_NAME} with given expire time.
     * 
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param response
     *            {@link SlingHttpServletResponse}
     * @param contentAccessInfoResponse
     *            {@link ContentAccessInfoResponse}
     * @param cookieExpiryHours
     *            {@link int}
     * @param cryptoSupport
     *            {@link CryptoSupport}
     */
    public static void setMadisonUserContentAccessInfoCookie(final SlingHttpServletRequest request,
            final SlingHttpServletResponse response, final ContentAccessInfoResponse contentAccessInfoResponse,
            final int cookieExpiryHours, final CryptoSupport cryptoSupport) {
        if (contentAccessInfoResponse != null && contentAccessInfoResponse.getData() != null
                && contentAccessInfoResponse.getData().getContentAccessInfo() != null) {
            String encodedContentAccessInfoString;
            try {
                encodedContentAccessInfoString = cryptoSupport.protect(new Gson().toJson(contentAccessInfoResponse.getData().getContentAccessInfo()));
                boolean isHttps = request.getProtocol().contains(Constants.HTTPS);
                UserRegUtil.setCookie(response, Constants.MADISON_USER_CONTENT_ACCESS_INFO_COOKIE_NAME,
                        encodedContentAccessInfoString, cookieExpiryHours, isHttps, true, null);
                LOGGER.debug(
                        "UserInformationUtil : setMadisonUserContentAccessInfoCookie() : Madison User Content Access Info cookie, {}",
                        encodedContentAccessInfoString);
            } catch (CryptoException cryptoException) {
                LOGGER.error(
                        "UserInformationUtil :  setMadisonUserContentAccessInfoCookie() : Crypto Exception occured while encrypting user content access info {}",
                        cryptoException);
            }
        }
    }

    /**
     * Retrun User's {@link ContentAccessInfo} from cookie
     * {@value Constants#MADISON_USER_CONTENT_ACCESS_INFO_COOKIE_NAME}.
     * 
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param cryptoSupport
     *            {@link CryptoSupport}
     * @return {@link ContentAccessInfo}
     */
    public static ContentAccessInfo getMadisonUserContentAccessInfo(final SlingHttpServletRequest request,
            final CryptoSupport cryptoSupport) {
        final Cookie contentAccessInfoCookie = UserRegUtil.getCookieByName(request,
                Constants.MADISON_USER_CONTENT_ACCESS_INFO_COOKIE_NAME);
        ContentAccessInfo contentAccessInfo = new ContentAccessInfo();
        if (null != contentAccessInfoCookie) {
            final String userProfileDecodedString = contentAccessInfoCookie.getValue();
            try {
                String contentAccessInfoString = cryptoSupport.unprotect(userProfileDecodedString);
                contentAccessInfo = new Gson().fromJson(contentAccessInfoString, ContentAccessInfo.class);
            } catch (CryptoException cryptoException) {
                LOGGER.error(
                        "UserInformationUtil :  getMadisonUserContentAccessInfo() : Crypto Exception occured while decrypting user content access info {}",
                        cryptoException);
            }
        }
        return contentAccessInfo;
    }
    
	public static void setLicenseInfoCookie(SlingHttpServletRequest request, SlingHttpServletResponse response,
			int cookieExpiryHours, List<License> licenses) {
		String encodeLicensesString = null;
		if (licenses != null) {
			String availableLicenses = new Gson().toJson(licenses);
			LOGGER.info("availableLicenses {} ", availableLicenses);
			try {
				encodeLicensesString = URLEncoder.encode(availableLicenses, MadisonConstants.UTF_8);
			} catch (UnsupportedEncodingException e) {
				LOGGER.error("UserInformationUtil :  setLicenseInfo() : UnsupportedEncodingException Exception occured while encrypting licenses {}", e);
			}
		}
		boolean isHttps = request.getProtocol().contains(Constants.HTTPS);
		UserRegUtil.setCookie(response, Constants.MADISON_USER_LICENSE_INFO_COOKIE_NAME, encodeLicensesString,
				cookieExpiryHours, isHttps, false, null);
	}
}
