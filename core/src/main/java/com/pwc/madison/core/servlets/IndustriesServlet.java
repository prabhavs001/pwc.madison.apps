package com.pwc.madison.core.servlets;

import com.adobe.granite.crypto.CryptoSupport;
import com.day.cq.i18n.I18n;
import com.google.gson.Gson;
import com.pwc.madison.core.models.Preference;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.Constants;
import com.pwc.madison.core.userreg.enums.UserPreferencesType;
import com.pwc.madison.core.userreg.models.User;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.userreg.utils.UserRegUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.*;

/**
 * Servlet to return industries list for a user
 */
@Component(service = Servlet.class,
        property = {org.osgi.framework.Constants.SERVICE_DESCRIPTION + "=Servlet to get the list of industries for the user",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.paths=/bin/pwc-madison/getIndustries"})
public class IndustriesServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndustriesServlet.class);
    private static final String PATH = "path";
    private static final String CURRENT_PAGE_LOCALE_QUERY_PARAMTER = "currentPageLocale";
    private static final String CURRENT_PAGE_TERRITORY_CODE_QUERY_PARAMTER = "currentPageTerritoryCode";
    @Reference
    private CountryTerritoryMapperService countryTerritoryMapperService;

    @Reference
    private UserRegRestService userRegRestService;

    @Reference
    private CryptoSupport cryptoSupport;
    @Reference
    private XSSAPI xssapi;
    @Reference
    private UserPreferencesProviderService userPreferencesProviderService;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
        try {
            String territoryCode = request.getParameter(CURRENT_PAGE_TERRITORY_CODE_QUERY_PARAMTER);
            Map<String, Preference> industryPreferencesMap;
            String pageLocale = request.getParameter(CURRENT_PAGE_LOCALE_QUERY_PARAMTER);

            final Locale locale = new Locale(pageLocale);
            final ResourceBundle resourceBundle = request.getResourceBundle(locale);
            final I18n i18n = new I18n(resourceBundle);
            String industryListJson;
            List<Preference> industryList;

            if (null != madisonCookie) {
                final User user = UserInformationUtil.getUser(request, false, userRegRestService,
                        cryptoSupport, response, true, countryTerritoryMapperService,
                        userPreferencesProviderService, false, true, xssapi);

                if (user.isUserLoggedIn()) {
                    UserProfile userProfile = user.getUserProfile();
                    if (userProfile != null && (Objects.nonNull(userProfile.getTerritoryCode()) && StringUtils.isNotEmpty(userProfile.getTerritoryCode()))) {
                        territoryCode = (userProfile.getPrimaryTerritory()!=null && !userProfile.getPrimaryTerritory().isEmpty()) ?
                                userProfile.getPrimaryTerritory() : userProfile.getTerritoryCode() ;
                    }
                }

                industryPreferencesMap = userPreferencesProviderService.getPreferencesByTerritory(UserPreferencesType.INDUSTRY, territoryCode, pageLocale, i18n);
                if (industryPreferencesMap == null) {
                    industryPreferencesMap = userPreferencesProviderService.getPreferencesByTerritory(UserPreferencesType.INDUSTRY, Constants.FIRM_WIDE_CODE, pageLocale, i18n);
                }
            } else {
                industryPreferencesMap = userPreferencesProviderService.getPreferencesByTerritory(UserPreferencesType.INDUSTRY, Constants.FIRM_WIDE_CODE, pageLocale, i18n);
            }
            industryList = new ArrayList<Preference>(industryPreferencesMap.values());
            industryListJson = new Gson().toJson(industryList);
            response.setContentType(Constants.CONTENT_TYPE_JSON);
            response.setCharacterEncoding(Constants.UTF_8_ENCODING);
            response.setStatus(SlingHttpServletResponse.SC_OK);
            response.getWriter().write(industryListJson);
        } catch (Exception e) {
            LOGGER.error("Error while gettting Industries", e);
        }
    }
}
