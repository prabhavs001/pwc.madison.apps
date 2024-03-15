package com.pwc.madison.core.reports;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.pwc.madison.core.util.DITALinkUtils;
import com.pwc.madison.core.util.DITAUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;

/**
 * The Class SeeAlsoTopicPathCSVExporter is used to export the topic Path column into CSV.
 */
@Model(adaptables = {Resource.class})
public class SeeAlsoBrokenLinksPathCSVExporter implements ReportCellCSVExporter {


    private static final String CONTENT_DAM_PWC_MADISON = "/content/dam/pwc-madison";

	@Override
    public String getValue(Object result) {
        return null;
    }

    /**
     * gets the topic path
     * @param result
     * @param request
     * @return topic path
     */
    public String getSourcePathValue(Object result, SlingHttpServletRequest request){
        String path = StringUtils.EMPTY;
        List<String> topicsList = new ArrayList<>();
        if(null == result || null == request){
            return  path;
        }
        ResourceResolver resourceResolver = request.getResourceResolver();
        Resource currentResource = (Resource) result;
        if(null != currentResource) {
        	Resource  res = currentResource.getChild("jcr:content");
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
            		//String pagePath = DITAUtils.getPageFromXrefDita(item, resourceResolver, null);
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
        
        return StringUtils.join(filteredList, "\n");
    }

}
