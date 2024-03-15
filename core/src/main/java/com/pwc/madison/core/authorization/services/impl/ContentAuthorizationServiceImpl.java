package com.pwc.madison.core.authorization.services.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.utils.URIBuilder;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.xss.XSSAPI;
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

import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.asset.api.AssetManager;
import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.drew.lang.annotations.NotNull;
import com.pwc.madison.core.authorization.constants.ContentAuthorizationConstants;
import com.pwc.madison.core.authorization.enums.AccessLevel;
import com.pwc.madison.core.authorization.enums.AudienceType;
import com.pwc.madison.core.authorization.models.AuthorizationInformation;
import com.pwc.madison.core.authorization.models.ContentAuthorization;
import com.pwc.madison.core.authorization.services.ContentAuthorizationService;
import com.pwc.madison.core.authorization.services.impl.ContentAuthorizationServiceImpl.ContentAuthrizationConfiguration;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.Territory;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.services.MadisonDomainsService;
import com.pwc.madison.core.userreg.models.AcceptedTerritoryCodeAndVersion;
import com.pwc.madison.core.userreg.models.License;
import com.pwc.madison.core.userreg.models.User;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegPagesPathProvidesService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.util.MadisonUtil;

import static com.pwc.madison.core.constants.MadisonConstants.*;

