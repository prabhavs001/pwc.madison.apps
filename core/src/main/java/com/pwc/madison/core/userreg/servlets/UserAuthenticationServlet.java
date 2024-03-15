package com.pwc.madison.core.userreg.servlets;

import com.adobe.granite.crypto.CryptoSupport;
import com.pwc.madison.core.authorization.services.ContentAuthorizationService;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.Constants;
import com.pwc.madison.core.userreg.services.UserLicensesProviderService;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserRegUtil;
import com.pwc.madison.core.util.LocaleUtils;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

/**
 * 
 * Servlet to authenticate user and redirect user to Viewpoint page from where user initiated the user login.
 * 
 */
@Component(
        service = Servlet.class,
        name = "PwC Viewpoint User Authentication Servlet",
        property = { org.osgi.framework.Constants.SERVICE_DESCRIPTION + "=PwC Viewpoint User Authentication Servlet",
                "sling.servlet.paths=/bin/userreg/authenticate", "sling.servlet.methods=" + HttpConstants.METHOD_GET })
public class UserAuthenticationServlet extends SlingAllMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAuthenticationServlet.class);

    private static final String FORBIDDEN_PAGE_TERRITORY_PLACEHOLDER = "<territory>";
    private static final String FORBIDDEN_PAGE_LANGUAGE_PLACEHOLDER = "<language>";

    @Reference
    private transient UserRegRestService userRegRestService;

    @Reference
    private transient ContentAuthorizationService contentAuthorizationService;

    @Reference
    private transient ResourceResolverFactory resourceResolverFactory;

    @Reference
    private transient CountryTerritoryMapperService countryTerritoryMapperService;

    @Reference
    private transient CryptoSupport cryptoSupport;

    @Reference
    private transient UserPreferencesProviderService userPreferencesProviderService;

    @Reference
    private transient XSSAPI xssAPI;

    @Reference
    private transient UserLicensesProviderService userLicensesProviderService;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        final String samlErrorParam = request.getParameter(MadisonConstants.SAML_ERROR_REQUEST_PARAM);
        if (StringUtils.isNotBlank(samlErrorParam)) {
            final String errorPageURL = getErrorPageUrl(request, response, false);
            response.sendRedirect(StringUtils.normalizeSpace(errorPageURL));
        } else {
            boolean isRedirected = true;
            final String token = request.getParameter(Constants.USER_AUTHENTICATION_TOKEN_QUERY_PARAMETER_NAME);
            final String extToken = request
                    .getParameter(Constants.EXTERNAL_USER_AUTHENTICATION_TOKEN_QUERY_PARAMETER_NAME);
            final String accessDenied = request.getParameter(Constants.ACCESS_DENIED_QUERY_PARAMETER_NAME);
            final String showRemoveSession = request.getParameter(Constants.SHOW_REMOVE_SESSION_QUERY_PARAM);
            final String deleteTokens = request.getParameter(Constants.DELETE_TOKENS_QUERY_PARAM);
            if (StringUtils.isNotBlank(accessDenied)) {
                isRedirected = false;
                String redirectionPath = getErrorPageUrl(request, response, true);
                LOGGER.debug("UserAuthenticationServlet doGet() : Unauthorized userRedirection Path is {} : ",
                        xssAPI.encodeForHTML(redirectionPath));
                response.sendRedirect(StringUtils.normalizeSpace(redirectionPath));
            } else if (StringUtils.isNotBlank(token) || StringUtils.isNotBlank(extToken)
                    || StringUtils.isNotBlank(showRemoveSession)) {
                boolean isValid = false;
                if (StringUtils.isNotBlank(showRemoveSession)) {
                    isValid = true;
                    LOGGER.debug("UserAuthenticationServlet doGet() : Need to show Remove Session Popup");
                    UserRegUtil.setCookie(response, Constants.SHOW_REMOVE_SESSION_COOKIE_NAME, "1",
                            userRegRestService.getAuthenticationSingleSeatLicenseCookieExpiryHours(),
                            request.getProtocol().contains(Constants.HTTPS), false, null);
                } else if (StringUtils.isNotBlank(extToken)) {
                    // set cookie for cp, valid till login token time
                    isValid = UserRegUtil.loginMadisonUser(request, response, resourceResolverFactory, extToken,
                            userRegRestService, countryTerritoryMapperService, cryptoSupport,
                            userPreferencesProviderService, xssAPI, userLicensesProviderService, true, true);
                } else {
                    isValid = UserRegUtil.loginMadisonUser(request, response, resourceResolverFactory, token,
                            userRegRestService, countryTerritoryMapperService, cryptoSupport,
                            userPreferencesProviderService, xssAPI, userLicensesProviderService, true, false);
                }
                if (isValid) {
                    isRedirected = false;
                    String redirectionPath = getRedirectPath(request, response);
                    LOGGER.debug("UserAuthenticationServlet doGet() : Redirection Path is {} : ",
                            xssAPI.encodeForHTML(redirectionPath));
                    response.sendRedirect(StringUtils.normalizeSpace(redirectionPath));
                }
            }
            if (isRedirected) {
                redirectToAuthenticator(request, response, StringUtils.isNotBlank(deleteTokens));
            }
        }
    }

    /**
     * Redirect to SAML Authentication URL.
     * 
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param response
     *            {@link SlingHttpServletResponse}
     * @throws IOException
     *             {@link IOException}
     */
    private void redirectToAuthenticator(final SlingHttpServletRequest request, final SlingHttpServletResponse response,
            final boolean deleteTokens) throws IOException {
        final String redirectUrlParamter = UriBuilder.fromUri(request.getRequestURL().toString())
                .replaceQueryParam(Constants.USER_AUTHENTICATION_TOKEN_QUERY_PARAMETER_NAME, null).build().toString();
        String authRedirectUrl = UriBuilder.fromUri(userRegRestService.getAuthenticationApiUrl())
                .queryParam(Constants.REDIRECT_URL_QUERY_PARAMETER_NAME, redirectUrlParamter).build().toString();
        if(deleteTokens) {
            authRedirectUrl += "&deleteTokens=1";
        }
        response.sendRedirect(StringUtils.normalizeSpace(authRedirectUrl));
    }

    /**
     * Returns the redirection page path where user is redirected after successful SAML login.
     * 
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param response
     *            {@link SlingHttpServletResponse
     * @return{@link String}
     */
    private String getRedirectPath(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {
        String redirectionPath = userRegRestService.getUserDefaultRedirectionPath();
        final Cookie madisonRedirectCookie = UserRegUtil.getCookieByName(request,
                Constants.MADISON_USER_REDIRECT_PATH_COOKIE);
        if (null != madisonRedirectCookie) {
            redirectionPath = madisonRedirectCookie.getValue();
            boolean isHttps = request.getProtocol().contains(Constants.HTTPS);
            UserRegUtil.setCookie(response, Constants.MADISON_USER_REDIRECT_PATH_COOKIE, null, 0, isHttps, false, null);
        }
        return redirectionPath;
    }

    /**
     * Returns error page path where user is redirected if errors occurs on SAML authentication. It gets the territory
     * and language from the redirection page path and replace it with in the error page path with placeholder
     * {@value #FORBIDDEN_PAGE_LANGUAGE_PLACEHOLDER} {@value #FORBIDDEN_PAGE_TERRITORY_PLACEHOLDER}.
     * 
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param response
     *            {@link SlingHttpServletResponse}
     * @param isForbidden
     *            true in case of 403, false in case of 500
     *
     * @return {@link String}
     */
    private String getErrorPageUrl(final SlingHttpServletRequest request, final SlingHttpServletResponse response,
            boolean isForbidden) {
        final String pageUrl = getRedirectPath(request, response);
        String pageAbsolutePath = null;

        try {
            URL pageURL = new URL(pageUrl);
            String urlContentPath = pageURL.getPath();
            ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                    MadisonConstants.MADISON_READ_SUB_SERVICE);
            Resource pageResource = resourceResolver.resolve(urlContentPath);
            pageAbsolutePath = pageResource.getPath();
            resourceResolver.close();
        } catch (MalformedURLException e) {
            LOGGER.error("Error in parsing URL : {}", StringUtils.normalizeSpace(pageUrl));
        }

        String defaultLocale = countryTerritoryMapperService.getDefaultLocale();

        String territoryCode = MadisonUtil.getTerritoryCodeForPathUrl(pageAbsolutePath);
        if (Objects.isNull(territoryCode)) {
            String defaultTerritory = LocaleUtils.getTerritoryFromLocale(defaultLocale);
            territoryCode = defaultTerritory;
            LOGGER.warn("Error in parsing territory code from path : {}. Using default territory code : {}",
                    pageAbsolutePath, territoryCode);
        }
        String languageCode = MadisonUtil.getLanguageCodeForPathUrl(pageAbsolutePath);
        if (Objects.isNull(languageCode)) {
            String defaultLanguage = LocaleUtils.getLanguageFromLocale(defaultLocale);
            languageCode = defaultLanguage;
            LOGGER.warn("Error in parsing language code from path : {}. Using default language code : {}",
                    pageAbsolutePath, languageCode);
        }
        String finalErrorPageUrl = isForbidden ? contentAuthorizationService.getForbiddenPageUrl()
                : userRegRestService.getServerErrorPageUrl();

        finalErrorPageUrl = finalErrorPageUrl.replace(FORBIDDEN_PAGE_TERRITORY_PLACEHOLDER, territoryCode)
                .replace(FORBIDDEN_PAGE_LANGUAGE_PLACEHOLDER, languageCode);
        LOGGER.debug("InternalUserAuthenticationServlet :  getErrorPageUrl() : Error page URL {} for page URL {}",
                xssAPI.encodeForHTML(pageUrl), xssAPI.encodeForHTML(finalErrorPageUrl));
        return finalErrorPageUrl;
    }

}
