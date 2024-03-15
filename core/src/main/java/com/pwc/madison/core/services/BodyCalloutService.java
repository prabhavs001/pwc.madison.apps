package com.pwc.madison.core.services;

import com.pwc.madison.core.models.Item;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

public interface BodyCalloutService {

	String getBodyCalloutJson(SlingHttpServletRequest request, SlingHttpServletResponse response);

	Item getPageMetadata(Resource pageResource, String format);

	String getContentFieldValue(String contentId, String standardSetter, String PwcAuthored, String contentType);
}
