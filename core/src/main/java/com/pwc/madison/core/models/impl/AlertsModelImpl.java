package com.pwc.madison.core.models.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.Cookie;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.day.cq.search.QueryBuilder;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.Alerts;
import com.pwc.madison.core.models.AlertsModel;
import com.pwc.madison.core.services.AlertsTileService;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;

/**
 * Sling Model for Alerts Component
 *
 * @author Divanshu
 *
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = AlertsModel.class)
public class AlertsModelImpl implements AlertsModel {

	/** Default Logger */
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	@Inject
	SlingHttpServletRequest request;

	@Inject
	SlingHttpServletResponse response;

	@ChildResource(injectionStrategy = InjectionStrategy.OPTIONAL)
	private Resource alertItems;
	
	private List<String> alertItemsList;

	@Inject
	private CryptoSupport cryptoSupport;

	@Inject
	private UserRegRestService userRegRestService;

	@Inject
	private CountryTerritoryMapperService countryTerritoryMapperService;

	@Inject
	private transient UserPreferencesProviderService userPreferencesProviderService;

	@SlingObject
	ResourceResolver resourceResolver;

	@OSGiService
	private AlertsTileService alertsTileService;
	
    @OSGiService
    private XSSAPI xssapi;

	@Inject
	QueryBuilder queryBuilder;

	final List<Alerts> alertsList = new ArrayList<Alerts>();
	String UserType = "external";
	String dismissPagesCk;
	Boolean isContentAvailable = false;

	UserProfile userProfile = null;

	/** Init method for Alerts model */
	
	@PostConstruct
	private void init() {

		if(alertItems!=null) {
			alertItemsList=new ArrayList<String>();
			for(Resource resource : alertItems.getChildren()) {
				String alertItem = resource.getValueMap().get("articlePagePath", String.class);
				if(Objects.nonNull(alertItem)) {
					alertItemsList.add(alertItem);
				}
			}
		}
		try {
			final Cookie madisonCookie = UserInformationUtil.getMadisonUserProfileCookie(request);
			dismissPagesCk = alertsTileService.getAlertsCookie(request,MadisonConstants.DISMISS_PAGE_COOKIE_NAME);
			if (null != madisonCookie) {
				userProfile = UserInformationUtil.getUserProfile(request, cryptoSupport, true, userRegRestService,
						countryTerritoryMapperService, response, userPreferencesProviderService, true, false, xssapi);
				alertsTileService.filterAlertsList(alertItemsList, userProfile, alertsList, dismissPagesCk);
			} else {
				alertsTileService.filterAlertsList(alertItemsList, null, alertsList, dismissPagesCk);
			}
		} catch (final Exception e) {
			LOGGER.error(" AlertsModelImpl ::: init() :::  exception in alerts model :: {} ", e);
		}
	}

	@Override
	public Boolean getIsContentAvailable() {
		isContentAvailable = alertsTileService.getIsContentAvailable();
		return isContentAvailable;
	}

	@Override
	public List<Alerts> getAlertsList() {
		return alertsList;
	}

}
