package com.pwc.madison.core.reports;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.xss.XSSAPI;

import com.pwc.madison.core.util.DITALinkUtils;
import com.pwc.madison.core.util.DITAUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.Session;

@Model(adaptables = SlingHttpServletRequest.class)
public class SeeAlsoBrokenLinksCellValue {

    private static final String CONTENT_DAM_PWC_MADISON = "/content/dam/pwc-madison";

	private String brokenLinks;
	
	private List<String> brokenLinksList;

    @Self
    private SlingHttpServletRequest request;

    @Inject
    @Optional
    private Resource result;
    
    @OSGiService
    private XSSAPI xssAPI;

    @PostConstruct
    private void init() {
    	brokenLinks = StringUtils.EMPTY;
    	
        List<String> topicsList = new ArrayList<>();
        if (result != null) {
            String topicPath = result.getPath();
            ResourceResolver resourceResolver = request.getResourceResolver();
            if(StringUtils.isNotBlank(topicPath)) {
            	Resource  res = resourceResolver.getResource(topicPath + "/jcr:content");
            	if(null != res && !ResourceUtil.isNonExistingResource(res)) {
            		ValueMap contentValueMap = res.adaptTo(ValueMap.class);
            		if(contentValueMap.containsKey("fmditaXrefs")) {
            			String[] includedTopicRefsArray = contentValueMap.get("fmditaXrefs", String[].class);
            			if(null !=  includedTopicRefsArray && ArrayUtils.isNotEmpty(includedTopicRefsArray)) {
            				for(String item : includedTopicRefsArray) {
            					if(StringUtils.isNotEmpty(item)) {
            						topicsList.add(item.replace(",", StringUtils.EMPTY).trim());
            					}
            				}
            			}
            		}
            	}
            }
            // Filter out existing links
            List<String> filteredList = topicsList.stream().filter(item -> {
				boolean isExternalOrBrokenLink = true;
				if (item.contains(CONTENT_DAM_PWC_MADISON)) {
					Resource  res = resourceResolver.getResource(item);
	            	if(null != res && !ResourceUtil.isNonExistingResource(res)) {
	            		isExternalOrBrokenLink = false;
	            	}
	            	if(!isExternalOrBrokenLink) {
	            		String pageUrl = DITALinkUtils.getUpdatedXrefLink(resourceResolver.adaptTo(Session.class), item);
	            		//String pagePath = DITAUtils.getPageFromXrefDita(item, resourceResolver, xssAPI);
	            		if(StringUtils.isBlank(pageUrl) || null == resourceResolver.getResource(pageUrl.split(".html")[0])) {
	            			isExternalOrBrokenLink = true;
		            	}
	            	}
				} else {
					// External links case
					isExternalOrBrokenLink = false;
				}
				return isExternalOrBrokenLink;
			}).collect(Collectors.toList());
            
            brokenLinksList = filteredList;
            brokenLinks = StringUtils.join(filteredList, ",");
        }
    }

    public String getBrokenLinks() {
        return brokenLinks;
    }

    public List<String> getBrokenLinksList() {
        return brokenLinksList;
    }
}
