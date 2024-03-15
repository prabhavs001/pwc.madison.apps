package com.pwc.madison.core.services;

import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.annotation.versioning.ConsumerType;

import com.pwc.madison.core.models.InsightsHomePageTile;
import com.pwc.madison.core.userreg.models.UserProfile;


/**
 * This Service is used to filter the Home Page Content. 
 *
 */
@ConsumerType
public interface InsightsHomePageService {

	public List<InsightsHomePageTile> fetchFilteredContent(SlingHttpServletRequest request, UserProfile userProfile, List<InsightsHomePageTile> insightsItems, String dateFormat);

	public InsightsHomePageTile fetchFallbackItems(InsightsHomePageTile featuredContentItem,UserProfile userProfile, String dateFormat);

}
