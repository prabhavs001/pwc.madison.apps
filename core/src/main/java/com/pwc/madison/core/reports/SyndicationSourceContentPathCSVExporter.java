package com.pwc.madison.core.reports;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.pwc.madison.core.constants.MadisonConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class SyndicationSourceContentPathCSVExporter is used to export the Source Content Path column into CSV.
 */
@Model(adaptables = {Resource.class})
public class SyndicationSourceContentPathCSVExporter implements ReportCellCSVExporter {

    private static final Logger log = LoggerFactory.getLogger(SyndicationSourceContentPathCSVExporter.class);


    private ResourceResolver resolver;

    /*
     * (non-Javadoc)
     * 
     * @see com.adobe.acs.commons.reports.api.ReportCellCSVExporter#getValue(java.lang.Object) This method returns the
     * Source syndication Content Path.
     */
    @Override
    public String getValue(Object result) {
        return null;
    }

    /**
     * gets syndication subscriber list and populates source syndicated content path
     * @param result
     * @param request
     * @return SourceSyndicatedContentPath
     */
    public String getSourcePathValue(Object result, SlingHttpServletRequest request){
        String path = StringUtils.EMPTY;
        if(null == result || null == request){
            return  path;
        }
        try {
            resolver = request.getResourceResolver();
            String[] subscriberLists = (String[]) request.getAttribute(MadisonConstants.SUBSCRIBER_LIST);
            String sourcePath = request.getParameter(MadisonConstants.SYNDICATION_INPUT_PATH);
            Resource currentResource = (Resource) result;
            if(null != subscriberLists){
                String currentResourcePath = currentResource.getPath();
                /* get source syndication content-path by replacing destination-path with source-path on current resource path */
                    for (String subscriber : subscriberLists) {
                        if (currentResourcePath.startsWith(subscriber)) {
                            String sourceResourcePath = currentResourcePath.replace(subscriber, sourcePath);
                            if (null != resolver.getResource(sourceResourcePath)) {
                                path = sourceResourcePath;
                                break;
                            }
                        }
                    }
            }
        }catch (SlingException | IllegalStateException | ClassCastException e) {
            log.error("SyndicationSourceContentPathCSVExporter - Error while exporting source syndication path: ", e);
        }
        return path;
    }

}
