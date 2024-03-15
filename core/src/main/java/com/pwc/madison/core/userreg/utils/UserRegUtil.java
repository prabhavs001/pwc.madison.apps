package com.pwc.madison.core.userreg.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.pwc.madison.core.userreg.models.UserProfile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.xss.XSSAPI;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.Page;
import com.google.gson.Gson;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.Constants;
import com.pwc.madison.core.userreg.models.ContentAccessInfo;
import com.pwc.madison.core.userreg.models.License;
import com.pwc.madison.core.userreg.models.request.DeleteFavoriteListRequest;
import com.pwc.madison.core.userreg.models.response.ContentAccessInfoResponse;
import com.pwc.madison.core.userreg.models.response.FavoriteFolderResponse;
import com.pwc.madison.core.userreg.models.response.FavoriteFolderResponse.FavoriteList;
import com.pwc.madison.core.userreg.models.response.FavoriteListResponse;
import com.pwc.madison.core.userreg.models.response.GetUserResponse;
import com.pwc.madison.core.userreg.services.UserLicensesProviderService;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;
import com.sun.jersey.api.client.ClientResponse;

/**
 * The Utility provides methods commonly used in UserReg.
 */
public class UserRegUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRegUtil.class);
    public static final String US_TERRITORY_CODE = "us";
    public static final String US_COUNTRY_CODE = "US";

    /**
     * Gets the string from node path.
     *
     * @param nodePath
     *            {@link String}
     * @param session
     *            {@link Session} must have rights to read the node
     * @return {@link String}
     */
    public static String getStringFromNodePath(final String nodePath, final Session session) {
        try {
            if (nodePath != null) {
                final Node contentNode = session.getNode(nodePath);
                if (contentNode != null) {
                    final InputStream emailStream = contentNode.getProperty(JcrConstants.JCR_DATA).getBinary()
                            .getStream();
                    return IOUtils.toString(emailStream, Constants.UTF_8_ENCODING);
                }
            }
        } catch (final RepositoryException repositoryException) {
            LOGGER.error(
                    "UserRegUtil :  getStringFromNodePath() : Respository Exception occured while reading the node of path {}",
                    repositoryException);
        } catch (final IOException ioException) {
            LOGGER.error(
                    "UserRegUtil :  getStringFromNodePath() : IO Exception occured while reading the node of path {}",
                    ioException);
        }
        return null;
    }

    /**
     * Gets the {@value Constants#AUTH_COOKIE_NAME} {@link Cookie}. Returns null if no cookie is found or httpRequest is
     * null.
     *
     * @param httpRequest
     *            {@link HttpServletRequest}
     * @return {@link Cookie}
     */
    public static Cookie getUserRegMadisonCookie(final SlingHttpServletRequest httpRequest) {
        return UserRegUtil.getCookieByName(httpRequest, Constants.AUTH_COOKIE_NAME);
    }

    /**
     * Returns the object of Type T fetched from {@link SlingHttpServletRequest}}
     *
     * @param slingHttpServletRequest
     *            {@link SlingHttpServletRequest}
     * @param type
     *            {@link Class}
     * @return {@link Object} of given {@link Class}
     * @throws IOException
     *             {@link IOException}
     */
    public static <T> T getObjectFromRequest(final SlingHttpServletRequest slingHttpServletRequest, final Class<T> type)
            throws IOException {
        T object = null;
        if (slingHttpServletRequest != null) {
            try (final BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(slingHttpServletRequest.getInputStream(), Constants.UTF_8_ENCODING))) {
                String jsonString = StringUtils.EMPTY;
                if (bufferedReader != null) {
                    jsonString = bufferedReader.readLine();
                }
                object = new Gson().fromJson(jsonString, type);
            } catch (final IOException ioException) {
                LOGGER.error(
                        "UserRegUtil : getObjectFromRequest() : IO Exception occured while getting Object from request {}",
                        ioException);
            }
        }
        return object;
    }

    /**
     * Removes the {@value Constants#IMAGE_SELECTOR_COOKIE_NAME} ,
     * {@value Constants#MADISON_USER_PROFILE_USERINFO_COOKIE_NAME}, {@value Constants#MADISON_USER_PROFILE_COOKIE_NAME}
     * and {@value Constants#AUTH_COOKIE_NAME} from {@link SlingHttpServletResponse}.
     * 
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param response
     *            {@link SlingHttpServletResponse}
     */
    public static void removeUserRegMadisonCookie(final SlingHttpServletRequest request,
            final SlingHttpServletResponse response) {
        boolean isHttps = request.getProtocol().contains(Constants.HTTPS);
        setCookie(response, Constants.AUTH_COOKIE_NAME, null, 0, isHttps, true, null);
        setCookie(response, Constants.AUTH_CLIENT_COOKIE_NAME, null, 0, isHttps, false, null);
        setCookie(response, Constants.MADISON_USER_PROFILE_COOKIE_NAME, null, 0, isHttps, true, null);
        setCookie(response, Constants.MADISON_USER_PROFILE_CLIENT_COOKIE_NAME, null, 0, isHttps, false, null);
        removeUserRegUserInfoCookie(request, response);
        setCookie(response, Constants.IMAGE_SELECTOR_COOKIE_NAME, null, 0, isHttps, false, null);
        setCookie(response, Constants.MADISON_USER_CONTENT_ACCESS_INFO_COOKIE_NAME, null, 0, isHttps, true, null);
        setCookie(response, Constants.MADISON_USER_LICENSE_INFO_COOKIE_NAME, null, 0, isHttps, false, null);
        setCookie(response, Constants.MADISON_CONCURRENT_LICENSE_INFO_COOKIE, null, 0, isHttps, false, null);
        setCookie(response, Constants.MADISON_EXTEND_CONCURRENT_LICENSE_SESSION_INFO_COOKIE, null, 0, isHttps, false,
                null);
        setCookie(response, Constants.LOGIN_COMPLETE_COOKIE_NAME, null, 0, isHttps, false, null);
    	setCookie(response, Constants.COMPLETE_PROFILE_COOKIE_NAME, null, 0, isHttps, false, null);
    }

    /**
     * Removes all splitted {@value Constants#MADISON_USER_PROFILE_USERINFO_COOKIE_NAME} from
     * {@link SlingHttpServletResponse}.
     * 
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param response
     *            {@link SlingHttpServletResponse}
     */
    public static void removeUserRegUserInfoCookie(final SlingHttpServletRequest request,
            final SlingHttpServletResponse response) {
        final Cookie userInfoCookie = getCookieByName(request, Constants.MADISON_USER_PROFILE_USERINFO_COOKIE_NAME);
        if (null != userInfoCookie) {
            String userInfoCookieValue = userInfoCookie.getValue();
            String[] userInfoSplittedCookies = userInfoCookieValue
                    .split(Constants.MADISON_USER_PROFILE_USERINFO_COOKIE_SEPERATOR);
            boolean isHttps = request.getProtocol().contains(Constants.HTTPS);
            setCookie(response, Constants.MADISON_USER_PROFILE_USERINFO_COOKIE_NAME, null, 0, isHttps, false, null);
            for (String userInfoSplittedCookie : userInfoSplittedCookies) {
                setCookie(response, Constants.MADISON_USER_PROFILE_USERINFO_COOKIE_NAME_PREFIX + userInfoSplittedCookie,
                        null, 0, isHttps, false, null);
            }
        }
    }

    /**
     * Sets the {@value Constants#IMAGE_SELECTOR_COOKIE_NAME} {@link Cookie} to the given
     * {@link SlingHttpServletResponse}.
     *
     * @param response
     *            {@link SlingHttpServletResponse}
     * @param email
     *            {@link String} can be null if isDefault is true
     * @param isDefault
     *            {@link boolean} if the cookie value is to be set 'default'
     * @param cookieExpiryHours
     *            {@link int}
     * @param isSecure
     */
    public static void setUserRegImageSelectorCookie(final SlingHttpServletResponse response, final String email,
            final boolean isDefault, final int cookieExpiryHours, boolean isSecure) {
        if (null != response) {
            final String cookieValue = isDefault ? "default" : email + "." + System.currentTimeMillis();
            final Cookie userRegImageSelectorCookie = new Cookie(Constants.IMAGE_SELECTOR_COOKIE_NAME,
                    StringUtils.normalizeSpace(cookieValue));
            userRegImageSelectorCookie.setMaxAge(cookieExpiryHours * 60 * 60);
            userRegImageSelectorCookie.setPath("/");
            userRegImageSelectorCookie.setSecure(isSecure);
            response.addCookie(userRegImageSelectorCookie);
        }
    }

    /**
     * Allows to perform all necessary task required to log in the Viewpoint user like create auth cookie, create/update
     * user etc.
     *
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param response
     *            {@link SlingHttpServletResponse}
     * @param resourceResolverFactory
     *            {@link ResourceResolverFactory}
     * @param token
     *            {@link String}
     * @param userRegRestService
     *            {@link UserRegRestService}
     * @param countryTerritoryMapperService
     *            {@link CountryTerritoryMapperService}
     * @param cryptoSupport
     *            {@link CryptoSupport}
     * @param xssAPI
     *            {@link XSSAPI} required for secure logging
     * @param userLicensesProviderService
     *            {@link UserLicensesProviderService} required for licenses popup
     * @return true if a user is Logged in successfully on AEM otherwise false
     */
    public static boolean loginMadisonUser(final SlingHttpServletRequest request,
            final SlingHttpServletResponse response, final ResourceResolverFactory resourceResolverFactory,
            final String token, final UserRegRestService userRegRestService,
            final CountryTerritoryMapperService countryTerritoryMapperService, final CryptoSupport cryptoSupport,
            final UserPreferencesProviderService userPreferencesProviderService, final XSSAPI xssAPI,
            final UserLicensesProviderService userLicensesProviderService, final boolean setLoginCompleteCookie,
            final boolean setCompleteProfileCookie) {
        boolean isUserLoggedIn = false;
        if (null != response && null != resourceResolverFactory && null != token && null != userRegRestService) {
            final ClientResponse clientResponse = userRegRestService.getUser(token);
            final String responseString = clientResponse.getEntity(String.class);
            if (clientResponse.getStatus() == HttpServletResponse.SC_OK) {
                final GetUserResponse getUserResponse = new Gson().fromJson(responseString, GetUserResponse.class);
                if(getUserResponse.getData().getUserProfile().getCountry().equals(US_COUNTRY_CODE)) {
                    getUserResponse.getData().setUserProfile(updateUsersIndustry(getUserResponse.getData().getUserProfile()));
                }
                final ClientResponse contentAccessInfoClientResponse = userRegRestService
                        .getUserContentAccessInformation(token);
                final String contentAccessInfoResponseString = contentAccessInfoClientResponse.getEntity(String.class);
                final ContentAccessInfoResponse contentAccessInfoResponse = new Gson()
                        .fromJson(contentAccessInfoResponseString, ContentAccessInfoResponse.class);
                boolean isHttps = request.getProtocol().contains(Constants.HTTPS);
                int cookieExpiryHours;
                if (getUserResponse.getData().getUserProfile().getIsInternalUser()) {
                    cookieExpiryHours = userRegRestService.getInternalUserCookieExpiryHours();
                } else {
                    cookieExpiryHours = contentAccessInfoResponse.getData().getContentAccessInfo()
                            .isConcurrentLicensedUser()
                                    ? userRegRestService.getAuthenticationConcurrentLicenseCookieExpiryHours()
                                    : userRegRestService.getAuthenticationSingleSeatLicenseCookieExpiryHours();
                    if (contentAccessInfoResponse.getData().getContentAccessInfo().isConcurrentLicensedUser()) {
                        UserRegUtil.setConcurrentLicenseInfoCookie(contentAccessInfoResponse,
                                userRegRestService.getAuthenticationConcurrentLicenseCookieExpiryHours(),
                                userLicensesProviderService, response, isHttps);
                        UserRegUtil.setConcurrentLicenseExtendSessionInfoCookie(request, response,
                                userRegRestService.getAuthenticationConcurrentLicenseCookieExpiryHours(), isHttps);
                    }
                }
                UserRegUtil.setCookie(response, Constants.AUTH_COOKIE_NAME, token, cookieExpiryHours, isHttps, true,
                        null);
                UserRegUtil.setCookie(response, Constants.AUTH_CLIENT_COOKIE_NAME, "1", cookieExpiryHours, isHttps,
                        false, null);
                if (setLoginCompleteCookie) {
                    UserRegUtil.setCookie(response, Constants.LOGIN_COMPLETE_COOKIE_NAME, "1", cookieExpiryHours,
                            isHttps, false, null);
                }
                if (setCompleteProfileCookie) {
                    UserRegUtil.setCookie(response, Constants.COMPLETE_PROFILE_COOKIE_NAME, "1", cookieExpiryHours,
                            isHttps, false, null);
                }
                isUserLoggedIn = true;
                UserInformationUtil.setMadisonUserContentAccessInfoCookie(request, response, contentAccessInfoResponse,
                        cookieExpiryHours, cryptoSupport);
                List<License> licenses = null != contentAccessInfoResponse ? contentAccessInfoResponse.getData().getContentAccessInfo().getLicenses() : null;
                boolean isCaiLicensesAvailable = null != licenses && !licenses.isEmpty();
                UserInformationUtil.setMadisonUserProfileCookie(request, response,
                        getUserResponse.getData().getUserProfile(), countryTerritoryMapperService, cookieExpiryHours,
                        cryptoSupport, userPreferencesProviderService, LocalDate.now(), xssAPI, isCaiLicensesAvailable, contentAccessInfoResponse.getData().getContentAccessInfo());
                
				// For external users set licenses list in vp_li cookie
				if (!getUserResponse.getData().getUserProfile().getIsInternalUser()) {
					UserInformationUtil.setLicenseInfoCookie(request, response, cookieExpiryHours, licenses);
				}
            } else {
                LOGGER.debug("UserRegUtil loginMadisonUser() : Getting userReg Admin Resource Resolver Null");
            }
        }
        return isUserLoggedIn;
    }

    /**
     * Set the {@value Constants#MADISON_CONCURRENT_LICENSE_INFO_COOKIE} cookie.
     * 
     * @param contentAccessInfoResponse
     *            {@link ContentAccessInfoResponse} required to build cookie data
     * @param concurrentLicenseCookieExpiryHours
     *            {@link int} cookie expiry time
     * @param userLicensesProviderService
     *            {@link UserLicensesProviderService} to get title for licenses code
     * @param response
     *            {@link SlingHttpServletResponse}
     * @param isHttps
     *            {@link Boolean}
     */
    private static void setConcurrentLicenseInfoCookie(final ContentAccessInfoResponse contentAccessInfoResponse,
            final int concurrentLicenseCookieExpiryHours, final UserLicensesProviderService userLicensesProviderService,
            final SlingHttpServletResponse response, final boolean isHttps) {
        final JSONObject licensesData = new JSONObject();
        final Map<String, String> licenseCodeToTitleMap = userLicensesProviderService.getLicenseCodeToTitleMap();
        try {
            for (final String license : contentAccessInfoResponse.getData().getContentAccessInfo()
                    .getUnavailableLicenses()) {
                licensesData.put(licenseCodeToTitleMap.get(license), false);
            }
            for (final License license : contentAccessInfoResponse.getData().getContentAccessInfo().getLicenses()) {
                licensesData.put(licenseCodeToTitleMap.get(license.getCode()), true);
            }
        } catch (JSONException jsonException) {
            LOGGER.error(
                    "UserRegUtil setConcurrentLicenseInfoCookie() : Error while setting concurrent user license info : {}",
                    jsonException);
        }
        try {
            if (licensesData.length() > 0) {
                String licensesDataString = Base64.getEncoder()
                        .encodeToString(licensesData.toString().getBytes(MadisonConstants.UTF_8));
                UserRegUtil.setCookie(response, Constants.MADISON_CONCURRENT_LICENSE_INFO_COOKIE, licensesDataString,
                        concurrentLicenseCookieExpiryHours, isHttps, false, null);
            }
        } catch (UnsupportedEncodingException unsupportedEncodingException) {
            LOGGER.error(
                    "UserInformationUtil setConcurrentLicenseInfoCookie() : UnsupportedEncodingException Occured {} while encoding value {}",
                    unsupportedEncodingException, licensesData.toString());
        }
    }

    /**
     * Returns the link expiry time in hours with respect to current time for the given linkExpiryTimeStamp.
     *
     * @param linkExpiryTimeStamp
     *            {@link long}
     * @return {@link int}
     */
    public static int getLinkExpiryHours(final long linkExpiryTimeStamp) {
        final long timeDifference = linkExpiryTimeStamp - System.currentTimeMillis();
        return (int) Math.ceil((double) timeDifference / (1000 * 60 * 60));
    }

    /**
     * Returns {@link Cookie} for given cookie name from {@link SlingHttpServletRequest}.
     *
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param cookieName
     *            {@link String}
     * @return {@link Cookie} returns null if no cookie is present
     */
    public static Cookie getCookieByName(final SlingHttpServletRequest request, final String cookieName) {
        if (request != null) {
            final Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (final Cookie cookie : cookies) {
                    if (cookie.getName().equalsIgnoreCase(cookieName)) {
                        return cookie;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Sets the {@link Cookie} to the given {@link SlingHttpServletResponse} with given cookie name and value for the
     * given cookie expiry hours.
     *
     * @param response          {@link HttpServletResponse}
     * @param cookieName        {@link String}
     * @param cookieValue       {@link String}
     * @param cookieExpiryHours {@link int}
     * @param isSecure
     * @param isHttpOnly
     * @param refererPage
     */
    public static void setCookie(final SlingHttpServletResponse response, final String cookieName, String cookieValue,
            final int cookieExpiryHours, boolean isSecure, boolean isHttpOnly, String cookiePath) {
        cookiePath = StringUtils.isNotEmpty(cookiePath) ? cookiePath : "/";
        final Cookie userRegMadisonCookie = new Cookie(cookieName, StringUtils.normalizeSpace(cookieValue));
        userRegMadisonCookie.setMaxAge(cookieExpiryHours * 60 * 60);
        userRegMadisonCookie.setPath(cookiePath);
        userRegMadisonCookie.setSecure(isSecure);
        userRegMadisonCookie.setHttpOnly(isHttpOnly);
        response.addCookie(userRegMadisonCookie);
    }

    /**
     * Returns the list of preferences path after converting each path to a shortened preference path.
     * 
     * @param preferencesList
     *            {@link List} list of preferences path
     * @return {@link List} list of shortened preferences path
     */
    public static List<String> getShortenedPathPreferences(final List<String> preferencesList) {
        if (null != preferencesList && !preferencesList.isEmpty()) {
            final List<String> shortenedPathPrefernces = new ArrayList<String>();
            for (final String preference : preferencesList) {
                shortenedPathPrefernces
                        .add(preference.replace(Constants.MADISON_PREFERENCES_BASE_PATH, StringUtils.EMPTY));
            }
            return shortenedPathPrefernces;
        }
        return preferencesList;
    }

    /**
     * Returns the list of preferences path after converting each path to a full preference path.
     * 
     * @param preferencesList
     *            {@link List} list of preferences path
     * @return {@link List} list of full preferences path
     */
    public static List<String> getFullPathPreferences(final List<String> preferencesList) {
        if (null != preferencesList && !preferencesList.isEmpty()) {
            final List<String> fullPathPrefernces = new ArrayList<String>();
            for (final String preference : preferencesList) {
                fullPathPrefernces.add(preference.indexOf(Constants.MADISON_PREFERENCES_BASE_PATH) == -1
                        ? Constants.MADISON_PREFERENCES_BASE_PATH + preference
                        : StringUtils.EMPTY);
            }
            return fullPathPrefernces;
        }
        return preferencesList;
    }

    /**
     * Update the Favorite list response with favorite list information and returns as {@link String}.
     * 
     * @param favoriteFoldersString
     *            {@link String} the response string must be JSON representation of {@link Map} where key is
     *            {@link String} and value is {@link FavoriteFolderResponse}.
     * @param resourceResolver
     *            {@link ResourceResolver}
     * @param updatePagesData
     *            {@link Boolean} true if full information is to be updated for all the folders or just title for the
     *            first given favoritePanelListCount items of {@value Constants#FAVORITE_LIST_DEFAULT_IDENTIFIER} folder
     * @param currentPageTerritoryCode
     *            {@link String} current page territory code will be passed from get all and delete favorite list
     *            Servlet. Required for date formatting
     * @param favoritePanelListCount
     *            {@link Integer} number of items in {@value Constants#FAVORITE_LIST_DEFAULT_IDENTIFIER} that needs to
     *            be updated with title
     * @param userToken
     *            {@link String}
     * @param userRegRestService
     *            {@link UserRegRestService}
     * @return {@link String}
     */
    public static String updateFavoriteListFolderResponse(final String favoriteFoldersString,
            final SlingHttpServletRequest request, final boolean updatePagesData, final String currentPageTerritoryCode,
            int favoritePanelListCount, final String userToken, final UserRegRestService userRegRestService,
            final CountryTerritoryMapperService countryTerritoryMapperService) {
        final FavoriteListResponse favoriteListResponse = new Gson().fromJson(favoriteFoldersString,
                FavoriteListResponse.class);
        final Map<String, FavoriteFolderResponse> favoriteFolders = favoriteListResponse.getData().getFavoriteFolders();
        if (updatePagesData) {
            final List<String> nonExistingPages = new ArrayList<String>();
            favoriteFolders.forEach((favoriteFolderKey, favoriteFolderResponse) -> {
                Iterator<FavoriteList> iterator = favoriteFolderResponse.getList().iterator();
                while (iterator.hasNext()) {
                    FavoriteList favoriteList = iterator.next();
                    if (!updateFavoriteListData(favoriteList, request, true, currentPageTerritoryCode,
                            countryTerritoryMapperService)) {
                        nonExistingPages.add(favoriteList.getPagePath());
                        iterator.remove();
                    }
                    if (!nonExistingPages.isEmpty()) {
                        handleNonExistingFavoriteList(nonExistingPages, userToken, userRegRestService);
                    }
                }
            });
        } else if (favoriteFolders.containsKey(Constants.FAVORITE_LIST_DEFAULT_IDENTIFIER)) {
            Iterator<FavoriteList> iterator = favoriteFolders.get(Constants.FAVORITE_LIST_DEFAULT_IDENTIFIER).getList()
                    .iterator();
            while (iterator.hasNext() && favoritePanelListCount != 0) {
                if (!updateFavoriteListData(iterator.next(), request, false, currentPageTerritoryCode,
                        countryTerritoryMapperService)) {
                    iterator.remove();
                } else {
                    favoritePanelListCount--;
                }
            }
        }
        return new Gson().toJson(favoriteListResponse);
    }

    /**
     * Update the page data in the given {@link FavoriteList} such as title, description, content string, published date
     * etc.
     * 
     * @param favoriteList
     *            {@link FavoriteList} in which data needs to be set
     * @param request
     *            {@link SlingHttpServletRequest} required to get {@link Resource}/{@link ResourceBundle} to get the
     *            required information
     * @param updateAllData
     *            specifies if title needs to be updated or entire data
     * @param currentPageTerritoryCode
     *            specifies territory code of the requested page
     * @param countryTerritoryMapperService
     *            required for fetching date format
     * @return {@link Boolean} true if favorite list exists as {@link Page} otherwise false
     */
    private static boolean updateFavoriteListData(final FavoriteList favoriteList,
            final SlingHttpServletRequest request, final boolean updateAllData, final String currentPageTerritoryCode,
            final CountryTerritoryMapperService countryTerritoryMapperService) {
        boolean exists = false;
        Resource favoriteListResource = request.getResourceResolver().getResource(favoriteList.getPagePath());
        if (null != favoriteListResource) {
            Locale locale = new Locale(MadisonUtil.getLocaleForPath(favoriteList.getPagePath()));
            ResourceBundle resourceBundle = request.getResourceBundle(locale);
            I18n i18n = new I18n(resourceBundle);
            Page favoriteListPage = favoriteListResource.adaptTo(Page.class);
            if (null != favoriteListPage) {
                Resource ghostPropertiesResource = favoriteListResource.getChild(MadisonConstants.GHOST_MODULE_PATH);
                exists = true;
                favoriteList.setTitle(ghostPropertiesResource != null
                        ? ghostPropertiesResource.getValueMap().get(MadisonConstants.TOPIC_LABEL,
                                ghostPropertiesResource.getValueMap().get(MadisonConstants.TOPIC_TEXT,
                                        StringUtils.EMPTY))
                        : favoriteListPage.getProperties().get(MadisonConstants.PWC_PAGE_TITLE,
                                favoriteListPage.getProperties().get(JcrConstants.JCR_TITLE, StringUtils.EMPTY)));
                favoriteList.setCountry(MadisonUtil.getTerritoryCodeForPath(favoriteListPage.getPath()).toUpperCase());
                favoriteList.setContent(i18n.get(MadisonUtil.getcontentFieldValue(favoriteListPage.getProperties())));
                if (updateAllData) {
                    final String dateFormat = MadisonUtil.fetchDateFormat(currentPageTerritoryCode,
                            countryTerritoryMapperService, MadisonConstants.COMPONENTS_DATE_FORMAT);
                    final boolean hidePublicationDate = DITAUtils
                            .isHidePublicationDate(favoriteListPage.getContentResource()).equals(MadisonConstants.YES)
                                    ? true
                                    : false;
                    favoriteList.setHidePublicationDate(hidePublicationDate);
                    favoriteList.setDescription(ghostPropertiesResource != null
                            ? ghostPropertiesResource.getValueMap()
                                    .get(MadisonConstants.TOPIC_TEXT, ghostPropertiesResource.getValueMap()
                                            .get(MadisonConstants.ABSTRACT_TEXT, StringUtils.EMPTY))
                            : favoriteListPage.getDescription());
                    favoriteList.setFavoritedDate(
                            dateFormatter(currentPageTerritoryCode, dateFormat, favoriteList.getFavoritedDate()));
                    if (!favoriteList.isHidePublicationDate()) {
                        favoriteList
                                .setPublicationDate(
                                        DITAUtils.formatDate(
                                                ghostPropertiesResource != null
                                                        ? ghostPropertiesResource.getValueMap().get(
                                                                MadisonConstants.GHOST_PUBLICATION_DATE, String.class)
                                                        : favoriteListPage.getProperties()
                                                                .get(DITAConstants.META_PUBLICATION_DATE, String.class),
                                                dateFormat));
                    }
                }
            }
        }
        return exists;
    }

    /**
     * Deletes Non existing pages that were favorited by user to whom the given userToken belongs.
     * 
     * @param nonExistingPages
     *            {@link List} of non existing pages
     * @param userToken
     *            {@link String}
     * @param userRegRestService
     *            {@link UserRegRestService}
     */
    private static void handleNonExistingFavoriteList(List<String> nonExistingPages, String userToken,
            UserRegRestService userRegRestService) {
        Runnable runnable = new Runnable() {
            public void run() {
                final DeleteFavoriteListRequest deleteFavoriteListRequest = new DeleteFavoriteListRequest(
                        nonExistingPages, false, null);
                userRegRestService.deleteFavoriteList(deleteFavoriteListRequest, userToken);
            }
        };
        Thread deleteFavoriteListThread = new Thread(runnable);
        deleteFavoriteListThread.start();
    }

    /**
     * Method used to format date if the content page is Japanese
     * 
     * @param territory
     * @param dateFormat
     * @param favoritedDate
     *
     */
    private static String dateFormatter(String territory, String dateFormat, String favoritedDate) {
        if (territory.equals(MadisonConstants.JP_TERRITORY_CODE)) {
            try {
                final SimpleDateFormat originalFormat = new SimpleDateFormat(MadisonConstants.COMPONENTS_DATE_FORMAT);
                final SimpleDateFormat parsedFormat = new SimpleDateFormat(dateFormat);
                final Date date = originalFormat.parse(favoritedDate);
                final String formattedDate = parsedFormat.format(date);
                return formattedDate;
            } catch (Exception e) {
                LOGGER.error("UserRegUtil dateFormatter() Exception occured in formatting Date", e);
            }
        }
        return favoritedDate;
    }

    /**
     * Allows to perform all necessary task required to extend the session of the Viewpoint user like updating all
     * cookie expiry time.
     *
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param response
     *            {@link SlingHttpServletResponse}
     * @param token
     *            {@link String}
     * @param userRegRestService
     *            {@link UserRegRestService}
     * @return true if a user is Logged in successfully on AEM otherwise false
     */
    public static boolean extendConcurrentMadisonUserSession(final SlingHttpServletRequest request,
            final SlingHttpServletResponse response, final String token, final UserRegRestService userRegRestService) {

        boolean isUserLoggedIn = false;
        if (null != response && null != token && null != userRegRestService) {
            boolean isHttps = request.getProtocol().contains(Constants.HTTPS);
            int cookieExpiryHours;
            cookieExpiryHours = userRegRestService.getExtendConcurrentLicenseSessionTimeoutInHours();
            UserRegUtil.setCookie(response, Constants.AUTH_COOKIE_NAME, token, cookieExpiryHours, isHttps, true, null);
            UserRegUtil.setConcurrentLicenseExtendSessionInfoCookie(request, response, cookieExpiryHours, isHttps);
            UserRegUtil.setCookie(response, Constants.AUTH_CLIENT_COOKIE_NAME, "1", cookieExpiryHours, isHttps, false,
                    null);
            isUserLoggedIn = true;
            UserRegUtil.setCookieExpiryTimeAtExtendSessionTimeout(request, response,
                    Constants.MADISON_USER_PROFILE_COOKIE_NAME, cookieExpiryHours);
            UserRegUtil.setCookie(response, Constants.MADISON_USER_PROFILE_CLIENT_COOKIE_NAME, "1", cookieExpiryHours,
                    isHttps, false, null);
            UserRegUtil.setCookieExpiryTimeAtExtendSessionTimeout(request, response,
                    Constants.MADISON_USER_CONTENT_ACCESS_INFO_COOKIE_NAME, cookieExpiryHours);
            UserRegUtil.setCookieExpiryTimeAtExtendSessionTimeout(request, response,
                    Constants.MADISON_USER_PROFILE_USERINFO_COOKIE_NAME, cookieExpiryHours);
        }

        return isUserLoggedIn;
    }

    /**
     * Set the {@value Constants#MADISON_EXTEND_CONCURRENT_LICENSE_SESSION_INFO_COOKIE} cookie
     *
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param response
     *            {@link SlingHttpServletResponse}
     * @param concurrentLicenseCookieExpiryInHours
     *            {@link Integer}
     * @param isHttps
     *            {@link boolean}
     */
    public static void setConcurrentLicenseExtendSessionInfoCookie(final SlingHttpServletRequest request,
            final SlingHttpServletResponse response, int concurrentLicenseCookieExpiryInHours, boolean isHttps) {
        JSONObject sessionData = new JSONObject();
        JSONObject decodedSessionData = null;
        // Initial session counter value is 1
        int sessionCounterValue = 1;
        try {
            final Cookie sessionCookie = UserRegUtil.getCookieByName(request,
                    Constants.MADISON_EXTEND_CONCURRENT_LICENSE_SESSION_INFO_COOKIE);
            if (null != sessionCookie) {
                String decodedSessionDataString = new String(Base64.getDecoder().decode(URLDecoder
                        .decode(sessionCookie.getValue(), MadisonConstants.UTF_8).getBytes(MadisonConstants.UTF_8)),
                        MadisonConstants.UTF_8);
                decodedSessionData = new JSONObject(decodedSessionDataString);
                sessionCounterValue = decodedSessionData.getInt(Constants.EXTEND_SESSION_COUNTER_KEY)
                        + Constants.INCREMENT_SESSION_COUNTER;
            }
            sessionData.put(Constants.EXTEND_SESSION_COUNTER_KEY, sessionCounterValue);
            sessionData.put(Constants.COOKIE_EXPIRY_TIME_KEY,
                    (new Date().getTime() + concurrentLicenseCookieExpiryInHours * 60 * 60 * 1000));
        } catch (UnsupportedEncodingException unsupportedDecodingException) {
            LOGGER.error(
                    "UserInformationUtil setConcurrentLicenseExtendSessionInfoCookie() : UnsupportedEncodingException Occurred {} while decoding value",
                    unsupportedDecodingException);
        } catch (JSONException jsonException) {
            LOGGER.error(
                    "UserRegUtil setConcurrentLicenseExtendSessionInfoCookie() : Error while setting concurrent license  session info : {}",
                    jsonException);
        }
        try {
            if (sessionData.length() > 0) {
                String sessionDataString = Base64.getEncoder()
                        .encodeToString(sessionData.toString().getBytes(MadisonConstants.UTF_8));
                UserRegUtil.setCookie(response, Constants.MADISON_EXTEND_CONCURRENT_LICENSE_SESSION_INFO_COOKIE,
                        sessionDataString, concurrentLicenseCookieExpiryInHours, isHttps, false, null);
            }
        } catch (UnsupportedEncodingException unsupportedEncodingException) {
            LOGGER.error(
                    "UserInformationUtil setConcurrentLicenseExtendSessionInfoCookie() : UnsupportedEncodingException Occurred {} while encoding value {}",
                    unsupportedEncodingException, sessionData.toString());
        }
    }

    /**
     * Allows to set all Viewpoint cookie expiry time.
     *
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param response
     *            {@link SlingHttpServletResponse}
     * @param cookieName
     *            {@link String}
     * @param cookieExpiryInHours
     *            {@link Integer}
     * @return true if a user is Logged in successfully on AEM otherwise false
     */
    public static void setCookieExpiryTimeAtExtendSessionTimeout(final SlingHttpServletRequest request,
            final SlingHttpServletResponse response, final String cookieName, final int cookieExpiryInHours) {

        boolean isHttps = request.getProtocol().contains(Constants.HTTPS);
        final Cookie madisonCookie = UserRegUtil.getCookieByName(request, cookieName);
        if (null != madisonCookie) {
            UserRegUtil.setCookie(response, cookieName, madisonCookie.getValue(), cookieExpiryInHours, isHttps, false,
                    null);
        }

        if (null != madisonCookie && null != madisonCookie.getValue()
                && cookieName.equals(Constants.MADISON_USER_PROFILE_USERINFO_COOKIE_NAME)) {
            final String[] userInfoParts = madisonCookie.getValue()
                    .split(Constants.MADISON_USER_PROFILE_USERINFO_COOKIE_SEPERATOR);
            for (int index = 0; index < userInfoParts.length; index++) {
                final Cookie madisonUserInfoCookie = UserRegUtil.getCookieByName(request,
                        Constants.MADISON_USER_PROFILE_USERINFO_COOKIE_NAME_PREFIX + userInfoParts[index]);
                if (null != madisonUserInfoCookie) {
                    UserRegUtil.setCookie(response,
                            Constants.MADISON_USER_PROFILE_USERINFO_COOKIE_NAME_PREFIX + userInfoParts[index],
                            madisonUserInfoCookie.getValue(), cookieExpiryInHours, isHttps, false, null);
                }

            }
        }
    }

    public static UserProfile updateUsersIndustry(UserProfile userProfile){
        Map<String, List<String>> removedIndustriesMapping = new HashMap<>();
        removedIndustriesMapping.put("/content/pwc-madison/global/user-preferences/industry/us/financial_services",
                Arrays.asList("/content/pwc-madison/global/user-preferences/industry/fw/real_estate",
                        "/content/pwc-madison/global/user-preferences/industry/fw/banking_and_capital_markets",
                        "/content/pwc-madison/global/user-preferences/industry/fw/private_equity",
                        "/content/pwc-madison/global/user-preferences/industry/fw/asset_management",
                        "/content/pwc-madison/global/user-preferences/industry/fw/insurance"));

        if (userProfile.getPrimaryTerritory()!= null && userProfile.getPrimaryTerritory().equals(US_TERRITORY_CODE)) {
            if (userProfile.getPreferredIndustry().size() == 1) {
                String updatedIndustry = removedIndustriesMapping.entrySet()
                        .stream()
                        .filter(entry -> entry.getValue().contains(userProfile.getPreferredIndustry().get(0)))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse("");
                if(!updatedIndustry.isEmpty()) {
                    userProfile.setPreferredIndustry(Collections.singletonList(updatedIndustry));
                }
            }
        }
        return userProfile;
    }
}
