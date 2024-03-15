package com.pwc.madison.core.services;

import java.util.List;
import javax.servlet.http.Cookie;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.json.JSONArray;

import com.pwc.madison.core.models.Alerts;
import com.pwc.madison.core.userreg.models.UserProfile;

/**
 * Alert Tile Service Interface
 *
 * @author Divanshu
 *
 */
public interface AlertsTileService {
	
	
	/**
	 * Method to get value of isContentAvaible
	 *  
	 * */
	public Boolean getIsContentAvailable();
	
	/**
	 * Method to get Results from content after query from AEM
	 * 
	 * @param alertItemsList
	 *            {@link String[]} of alertItems
	 * @param userProfile
	 *            {@link UserProfile} get from cookie
	 * @param titleList
	 *            {@link List<Alerts>} of titles
	 * @param dismissPagesCk
	 *            {@link Cookie} dismissed pages cookie
	 * @param readAlertsCk
	 *            {@link Cookie} Read Alerts cookie
	 * @return {@link Void}
	 */
	public void filterAlertsList(final List<String> alertItemsList, final UserProfile userProfile,
			final List<Alerts> alertsList, final String dismissPagesCk);

	/**
	 * Method to set Dismissed Pages Cookie to Client Side
	 *
	 * @param request           {@link SlingHttpServletRequest}
	 * @param response          {@link SlingHttpServletResponse}
	 * @param cookieExpiryHours {@link Integer} Time for which the cookie will be set
	 * @param cookieName        {@link String} Name of the cookie
	 * @param cookieValue       {@link String} value of the cookie
	 * @param refererPage		{@link String} referer Page relative Path
	 */
	public void setPageCookie(final SlingHttpServletResponse response, final int cookieExpiryHours,final String cookieName,final JSONArray cookieValue,
			String refererPage);

	/**
	 * Method to get Cookie from client side
	 *
	 * @param request
	 *            {@link SlingHttpServletRequest}
	 *            
	 * @param cookieName
	 *            {@link String}
	 */
	public String getAlertsCookie(final SlingHttpServletRequest request, final String cookieName);
}
