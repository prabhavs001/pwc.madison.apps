package com.pwc.madison.core.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;

import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.models.impl.Link;

public interface TocContentService {

	String getFullTocContentJson(SlingHttpServletRequest request, SlingHttpServletResponse response, int joinedLevel, boolean isJoinViewEnabled) throws IOException;
	
	void createChapterTocContentJson(ResourceResolver resolver, String basePath, List<Page> pageList, int joinedLevel, boolean isJoinViewEnabled);
	
	List<Link> getFullTocContent(ResourceResolver resolver, String basePath, int joinedLevel, boolean isJoinViewEnabled);
	
	String getChapterTocContentJson(SlingHttpServletRequest request, SlingHttpServletResponse response, int joinedLevel, boolean isJoinViewEnabled) throws IOException;

	void createAndUpdateJoinedSectionPage(ResourceResolver resolver, String basePath, int joinedLevel, Map<Integer, String> overrideJoinMap, boolean isJoinViewEnabled);
}
