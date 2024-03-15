package com.pwc.madison.core.reports;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

/**
 * The Class SeeAlsoTopicPathCSVExporter is used to export the topic Path column into CSV.
 */
@Model(adaptables = {Resource.class})
public class SeeAlsoTopicPathCSVExporter implements ReportCellCSVExporter {


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
        if(null == result || null == request){
            return  path;
        }
        Resource currentResource = (Resource) result;
        if(null != currentResource) {
        	path = currentResource.getPath();
        }
        return path;
    }

}
