package com.pwc.madison.core.servlets;

import java.io.IOException;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.services.TocContentService;

/**
 * Servlet class to fetch toc json
 */
@Component(
    immediate = false,
    service = Servlet.class,
    property = { "sling.servlet.resourceTypes=cq:Page", "sling.servlet.methods=GET", "sling.servlet.selectors=fetchtoc",
            "sling.servlet.extensions=json" })
public class TOCServlet extends SlingAllMethodsServlet {

    private static final String YES = "yes";
	private static final String JCR_CONTENT = "/jcr:content";
	private static final String JCR_CONTENT_TOC = "/jcr:content/toc";
	private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(TOCServlet.class);
    @Reference
    private transient TocContentService tocContentService;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {
    	boolean isChapterToc = false;
    	boolean isJoinViewEnabled = false;
        int joinedLevel = 0;
    	final ResourceResolver resolver = request.getResourceResolver();
    	final String basePagePath = request.getResource().getPath();
    	Resource baseContentResource = getTocNodeResource(resolver, basePagePath);
    	if(null != baseContentResource && !ResourceUtil.isNonExistingResource(baseContentResource)) {
    		ValueMap valueMap = baseContentResource.adaptTo(ValueMap.class);
        	if(YES.equals(valueMap.get(DITAConstants.META_LOAD_LITE_TOC, String.class))) {
        		isChapterToc = true;
        	}
        	if (YES.equals(valueMap.get(DITAConstants.META_JOINED_SECTION_TOC, String.class))) {
        		isJoinViewEnabled = true;
				joinedLevel = valueMap.get(DITAConstants.META_JOINED_SECTION_LEVEL, Integer.class);
			}
    	}
    	if(isChapterToc) {
    		LOGGER.debug("Chapter TOC is enabled");
    		tocContentService.getChapterTocContentJson(request, response, joinedLevel, isJoinViewEnabled);
    	} else {
    		LOGGER.debug("Full TOC is enabled");
    		tocContentService.getFullTocContentJson(request, response, joinedLevel, isJoinViewEnabled);
    	}
    }

    private Resource getTocNodeResource(ResourceResolver resolver, String basePagePath) {
    	Resource resource = resolver.getResource(basePagePath+JCR_CONTENT_TOC);
    	if(null != resource && !ResourceUtil.isNonExistingResource(resource)) {
    		return resolver.getResource(basePagePath+JCR_CONTENT);
    	} else {
    		Resource baseResource = resolver.getResource(basePagePath);
    		return getTocNodeResource(resolver, baseResource.getParent().getPath());
    	}
	}

}
