package com.pwc.madison.core.services;

import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.annotation.versioning.ConsumerType;

import com.pwc.madison.core.models.Item;
import com.pwc.madison.core.userreg.models.UserProfile;


/**
 * This Service is used to filter the Home Page Content. 
 *
 */
@ConsumerType
public interface ContentFilterService {

	public <T extends Item> List<T> fetchFilteredContent(SlingHttpServletRequest request, UserProfile userProfile, List<T> itemsList, Class<T> type, String dateFormat);

	public boolean isValidContent(ValueMap pageProperties, UserProfile userProfile,boolean isItAFallBackItem);

	public <T extends Item> T addFilteredItem(T filteredItem, ValueMap pageProperties, T singleItem, String dateFormat);

	public <T extends Item> T fetchFallbackItems(T item, Class<T> type, String dateFormat);

	public <T extends Item> T addFeaturedFilteredItem(T filteredItem, ValueMap pageProperties, T singleItem, Resource ghostPropertiesResource, String dateFormat);
	
	public <T extends Item> T addFilteredHeroItem(T filteredItem, ValueMap pageProperties, T singleItem, String dateFormat) ;

}
