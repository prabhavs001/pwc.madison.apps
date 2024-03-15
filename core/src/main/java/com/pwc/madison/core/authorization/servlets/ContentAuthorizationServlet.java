package com.pwc.madison.core.authorization.servlets;

import java.io.IOException;

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.pwc.madison.core.authorization.constants.ContentAuthorizationConstants;
import com.pwc.madison.core.authorization.services.ContentAuthorizationService;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.models.User;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;

/**
 * The servlet implements content authorization.
 */
@Component(
        service = Servlet.class,
        name = "PwC Viewpoint Secured Content Authentication Servlet",
        property = {
                org.osgi.framework.Constants.SERVICE_DESCRIPTION + "=PwC Viewpoint Secured Content Authentication Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/authorization/check",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_HEAD })
public class ContentAuthorizationServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentAuthorizationServlet.class);

    @Reference
    private transient ContentAuthorizationService contentAuthorizationService;
    
    @Reference
    private UserRegRestService userregRestService;

    @Reference
    private CryptoSupport cryptoSupport;

    @Reference
    private CountryTerritoryMapperService countryTerritoryMapperService;

    @Reference
    private UserPreferencesProviderService userPreferencesProviderService;

    @Reference
    private transient XSSAPI xssApi;
    
    @Override
    public void doHead(SlingHttpServletRequest slingHttpServletRequest,
            SlingHttpServletResponse slingHttpServletResponse) throws IOException {
        final String uri = slingHttpServletRequest
                .getParameter(ContentAuthorizationConstants.CONTENT_AUTHORIZATION_URI_QUERY_PARAMETER);
        LOGGER.debug("ContentAuthorizationServlet doHead : Entered Content authorization servlet with URI : {}", xssApi.encodeForHTML(uri));
        if (null != uri) {
            if (uri.contains(MadisonConstants.DWNLD_PDF_SELECTOR)) {
                final User user = UserInformationUtil.getUser(slingHttpServletRequest, true, userregRestService,
                        cryptoSupport, slingHttpServletResponse, true, countryTerritoryMapperService,
                        userPreferencesProviderService, false, true, xssApi);
                if (user.isUserLoggedIn()) {
                    UserProfile userProfile = user.getUserProfile();
                    if (userProfile != null && !userProfile.getIsInternalUser()
                            && (uri.endsWith(".dwnldpdf.i_n.html") || uri.endsWith(".dwnldpdf.i_p.html"))) {
                        slingHttpServletResponse.sendError(SlingHttpServletResponse.SC_UNAUTHORIZED);
                    }
                } else if (uri.endsWith(".dwnldpdf.i_e_p.html") || uri.endsWith(".dwnldpdf.i_e_l.html")) {
                    slingHttpServletResponse.sendError(SlingHttpServletResponse.SC_UNAUTHORIZED);
                }
            }
            contentAuthorizationService.performContentAuthorization(getResourcePathFromUri(uri),
                    slingHttpServletRequest, slingHttpServletResponse, true);
        } else {
            LOGGER.warn("ContentAuthorizationServlet doHead : Method called with no {} parameter",
                    ContentAuthorizationConstants.CONTENT_AUTHORIZATION_URI_QUERY_PARAMETER);
        }
    }

    /**
     * Get the resource path from the given URI.
     * 
     * @param uri
     *            {@link String}
     * @return {@link String}
     */
    private String getResourcePathFromUri(final String uri) {
        String extension = MadisonConstants.HTML_EXTN;
        if (uri.contains(MadisonConstants.DWNLD_PDF_SELECTOR)) {
            extension = MadisonConstants.DWNLD_PDF_SELECTOR;
        }
        final int htmlExtensionIndex = uri.indexOf(extension);
        return htmlExtensionIndex > 0 ? uri.substring(0, htmlExtensionIndex) : uri;
    }
}
