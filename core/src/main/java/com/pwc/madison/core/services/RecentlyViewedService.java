package com.pwc.madison.core.services;

import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ConsumerType;

import com.pwc.madison.core.models.RecentlyViewedItem;
import com.pwc.madison.core.userreg.models.UserProfile;

/**
 * The Interface RecentlyViewedService provides the page links from Spring Boot Services
 *
 */
@ConsumerType
public interface RecentlyViewedService {

	/**
	 * Returns {@link boolean} response for each page the user visits and map it to that user.
	 * 
	 * @param user
	 * 			{@link UserProfile}
	 * @param itemPath
	 * 			{@link String} Item Page Path
	 * @param token
	 * 			{@link String} to authenticate request
	 */
	public boolean addRecentlyViewedItem(final UserProfile user, final String contentPath, final String token);

	/**
	 * Returns {@link List<RecentlyViewiedItem>} of the recently viewed items for the particular user.
	 * 
	 * @param request 
	 * 			{@link SlingHttpServletRequest}
	 * @param user 
	 * 			{@link UserProfile}
	 * @param token 
	 * 			{@link String} to authenticate request
	 * @param resourceResolver 
	 * 
	 * @return {@link List<RecentlyViewedItem>}
	 */
    public List<RecentlyViewedItem> getRecentlyViewedItem(final SlingHttpServletRequest request, UserProfile user, final String token, ResourceResolver resourceResolver);
    
    /**
	 * This method is consumed by the Page Expiration Workflow to inser the 
	 * recently viewed items from all the users to temp table in sql.
	 * 
	 * @param contentPath
	 * 			{@link String}
	 * 
	 * @return
	 * 		{@link Boolean}
	 */
    public Boolean insertRecentlyViewedItemInTempTable(final List<String> contentPathList);
    /**
	 * This method is consumed by the Page Expiration Workflow to remove the 
	 * recently viewed items from all the users.
	 * 
	 */
    public Boolean removeRecentlyViewedItemFromTempTable();
    

}
