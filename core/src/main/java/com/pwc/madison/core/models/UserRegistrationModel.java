package com.pwc.madison.core.models;

import javax.annotation.PostConstruct;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import com.day.cq.commons.LanguageUtil;
import com.pwc.madison.core.authorization.constants.ContentAuthorizationConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.services.ExternalRedirectDomainConfigurationService;
import com.pwc.madison.core.userreg.services.UserRegPagesPathProvidesService;
import com.pwc.madison.core.util.MadisonUtil;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * This model adapts SlingHttpServletRequest and returns locale present in request parameter and home page path
 * corresponding to it.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class UserRegistrationModel {

    private static final String TRUE = "true";

	public static final Logger LOGGER = LoggerFactory.getLogger(UserRegistrationModel.class);
    public static final String RESTRICTED_ACCESS_MODAL_COMPONENT_RESOURCE_TYPE = "pwc-madison/components/content/restricted-access-modal";

    @OSGiService
    private CryptoSupport cryptoSupport;
    
    @OSGiService
    private UserRegPagesPathProvidesService userRegPagesPathProvidesService;
    
    @OSGiService
    private CountryTerritoryMapperService countryTerritoryMapperService;
    
    @OSGiService
	private ExternalRedirectDomainConfigurationService externalRedirectDomainConfigurationService;

    @Self
    private SlingHttpServletRequest slingRequest;
    
    @ValueMapValue @Optional @Named("showButton")
    private String showButton;
    
    private String homePagePath;

    private String locale;
    
    private String localeForHomePagePath;

    private String referrer;
    
    private String refererHeader;
    
    private Boolean externalRedirectDomain;

    @OSGiService
    private XSSAPI xssAPI;
    
    @PostConstruct
    protected void init() {
        localeForHomePagePath = MadisonUtil.getLocaleOfCurrentPage(userRegPagesPathProvidesService, countryTerritoryMapperService, slingRequest);
        homePagePath = MadisonUtil.getHomePageFromLocale(localeForHomePagePath);
        if(localeForHomePagePath.equalsIgnoreCase(MadisonConstants.ENGLISH_INT_LOCALE)) {
            locale = MadisonConstants.ENGLISH_INT_LOCALE;
        }else {
            locale = String.valueOf(LanguageUtil.getLocale(localeForHomePagePath));
        }
        if (locale.equals("null"))
        	locale = MadisonConstants.ENGLISH_LOCALE;
        externalRedirectDomain = false;
        refererHeader = slingRequest
				.getParameter(ContentAuthorizationConstants.CONTENT_AUTHORIZATION_REFERER_HEADER_QUERY_PARAMETER);
        if (refererHeader != null) {
        	try {
        		refererHeader = cryptoSupport.unprotect(refererHeader);
        		final String[] externalDomains = externalRedirectDomainConfigurationService.getDomainList();
        		for (String externalDomain : externalDomains) {
        			if (refererHeader.startsWith(externalDomain)) {
        				externalRedirectDomain = true;
        				break;
        			}
        		}
        	} catch (CryptoException cryptoException) {
        		LOGGER.error("UserRegistrationModel :  Crypto Exception {} occured while decrypting refererHeader {}",
        				cryptoException, xssAPI.encodeForHTML(refererHeader));
        	}
        }
    }

    public String getHomePagePath() {
        return homePagePath;
    }
    
    public String getLocale() {
        return locale;
    }

    public String getReferrer() {
        referrer = slingRequest.getParameter(MadisonConstants.REFERRER_QUERY_PARAM);
        String unprotectedReferrer = referrer;
        try {
            unprotectedReferrer = unprotectedReferrer == null ? unprotectedReferrer : cryptoSupport.unprotect(unprotectedReferrer);
        } catch (CryptoException cryptoException) {
            LOGGER.error("UserRegistrationModel :  init() : Crypto Exception {} occured while encrypting referrer {}",
                    cryptoException, xssAPI.encodeForHTML(unprotectedReferrer));
        }
        return unprotectedReferrer;
    }
    
    public String getI18nKeyForTncContent() {
        return MadisonConstants.USERREG_TNC_CONTENT + locale.toUpperCase();
    }
    
    public String getI18nKeyForAcknowledgeText() {
        return MadisonConstants.USERREG_REGISTER_ACKNOWLEDGE_TEXT + locale.toUpperCase();
    }
    
    public String getShowButton() {
        return showButton;
    }

    public String getRefererHeader() {
        String referrerUrl = "";
        if (StringUtils.isEmpty(refererHeader)) {
            referrerUrl = homePagePath;
        } else {
            try {
                URL requestURL = new URL(slingRequest.getRequestURL().toString());
                URL referrerURL = new URL(refererHeader);
                if (StringUtils.equals(requestURL.getHost(), referrerURL.getHost())) {
                    referrerUrl = refererHeader;
                } else {
                    referrerUrl = homePagePath;
                }
            } catch (MalformedURLException malformedURLException) {
                LOGGER.error("UserRegistrationModel :  init() : MalformedURLException {} occurred forming URL for referrer {}",
                        malformedURLException, refererHeader);
            }
        }
        return referrerUrl;
    }
    
    public Boolean getExternalRedirectDomain() {
		return externalRedirectDomain;
	}
    
    public String getAccessType() {
        return slingRequest.getParameter(ContentAuthorizationConstants.CONTENT_AUTHORIZATION_ACCESS_TYPE_QUERY_PARAMETER);
    }
    
    public boolean isInternalOnly() {
    	String internalOnly = slingRequest.getParameter(ContentAuthorizationConstants.CONTENT_AUTHORIZATION_IS_INTERNAL_ONLY_QUERY_PARAMETER);
        return StringUtils.isNotBlank(internalOnly) && TRUE.equals(internalOnly);
    }

}
