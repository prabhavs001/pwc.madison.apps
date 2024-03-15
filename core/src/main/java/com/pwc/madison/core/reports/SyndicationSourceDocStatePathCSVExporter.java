package com.pwc.madison.core.reports;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.DITAUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;

/**
 * The Class SyndicationSourceContentPathCSVExporter is used to export the Subscriber DocumentState column into CSV.
 */
@Model(adaptables = {Resource.class})
public class SyndicationSourceDocStatePathCSVExporter implements ReportCellCSVExporter {

    private static final Logger log = LoggerFactory.getLogger(SyndicationSourceDocStatePathCSVExporter.class);


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
     * gets syndication subscriber list and populates source Document-State
     * @param result
     * @param request
     * @return docState
     */
    public String getDocStateValue(Object result, SlingHttpServletRequest request){
        String docState = StringUtils.EMPTY;
        if(null == result || null == request){
            return  docState;
        }
        try {
            ResourceResolver resolver = request.getResourceResolver();
            Session session = resolver.adaptTo(Session.class);
            String[] subscriberLists = (String[]) request.getAttribute(MadisonConstants.SUBSCRIBER_LIST);
            String inputPath = request.getParameter(MadisonConstants.SYNDICATION_INPUT_PATH);
            String currentResourcePath = ((Resource)result).getPath();
            String sourcePath = DITAUtils.getSourcePath(currentResourcePath, subscriberLists, resolver, inputPath);
            docState = DITAUtils.getDocState(sourcePath, session);
        }catch (SlingException | IllegalStateException | ClassCastException e) {
            log.error("SyndicationSourceContentPathCSVExporter - Error while exporting source document-state: ", e);
        }
        return docState;
    }

}