@Component(
        service = ContentAuthorizationService.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
@Designate(ocd = ContentAuthrizationConfiguration.class)
public class ContentAuthorizationServiceImpl implements ContentAuthorizationService {

    private static final String INTERNAL_ONLY = "internalOnly";

	public static final Logger LOGGER = LoggerFactory.getLogger(ContentAuthorizationService.class);

    private static final String FORBIDDEN_PAGE_TERRITORY_PLACEHOLDER = "<territory>";
    private static final String FORBIDDEN_PAGE_LANGUAGE_PLACEHOLDER = "<language>";
    public static final String REFERER = "referer";
    public static final String USER_TYPE_PERSEAT = "perseat";
    public static final String USER_TYPE_CONCURRENT = "concurrent";

    @Reference
    private UserRegRestService userregRestService;

    @Reference
    private CryptoSupport cryptoSupport;

    @Reference
    private CountryTerritoryMapperService countryTerritoryMapperService;

    @Reference
    private UserRegPagesPathProvidesService userRegPagesPathProvidesService;

    @Reference
    private UserPreferencesProviderService userPreferencesProviderService;

    @Reference
    private MadisonDomainsService madisonDomainsService;

    @Reference
    private XSSAPI xssAPI;

    private String forbiddenPageUrl;
    private String forbiddenPageUrlForInternalTerritories;
    private List<String> permittedSearchBots;
    private List<String> credentialPairs;
    private List<String> pageSelectorsToBypass;
    private List<String> assetAuthorizedExtensions;
    private boolean isRcl;

    @Activate
    @Modified
    protected void Activate(final ContentAuthrizationConfiguration contentAuthrizationConfiguration) {
        forbiddenPageUrl = contentAuthrizationConfiguration.madison_forbidden_page_url();
        forbiddenPageUrlForInternalTerritories = contentAuthrizationConfiguration.madison_forbidden_page_url_internal_territory();
        permittedSearchBots = Arrays.asList(contentAuthrizationConfiguration.madison_permitted_search_bots());
        credentialPairs = Arrays.asList(contentAuthrizationConfiguration.madison_allowed_credential_pairs());
        pageSelectorsToBypass = Arrays.asList(contentAuthrizationConfiguration.madison_bypass_selectors());
        assetAuthorizedExtensions = Arrays
                .asList(contentAuthrizationConfiguration.madison_authorized_asset_extensions());
        LOGGER.debug("ContentAuthorizationServiceImpl Activate() Madison Forbidden Error Page Path : {}",
                forbiddenPageUrl);
        LOGGER.debug("ContentAuthorizationServiceImpl Activate() Madison Forbidden Error Page Path for Internal Territories : {}",
                forbiddenPageUrlForInternalTerritories);
        LOGGER.debug("ContentAuthorizationServiceImpl Activate() Madison Permitted Search Bots User Agents : {}",
                permittedSearchBots);
        LOGGER.debug("ContentAuthorizationServiceImpl Activate() Madison page selectors to bypass authorization : {}",
                pageSelectorsToBypass);
        LOGGER.debug(
                "ContentAuthorizationServiceImpl Activate() Madison asset extensions for which the authorization is to be performed : {}",
                assetAuthorizedExtensions);
    }

    @ObjectClassDefinition(name = "PwC Viewpoint Content Authorization Configuration")
    public @interface ContentAuthrizationConfiguration {

        @AttributeDefinition(
                name = "Viewpoint Forbidden Error Page URL",
                description = "The page path where a user is redirected if the user do not have required permission to access page. The url can have placeholders like for <territory> and <language> code which can then be replaced with requested page territory code and language code.",
                type = AttributeType.STRING)
        String madison_forbidden_page_url();

        @AttributeDefinition(
                name = "Viewpoint Forbidden Error Page URL for Internal territories",
                description = "The page path where a user is redirected if the user do not have required permission to access internal territories.",
                type = AttributeType.STRING)
        String madison_forbidden_page_url_internal_territory();

        @AttributeDefinition(
                name = "Viewpoint Permitted Search Bots User Agent",
                description = "User Agent Request Header Regular Expression Strings which will be allowed to access the page",
                type = AttributeType.STRING)
        String[] madison_permitted_search_bots();

        @AttributeDefinition(
                name = "Viewpoint Allowed Credential Pairs (UserName:Password)",
                description = "User ID and password pairs which will be allowed to bypass page filter to access the authorized page. Specify the username and password in format username:password. Eg: User110:abcd@1234",
                type = AttributeType.STRING,
                cardinality = Integer.MAX_VALUE)
        String[] madison_allowed_credential_pairs();

        @AttributeDefinition(
                name = "Page Selectors To Bypass Authorization",
                description = "Page selectors for which the authorization will be bypassed for HTML pages",
                type = AttributeType.STRING,
                cardinality = Integer.MAX_VALUE)
        String[] madison_bypass_selectors();

        @AttributeDefinition(
                name = "Asset Extensions To perform Authorization",
                description = "Asset Extensions for which the authorization will be performed",
                type = AttributeType.STRING,
                cardinality = Integer.MAX_VALUE)
        String[] madison_authorized_asset_extensions();

    }

    @Override
    public String getForbiddenPageUrl() {
        return madisonDomainsService.getDefaultDomain() + forbiddenPageUrl;
    }

    @Override
    public String getFullForbiddenPageUrlForInternalTerritories(String pagePath, String contentType, String protectedReferrer,
                                                                final SlingHttpServletRequest slingHttpServletRequest, final boolean isServletRequest) {
        final String territoryCode = MadisonUtil.getTerritoryCodeForPath(pagePath);
        final String languageCode = MadisonUtil.getLanguageCodeForPath(pagePath);
        String tempForbiddenPageUrl = madisonDomainsService.getDefaultDomain()
                + getForbiddenPageUrlForInternalTerritories(pagePath);
//        UriBuilder uriBuilder = UriBuilder.fromUri(tempForbiddenPageUrl);
        final String protectedReferrerURL = getProtectedReferrer(slingHttpServletRequest, isServletRequest);
        UriBuilder uriBuilder = UriBuilder
                .fromUri(tempForbiddenPageUrl)
                .queryParam(MadisonConstants.TERRITORY_QUERY_PARAM, territoryCode)
                .queryParam(MadisonConstants.LOCALE_QUERY_PARAM, languageCode)
                .queryParam(MadisonConstants.REFERRER_QUERY_PARAM, protectedReferrerURL);
        if(Objects.nonNull(contentType) && !contentType.isEmpty()){
            uriBuilder.queryParam(REQ_PARAM_CONTENT_TYPE,contentType);
        }
        if(Objects.nonNull(protectedReferrer) && !protectedReferrer.isEmpty()){
            uriBuilder.queryParam(ContentAuthorizationConstants.CONTENT_AUTHORIZATION_REFERER_HEADER_QUERY_PARAMETER, protectedReferrer);
        }
        String finalForbiddenPageUrl = uriBuilder.build().toString();

        LOGGER.debug(
                "ContentAuthorizationServiceImpl :  getForbiddenPageUrlForInternalTerritories() : Forbidden page url {} for page path {}",
                xssAPI.encodeForHTML(finalForbiddenPageUrl));
        return finalForbiddenPageUrl;
    }

    private String getForbiddenPageUrlForInternalTerritories(String pagePath) {
        final String territoryCode = MadisonUtil.getTerritoryCodeForPath(pagePath);
        final String languageCode = MadisonUtil.getLanguageCodeForPath(pagePath);
        String tempForbiddenPageUrl = forbiddenPageUrl;
        tempForbiddenPageUrl = tempForbiddenPageUrl.replace(FORBIDDEN_PAGE_TERRITORY_PLACEHOLDER, territoryCode)
                .replace(FORBIDDEN_PAGE_LANGUAGE_PLACEHOLDER, languageCode);
        return tempForbiddenPageUrl;
    }

    @Override
    public boolean performContentAuthorization(@NotNull final String resourcePath,
            final SlingHttpServletRequest slingHttpServletRequest,
            final SlingHttpServletResponse slingHttpServletResponse, final boolean isServletRequest)
            throws IOException {
        boolean allowFilterChain = true;
        isRcl = false;
        List<String> selectors = new ArrayList<String>();
        selectors.addAll(Arrays.asList(slingHttpServletRequest.getRequestPathInfo().getSelectors()));
        selectors.retainAll(pageSelectorsToBypass);
        if (!credentialPairs.contains(getCredentials(slingHttpServletRequest)) && selectors.isEmpty()) {
            final ResourceResolver resourceResolver = slingHttpServletRequest.getResourceResolver();
            if (resourceResolver != null) {
                Resource assetMetadataResource = null;
                Page page = null;
                if (resourcePath.startsWith(MadisonConstants.PWC_MADISON_DAM_BASEPATH)
                        && isAssetAuthorizable(resourcePath)) {
                    final AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);
                    int assetRenditionIndex = resourcePath
                            .indexOf(ContentAuthorizationConstants.ASSET_RENDITIONS_NODE_REL_PATH);
                    final Asset asset = assetManager
                            .getAsset(assetRenditionIndex > 0 ? resourcePath.substring(0, assetRenditionIndex - 1)
                                    : resourcePath);
                    if (null != asset) {
                        assetMetadataResource = asset
                                .getChild(ContentAuthorizationConstants.ASSET_METADATA_NODE_REL_PATH);
                    }
                } else {
                    final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
                    page = pageManager.getContainingPage(resourceResolver.resolve(resourcePath));
                }
                if (assetMetadataResource != null || page != null) {
                    allowFilterChain = handleContentAuthorization(assetMetadataResource, page, resourcePath,
                            slingHttpServletRequest, slingHttpServletResponse, isServletRequest, resourceResolver);
                } else {
                    LOGGER.warn(
                            "ContentAuthorizationServiceImpl :  handleContentAuthorization() : Resource path {} request is either not adaptable to page/asset or page lies under home page or asset extension is not be authorized",
                            xssAPI.encodeForHTML(resourcePath));
                }
            }
        }
        return allowFilterChain;
    }

    /**
     * Returns true if asset extension is present in {@link List} of authorized extensions.
     * 
     * @param resourcePath
     *            {@link String} Asset Path
     * @return {@link Boolean}
     */
    private boolean isAssetAuthorizable(final String resourcePath) {
        final int extensionStartIndex = resourcePath.lastIndexOf(".");
        return extensionStartIndex > 0 && assetAuthorizedExtensions
                .contains(resourcePath.substring(extensionStartIndex + 1, resourcePath.length()));
    }

    /**
     * Returns true if given path is rcl or not.
     * 
     * @param path
     *            {@link String}
     * @param slingHttpServletRequest
     *            {@link SlingHttpServletRequest}
     * @return {@link Boolean} if path is RCL
     */
    private boolean isRcl(String path, SlingHttpServletRequest slingHttpServletRequest) {
        RequestPathInfo requestPathInfo = slingHttpServletRequest.getRequestPathInfo();
        return StringUtils.endsWith(path, ".pwcBody")
                || (requestPathInfo != null && ArrayUtils.contains(requestPathInfo.getSelectors(), "pwcBody"));
    }

    @Override
    public AuthorizationInformation getUserAuthorization(final Page page, final User user) {
        final ContentAuthorization contentAuthorization = MadisonUtil.getPageContentAuthorization(page);
        return getUserAuthorization(contentAuthorization, user);
    }

    /**
     * Returns {@link AuthorizationInformation} which contain information like if user is authorized to access the
     * content and whether the user is redirected to forbidden page or not.
     * 
     * @param contentAuthorization
     *            {@link ContentAuthorization} of the content
     * @param user
     *            {@link User}
     * @return {@link AuthorizationInformation}
     */
    private AuthorizationInformation getUserAuthorization(final ContentAuthorization contentAuthorization,
            final User user) {
        final AuthorizationInformation authorizationInformation = new AuthorizationInformation();
        boolean isAuthorized = false;
        final UserProfile userProfile = user.getUserProfile();
        if (user.isUserLoggedIn()) {
            LOGGER.debug(
                    "ContentAuthorizationServiceImpl :  getUserAuthorization() : Getting Authorization Information for user {}",
                    xssAPI.encodeForHTML(userProfile.getEmail()));
            if(contentAuthorization.getIsInternalTerritory()){
                isAuthorized = userProfile.getIsInternalUser();
            } else if (contentAuthorization.getAudienceType().equals(AudienceType.PRIVATE_GROUP.getValue())) {
                isAuthorized = userProfile.getIsInternalUser()
                        && isUserInPrivateGroups(contentAuthorization.getPrivateGroups(), userProfile);
            } else if (contentAuthorization.getAudienceType().equals(AudienceType.INTERNAL_ONLY.getValue())) {
                isAuthorized = userProfile.getIsInternalUser();
            } else if (contentAuthorization.getAudienceType().equals(AudienceType.EXTERNAL_ONLY.getValue())) {
                isAuthorized = !userProfile.getIsInternalUser()
                        && isAuthorizedByAccessLevel(userProfile, contentAuthorization);
            } else if (contentAuthorization.getAudienceType().equals(AudienceType.INTERNAL_AND_EXTERNAL.getValue())) {
                isAuthorized = userProfile.getIsInternalUser() || (!userProfile.getIsInternalUser()
                        && isAuthorizedByAccessLevel(userProfile, contentAuthorization));
            }
            authorizationInformation.setRedirectToForbidden(!isAuthorized);
        } else if(contentAuthorization.getIsInternalTerritory()){
            authorizationInformation.setRedirectToForbidden(true);
        } else if ((contentAuthorization.getAudienceType().equals(AudienceType.INTERNAL_AND_EXTERNAL.getValue())
                || contentAuthorization.getAudienceType().equals(AudienceType.EXTERNAL_ONLY.getValue()))
                && contentAuthorization.getAccessLevel().equals(AccessLevel.FREE.getValue())) {
            isAuthorized = true;
        } else if (contentAuthorization.getAudienceType().equals(AudienceType.INTERNAL_ONLY.getValue())
                || contentAuthorization.getAudienceType().equals(AudienceType.PRIVATE_GROUP.getValue())) {
            authorizationInformation.setRedirectToForbidden(true);
        } else if ((contentAuthorization.getAudienceType().equals(AudienceType.INTERNAL_AND_EXTERNAL.getValue())
                || contentAuthorization.getAudienceType().equals(AudienceType.EXTERNAL_ONLY.getValue()))
                && contentAuthorization.getAccessLevel().equals(AccessLevel.PREMIUM.getValue())) {
            authorizationInformation.setPremiumPageRequested(true);
        }
        authorizationInformation.setAuthorized(isAuthorized);
        LOGGER.debug("ContentAuthorizationServiceImpl :  getUserAuthorization() : Authorization Information? {}",
                authorizationInformation);
        return authorizationInformation;
    }

    /**
     * Returns true if the {@link User} is authorized to access requested resource path. It returns false if the user is
     * redirected to access T&C or is not authorized.
     * 
     * @param assetMetadataResource
     *            {@link Resource}
     * @param page
     *            {@link Page}
     * @param resourcePath
     *            {@link String}
     * @param slingHttpServletRequest
     *            {@link SlingHttpServletRequest}
     * @param slingHttpServletResponse
     *            {@link SlingHttpServletResponse}
     * @param isServletRequest
     *            {@link Boolean} true if the authorized is to be performed using secured content servlet otherwise
     *            false
     * @return {@link Boolean}
     * @throws IOException
     */
    private boolean handleContentAuthorization(final Resource assetMetadataResource, final Page page,
            final String resourcePath, final SlingHttpServletRequest slingHttpServletRequest,
            final SlingHttpServletResponse slingHttpServletResponse, final boolean isServletRequest,
            final ResourceResolver resourceResolver) throws IOException {
        this.isRcl = isRcl(resourcePath, slingHttpServletRequest);
        final User user = UserInformationUtil.getUser(slingHttpServletRequest, true, userregRestService, cryptoSupport,
                slingHttpServletResponse, true, countryTerritoryMapperService, userPreferencesProviderService, false,
                true, xssAPI);
        LOGGER.debug(
                "ContentAuthorizationServiceImpl :  handleContentAuthorization() : Entered method to check permission for user.");
        boolean allowFilterChain = true;
        boolean isSearchBot = false;
        LOGGER.debug(xssAPI.encodeForHTML(slingHttpServletRequest.getHeader(HttpHeaders.USER_AGENT)));
        final String userAgent = slingHttpServletRequest.getHeader(HttpHeaders.USER_AGENT);
        if (slingHttpServletRequest.getHeader(HttpHeaders.USER_AGENT) != null) {
            for (String input : permittedSearchBots) {
                final Pattern pattern = Pattern.compile(input);
                final Matcher matcher = pattern.matcher(userAgent);
                isSearchBot = matcher.matches();
                if (isSearchBot == true) {
                    break;
                }
            }
        }
        LOGGER.debug("ContentAuthorizationServiceImpl : isSearchBot: " + isSearchBot);
        final ContentAuthorization contentAuthorization = page != null ? MadisonUtil.getPageContentAuthorization(page)
                : MadisonUtil.getAssetContentAuthorization(assetMetadataResource);
        boolean isInternalTerritory =false, isErrorPage = false;
        if (page != null) {
            isErrorPage = (page.getPath()+HTML_EXTN).endsWith(getForbiddenPageUrlForInternalTerritories(page.getPath()));
            isInternalTerritory = !isErrorPage && countryTerritoryMapperService.getTerritoryByTerritoryCode(
                            MadisonUtil.getTerritoryCodeForPath(page.getPath()))
                    .getDesignation().equals(MadisonConstants.INTERNAL_ONLY);

            contentAuthorization.setIsInternalTerritory(isInternalTerritory);
        }
        final AuthorizationInformation authorizationInformation = isSearchBot
                ? getSearchBotAuthrization(contentAuthorization)
                : getUserAuthorization(contentAuthorization, user);
        final String territory = MadisonUtil.getTerritoryCodeForPath(resourcePath);
        final String locale = MadisonUtil.getLocaleForPath(resourcePath);
        if (page != null && !isSearchBot
                && redirectToAcceptTermsAndCondition(authorizationInformation.isAuthorized(), user, page, territory)) {
            LOGGER.debug(
                    "ContentAuthorizationServiceImpl :  handleContentAuthorization() : Redirecting user {} to accept T&C for territory {}",
                    xssAPI.encodeForHTML(user.getUserProfile().getEmail()), xssAPI.encodeForHTML(territory));
            allowFilterChain = false;
            final String termsAndConditionsPageURL = getTermsAndConditionsPageURL(territory, locale,
                    slingHttpServletRequest, isServletRequest);
            slingHttpServletResponse.sendRedirect(StringUtils.normalizeSpace(termsAndConditionsPageURL));
        }
        if (!authorizationInformation.isAuthorized()) {
            allowFilterChain = false;
            if (!this.isRcl) {
                String protectedReferrer = getProtectedUrl(slingHttpServletRequest.getHeader(REFERER));
                if (isSearchBot) {
                    slingHttpServletResponse.sendError(SlingHttpServletResponse.SC_UNAUTHORIZED);
                } else if (authorizationInformation.isRedirectToForbidden()) {
                    LOGGER.debug(
                            "ContentAuthorizationServiceImpl :  handleContentAuthorization() : Redirecting user to forbidden page");
                    UserProfile userProfile = user.getUserProfile();
                    if(!user.isUserLoggedIn() && (contentAuthorization.getAudienceType().equals(AudienceType.INTERNAL_ONLY.getValue())
                            || contentAuthorization.getAudienceType().equals(AudienceType.PRIVATE_GROUP.getValue()) || isInternalTerritory)){
                        slingHttpServletResponse.sendRedirect(StringUtils.normalizeSpace(getForbiddenPageUrl(resourcePath, INTERNAL_ONLY, user.isUserLoggedIn(), protectedReferrer, slingHttpServletRequest, isServletRequest)));
                    }else if(!userProfile.getIsInternalUser()) {
                        if(isInternalTerritory){
                            slingHttpServletResponse.sendRedirect(StringUtils.normalizeSpace(getFullForbiddenPageUrlForInternalTerritories(resourcePath, INTERNAL_ONLY, protectedReferrer, slingHttpServletRequest, isServletRequest)));
                        }
                        boolean isConcurrentUser = userProfile.getContentAccessInfo().isConcurrentLicensedUser();

                        String accessLevel = contentAuthorization.getAccessLevel();
                        if (Objects.nonNull(accessLevel) && accessLevel.equals(AccessLevel.LICENSED.getValue())) {
                            boolean isUserLicensed = isUserLicensed(contentAuthorization.getLicenses(), userProfile);
                            if (isConcurrentUser) {
                                //If concurrent user does not have **due to unavailability of a free concurrent license** seat
                                if (isUnavailbilityOfLicense(contentAuthorization.getLicenses(), userProfile)) {
                                    slingHttpServletResponse.sendRedirect(StringUtils.normalizeSpace(getAccessRestrictedPageUrl(resourcePath, USER_TYPE_CONCURRENT, protectedReferrer)));
                                }
                                //If concurrent user does not have license for the requested content
                                else if (!isUserLicensed) {
                                    slingHttpServletResponse.sendRedirect(StringUtils.normalizeSpace(getForbiddenPageUrl(resourcePath)));
                                } else {
                                    slingHttpServletResponse.sendRedirect(StringUtils.normalizeSpace(getForbiddenPageUrl(resourcePath, INTERNAL_ONLY, user.isUserLoggedIn(), protectedReferrer, slingHttpServletRequest, isServletRequest)));
                                }
                            }
                            //If user is PerSeat Only and has no License for the requested content
                            else if (!isUserLicensed) {
                                slingHttpServletResponse.sendRedirect(StringUtils.normalizeSpace(getAccessRestrictedPageUrl(resourcePath, USER_TYPE_PERSEAT, protectedReferrer)));
                            } else {
                                slingHttpServletResponse.sendRedirect(StringUtils.normalizeSpace(getForbiddenPageUrl(resourcePath, INTERNAL_ONLY, user.isUserLoggedIn(), protectedReferrer, slingHttpServletRequest, isServletRequest)));
                            }
                        } else {
                            slingHttpServletResponse.sendRedirect(StringUtils.normalizeSpace(getForbiddenPageUrl(resourcePath, INTERNAL_ONLY, user.isUserLoggedIn(), protectedReferrer, slingHttpServletRequest, isServletRequest)));
                        }
                    }else {
                        slingHttpServletResponse.sendRedirect(StringUtils.normalizeSpace(getForbiddenPageUrl(resourcePath)));
                    }

                } else {
                    LOGGER.debug(
                            "ContentAuthorizationServiceImpl :  handleContentAuthorization() : Redirecting user to Gated content page");
                    boolean isInternalOnly = INTERNAL_ONLY.equals(contentAuthorization.getAudienceType());
                    String accessType = StringUtils.isNotBlank(contentAuthorization.getAccessLevel()) ? contentAuthorization.getAccessLevel() : StringUtils.EMPTY;
                    final String gatedContentPagePath = getGatedContentPagePath(territory, locale,
                            slingHttpServletRequest, isServletRequest,
                            authorizationInformation.isPremiumPageRequested(), isInternalOnly, accessType);
                    slingHttpServletResponse.sendRedirect(StringUtils.normalizeSpace(gatedContentPagePath));
                }
            } else {
                slingHttpServletResponse.sendError(SlingHttpServletResponse.SC_UNAUTHORIZED);
            }

        }
        return allowFilterChain;
    }

    /**
     * Returns credentials from {@Value HttpHeaders#AUTHORIZATION} request header.
     * 
     * @param slingHttpServletRequest
     *            {@link SlingHttpServletRequest}
     * @return {@link String}
     */
    public static String getCredentials(final SlingHttpServletRequest slingHttpServletRequest) {
        final String authorization = slingHttpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (null != authorization
                && authorization.startsWith(ContentAuthorizationConstants.AUTHORIZATION_HEADER_PREFIX)) {
            String base64Credentials = authorization
                    .substring(ContentAuthorizationConstants.AUTHORIZATION_HEADER_PREFIX.length()).trim();
            final byte[] credentialsDecoded = Base64.getDecoder().decode(base64Credentials);
            return new String(credentialsDecoded, StandardCharsets.UTF_8);
        }
        return null;
    }

    /**
     * Returns the content content page path where the user is redirected when the user is not authorized. As well as
     * adds given {@value MadisonConstants#TERRITORY_QUERY_PARAM}, {@value MadisonConstants#LOCALE_QUERY_PARAM},
     * {@value MadisonConstants#REFERRER_QUERY_PARAM},
     * {@value ContentAuthorizationConstants#CONTENT_AUTHORIZATION_IS_PREMIUM_QUERY_PARAMETER} and
     * {@value ContentAuthorizationConstants#CONTENT_AUTHORIZATION_REFERER_HEADER_QUERY_PARAMETER} as query parameters.
     * 
     * @param territory
     *            {@link String}
     * @param locale
     *            {@link String}
     * @param slingHttpServletRequest
     *            {@link SlingHttpServletRequest}
     * @param isServletRequest
     *            {@link Boolean} true if the authorized is to be performed using secured content servlet otherwise
     *            false
     * @param isPremiumPage
     *            {@link Boolean} true if page requested was premium or not.
     * @param accessType 
     * @param isInternalOnly 
     * @return {@link string}
     */
    private String getGatedContentPagePath(final String territory, final String locale,
            final SlingHttpServletRequest slingHttpServletRequest, final boolean isServletRequest,
			final boolean isPremiumPage, final boolean isInternalOnly, final String accessType) {
		final String protectedReferrer = getProtectedReferrer(slingHttpServletRequest, isServletRequest);
		final String protectedRefererHeader = getProtectedReferrerHeader(slingHttpServletRequest);
		UriBuilder uriBuilder = UriBuilder
				.fromUri(userRegPagesPathProvidesService.getGatedContentpagePath() + HTML_EXTN)
				.queryParam(MadisonConstants.TERRITORY_QUERY_PARAM, territory)
				.queryParam(MadisonConstants.LOCALE_QUERY_PARAM, locale)
				.queryParam(MadisonConstants.REFERRER_QUERY_PARAM, protectedReferrer)
				.queryParam(ContentAuthorizationConstants.CONTENT_AUTHORIZATION_IS_PREMIUM_QUERY_PARAMETER,
						isPremiumPage)
				.queryParam(ContentAuthorizationConstants.CONTENT_AUTHORIZATION_ACCESS_TYPE_QUERY_PARAMETER,
						accessType)
				.queryParam(ContentAuthorizationConstants.CONTENT_AUTHORIZATION_IS_INTERNAL_ONLY_QUERY_PARAMETER,
						isInternalOnly);
		if (null != protectedRefererHeader) {
			uriBuilder = uriBuilder.queryParam(
					ContentAuthorizationConstants.CONTENT_AUTHORIZATION_REFERER_HEADER_QUERY_PARAMETER,
					protectedRefererHeader);
		}
		final String gatedContentUrl = uriBuilder.build().toString();
		LOGGER.debug("ContentAuthorizationServiceImpl :  getGatedContentPagePath() : Gated content URL is {}",
				xssAPI.encodeForHTML(gatedContentUrl));
		return gatedContentUrl;
	}

    /**
     * Returns true if user should be redirected to accept Terms and Conditions for given {@link Page} territory.
     * 
     * @param isAuthorized
     *            {@link Boolean}
     * @param user
     *            {@link User}
     * @param page
     *            {@link Page}
     * @param territory
     *            {@link string}
     * @return {@link Boolean}
     */
    private boolean redirectToAcceptTermsAndCondition(final boolean isAuthorized, final User user, final Page page,
            final String territory) {
        return user.isUserLoggedIn() && isAuthorized && !user.getUserProfile().getIsInternalUser()
                && !isLatestTerritoryTermsConditionsAccepted(user.getUserProfile(), territory)
                && isPageNotExcludedFromTermsCondition(page);
    }

    /**
     * Returns {@link AuthorizationInformation} which contains information like if search bot is authorized to access
     * requested page or asset by accessing given {@link ContentAuthorization}. Search bot is authorized if content is
     * external or 'internal and external'.
     * 
     * @param contentAuthorization
     *            {@link ContentAuthorization}
     * @return {@link AuthorizationInformation}
     */
    private AuthorizationInformation getSearchBotAuthrization(final ContentAuthorization contentAuthorization) {
        final AuthorizationInformation authorizationInformation = new AuthorizationInformation();
        authorizationInformation.setAuthorized(
                contentAuthorization.getAudienceType().equals(AudienceType.INTERNAL_AND_EXTERNAL.getValue())
                        || contentAuthorization.getAudienceType().equals(AudienceType.EXTERNAL_ONLY.getValue()));
        return authorizationInformation;
    }

    /**
     * Returns true if the user access level matches the content access level i.e
     * <ul>
     * <li>if the page access level is {@value AccessLevel#FREE} or {@value AccessLevel#PREMIUM}
     * <li>if user has license for content
     * </ul>
     * otherwise false.
     * 
     * @param userProfile
     *            {@link UserProfile}
     * @param contentAuthorization
     *            {@link ContentAuthorization}
     * @return {@link Boolean}
     */
    private boolean isAuthorizedByAccessLevel(final UserProfile userProfile,
            final ContentAuthorization contentAuthorization) {
        return contentAuthorization.getAccessLevel().equals(AccessLevel.FREE.getValue())
                || contentAuthorization.getAccessLevel().equals(AccessLevel.PREMIUM.getValue())
                || (contentAuthorization.getAccessLevel().equals(AccessLevel.LICENSED.getValue())
                        && isUserLicensed(contentAuthorization.getLicenses(), userProfile));
    }

    /**
     * Returns true if user has any of the given content licenses and the time of user's license is not expired.
     * 
     * @param contentLicenses
     *            {@link String[]}
     * @param userProfile
     *            {@link UserProfile}
     * @return {@link Boolean}
     */
    private boolean isUserLicensed(final String[] contentLicenses, final UserProfile userProfile) {
        if (null != contentLicenses && null != userProfile.getContentAccessInfo().getLicenses()) {
            List<String> contentLicensesList = Arrays.asList(contentLicenses);
            for (final License license : userProfile.getContentAccessInfo().getLicenses()) {
                if (contentLicensesList.contains(license.getCode())
                        && System.currentTimeMillis() <= license.getExpiryTs()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if user is in any of the given content private groups.
     * 
     * @param contentPrivateGroups
     *            {@link String[]}
     * @param userProfile
     *            {@link UserProfile}
     * @return {@link Boolean}
     */
    private boolean isUserInPrivateGroups(final String[] contentPrivateGroups, final UserProfile userProfile) {
        if (null != contentPrivateGroups && null != userProfile.getContentAccessInfo().getPrivateGroups()) {
            List<String> contentPrivateGroupsList = Arrays.asList(contentPrivateGroups);
            for (final String privateGroup : userProfile.getContentAccessInfo().getPrivateGroups()) {
                if (contentPrivateGroupsList.contains(privateGroup)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the page is not excluded from Terms and conditions check. Some criteria to exclude page from
     * terms and conditions are:
     * <ul>
     * <li>page is home page
     * <li>author has checked {@value #PAGE_PROPERTY_DISABLE_TNC} property on page
     * </ul>
     * 
     * @param page
     *            {@link Page}
     * @return {@link Boolean}
     */
    private boolean isPageNotExcludedFromTermsCondition(final Page page) {
        if (page.getPath().matches(MadisonConstants.MADISON_HOMEPAGE_HIERARCHY)) {
            return false;
        } else {
            return !page.getProperties().get(ContentAuthorizationConstants.PAGE_PROPERTY_DISABLE_TNC, false);
        }
    }

    /**
     * Returns true if the user to which {link {@link UserProfile} belongs has accepted the latest terms and conditions
     * version for given territory.
     * 
     * @param userProfile
     *            {@link UserProfile}
     * @param territory
     *            {@link Territory} territory code
     * @return {@link Boolean}
     */
    private boolean isLatestTerritoryTermsConditionsAccepted(final UserProfile userProfile, final String territory) {
        final List<AcceptedTerritoryCodeAndVersion> acceptedTerritoryCodeAndVersionList = userProfile
                .getAcceptedTerritoryCodeAndVersion();
        for (final AcceptedTerritoryCodeAndVersion acceptedTerritoryCodeAndVersion : acceptedTerritoryCodeAndVersionList) {
            if (acceptedTerritoryCodeAndVersion.getTerritoryCode().equals(territory)) {
                return acceptedTerritoryCodeAndVersion.getVersion().equals(countryTerritoryMapperService
                        .getTerritoryCodeToTerritoryMap().get(territory).getTermsAndConditionsVersion());
            }
        }
        return false;
    }

    /**
     * Returns the terms and conditions page URL and add given {@value MadisonConstants#TERRITORY_QUERY_PARAM},
     * {@value MadisonConstants#LOCALE_QUERY_PARAM}, {@value MadisonConstants#REFERRER_QUERY_PARAM} as query parameters.
     * 
     * @param territory
     *            {@link String}
     * @param locale
     *            {@link String}
     * @param slingHttpServletRequest
     *            {@link SlingHttpServletRequest}
     * @param isServletRequest
     *            {@link Boolean} true if the authorized is to be performed using secured content servlet otherwise
     *            false
     * @return {@link String}
     */
    private String getTermsAndConditionsPageURL(final String territory, final String locale,
            final SlingHttpServletRequest slingHttpServletRequest, final boolean isServletRequest) {
        final String protectedReferrer = getProtectedReferrer(slingHttpServletRequest, isServletRequest);
        return UriBuilder
                .fromUri(userRegPagesPathProvidesService.getTermsAndConditionPagePath() + HTML_EXTN)
                .queryParam(MadisonConstants.TERRITORY_QUERY_PARAM, territory)
                .queryParam(MadisonConstants.LOCALE_QUERY_PARAM, locale)
                .queryParam(MadisonConstants.REFERRER_QUERY_PARAM, protectedReferrer).build().toString();
    }

    /**
     * Returns the protected referrer from {@link SlingHttpServletRequest}. Returns blank if some exception occurs while
     * creating referrer.
     * 
     * @param slingHttpServletRequest
     *            {@link SlingHttpServletRequest}
     * @param isServletRequest
     *            {@link Boolean} true if the authorized is to be performed using secured content servlet otherwise
     *            false
     * @return {@link String}
     */
    private String getProtectedReferrer(final SlingHttpServletRequest slingHttpServletRequest,
            final boolean isServletRequest) {
        String referer = StringUtils.EMPTY;
        try {
            referer = getReferrerUrl(slingHttpServletRequest, isServletRequest);
            referer = referer == null ? referer : getProtectedUrl(referer);
        } catch (URISyntaxException uriSyntaxException) {
            LOGGER.error(
                    "ContentAuthorizationServiceImpl :  getProtectedReferrer() : UriSyntaxException Exception occured while adding parameters to the referrer{}",
                    uriSyntaxException);
        }
        LOGGER.debug("ContentAuthorizationServiceImpl :  getProtectedReferrer() : referer returned is {}",
                xssAPI.encodeForHTML(referer));
        return referer;
    }

    /**
     * Returns the protected {@value HttpHeaders#REFERER} header value from {@link SlingHttpServletRequest} or null in
     * case the {@value HttpHeaders#REFERER} header is not present.
     * 
     * @param slingHttpServletRequest
     *            {@link SlingHttpServletRequest}
     * @return {@link String}
     */
    private String getProtectedReferrerHeader(final SlingHttpServletRequest slingHttpServletRequest) {
        String refererHeader = slingHttpServletRequest.getHeader(HttpHeaders.REFERER);
        return refererHeader == null ? refererHeader : getProtectedUrl(refererHeader);
    }

    /**
     * Returns the protected URL from the {@link SlingHttpServletRequest}. Returns empty if exception occurs while
     * protecting URL.
     * 
     * @param url
     *            {@link String}
     * @return {@link String}
     */
    private String getProtectedUrl(final String url) {
        String protectedReferrer = StringUtils.EMPTY;
        try {
            protectedReferrer = cryptoSupport.protect(url);
        } catch (CryptoException cryptoException) {
            LOGGER.error(
                    "ContentAuthorizationServiceImpl :  getProtectedUrl() : Crypto Exception occured while encrypting url {} : {}",
                    xssAPI.encodeForHTML(url), cryptoException);
        }
        return protectedReferrer;
    }

    /**
     * Get the referrer URL after adding parameters.
     * 
     * @param request
     *            {@link SlingHttpServletRequest}
     * @param isServletRequest
     *            {@link Boolean} true if the authorized is to be performed using secured content servlet otherwise
     *            false
     * @return {@link String}
     * @throws URISyntaxException
     *             {@link URISyntaxException}
     */
    private String getReferrerUrl(final SlingHttpServletRequest slingHttpServletRequest, final boolean isServletRequest)
            throws URISyntaxException {
        if (isServletRequest) {
            return slingHttpServletRequest
                    .getParameter(ContentAuthorizationConstants.CONTENT_AUTHORIZATION_URI_QUERY_PARAMETER);
        } else {
            final URIBuilder uriBuilder = new URIBuilder(slingHttpServletRequest.getRequestURL().toString());
            final Map<String, String[]> parameterMap = slingHttpServletRequest.getParameterMap();
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                for (String value : entry.getValue()) {
                    uriBuilder.addParameter(entry.getKey(), value);
                }
            }
            return uriBuilder.toString();
        }
    }

    /**
     * Returns forbidden page path for the requested page. It gets the territory and language from the given page path
     * and replace it with in the forbidden page path with placeholder {@value #FORBIDDEN_PAGE_LANGUAGE_PLACEHOLDER}
     * {@value #FORBIDDEN_PAGE_TERRITORY_PLACEHOLDER}.
     * 
     * @param pagePathxssAPI.encodeForHTML(
     *            {@link String}
     * @return {@link String}
     */
    private String getForbiddenPageUrl(final String pagePath) {
        final String territoryCode = MadisonUtil.getTerritoryCodeForPath(pagePath);
        final String languageCode = MadisonUtil.getLanguageCodeForPath(pagePath);
        final String finalForbiddenPageUrl = madisonDomainsService.getDefaultDomain()
                + forbiddenPageUrl.replace(FORBIDDEN_PAGE_TERRITORY_PLACEHOLDER, territoryCode)
                        .replace(FORBIDDEN_PAGE_LANGUAGE_PLACEHOLDER, languageCode);
        LOGGER.debug(
                "ContentAuthorizationServiceImpl :  getForbiddenPageUrl() : Forbidden page url {} for page path {}",
                xssAPI.encodeForHTML(pagePath), xssAPI.encodeForHTML(finalForbiddenPageUrl));
        return finalForbiddenPageUrl;
    }

    /**
     * Returns forbidden page path for the requested page. It gets the territory and language from the given page path
     * and replace it with in the forbidden page path with placeholder {@value #FORBIDDEN_PAGE_LANGUAGE_PLACEHOLDER}
     * {@value #FORBIDDEN_PAGE_TERRITORY_PLACEHOLDER}. In addition to this method {@link #getForbiddenPageUrl(String)},
     * this method allows to add query paramter to the forbidden URL.
     *
     * @param pagePathxssAPI.encodeForHTML(
     *            {@link String}
     * @param pagePathxssAPI.encodeForHTML(
     *            {@link String}
     * @return {@link String}
     */
    private String getForbiddenPageUrl(final String pagePath, String contentType, boolean isUserLoggedIn, String protectedRefererHeader,
                                       final SlingHttpServletRequest slingHttpServletRequest, final boolean isServletRequest) {
        final String territoryCode = MadisonUtil.getTerritoryCodeForPath(pagePath);
        final String languageCode = MadisonUtil.getLanguageCodeForPath(pagePath);
        final String tempFinalForbiddenPageUrl = madisonDomainsService.getDefaultDomain()
                + forbiddenPageUrl.replace(FORBIDDEN_PAGE_TERRITORY_PLACEHOLDER, territoryCode)
                .replace(FORBIDDEN_PAGE_LANGUAGE_PLACEHOLDER, languageCode);
        final String protectedReferrerURL = getProtectedReferrer(slingHttpServletRequest, isServletRequest);
        UriBuilder uriBuilder = UriBuilder
                .fromUri(tempFinalForbiddenPageUrl)
                .queryParam(MadisonConstants.TERRITORY_QUERY_PARAM, territoryCode)
                .queryParam(MadisonConstants.LOCALE_QUERY_PARAM, languageCode);

        if(!isUserLoggedIn){
            uriBuilder.queryParam(MadisonConstants.REFERRER_QUERY_PARAM, protectedReferrerURL);
        }
        if(Objects.nonNull(contentType) && !contentType.isEmpty()){
            uriBuilder.queryParam(REQ_PARAM_CONTENT_TYPE,contentType);
        }
        if(Objects.nonNull(protectedRefererHeader) && !protectedRefererHeader.isEmpty()){
            uriBuilder.queryParam(ContentAuthorizationConstants.CONTENT_AUTHORIZATION_REFERER_HEADER_QUERY_PARAMETER, protectedRefererHeader);
        }
        String finalForbiddenPageUrl = uriBuilder.build().toString();

        LOGGER.debug(
                "ContentAuthorizationServiceImpl :  getForbiddenPageUrl() : Forbidden page url {} for page path {}",
                xssAPI.encodeForHTML(pagePath), xssAPI.encodeForHTML(finalForbiddenPageUrl));
        return finalForbiddenPageUrl;
    }

    private String getAccessRestrictedPageUrl(final String pagePath, String userType, String protectedReferrer) {
        final String territoryCode = MadisonUtil.getTerritoryCodeForPath(pagePath);
        final String languageCode = MadisonUtil.getLanguageCodeForPath(pagePath);
        String tempForbiddenPageUrl = forbiddenPageUrl;
        tempForbiddenPageUrl = madisonDomainsService.getDefaultDomain()
                + tempForbiddenPageUrl.replace(FORBIDDEN_PAGE_TERRITORY_PLACEHOLDER, territoryCode)
                .replace(FORBIDDEN_PAGE_LANGUAGE_PLACEHOLDER, languageCode);
        UriBuilder uriBuilder = UriBuilder.fromUri(tempForbiddenPageUrl);
        if(Objects.nonNull(userType) && !userType.isEmpty()){
            uriBuilder.queryParam(REQ_PARAM_USER_TYPE,userType);
        }
        if(Objects.nonNull(protectedReferrer) && !protectedReferrer.isEmpty()){
            uriBuilder.queryParam(ContentAuthorizationConstants.CONTENT_AUTHORIZATION_REFERER_HEADER_QUERY_PARAMETER, protectedReferrer);
        }
        String finalForbiddenPageUrl = uriBuilder.build().toString();

        LOGGER.debug(
                "ContentAuthorizationServiceImpl :  getForbiddenPageUrl() : Forbidden page url {} for page path {}",
                xssAPI.encodeForHTML(pagePath), xssAPI.encodeForHTML(finalForbiddenPageUrl));
        return finalForbiddenPageUrl;
    }

    /**
     * Returns true if user has any of the given content licenses, but it is currently unavailable.
     *
     * @param contentLicenses
     *            {@link String[]}
     * @param userProfile
     *            {@link UserProfile}
     * @return {@link Boolean}
     */
    private boolean isUnavailbilityOfLicense(final String[] contentLicenses, final UserProfile userProfile){
        if (null != contentLicenses && null != userProfile.getContentAccessInfo().getUnavailableLicenses()) {
            List<String> contentLicensesList = Arrays.asList(contentLicenses);
            for (final String license : userProfile.getContentAccessInfo().getUnavailableLicenses()) {
                if (contentLicensesList.contains(license)) {
                    return true;
                }
            }
        }
        return false;
    }
}
