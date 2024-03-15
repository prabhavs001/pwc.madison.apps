/**
 * 
 */
package com.pwc.madison.core.reports.models.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.jcr.RepositoryException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.RequestAttribute;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.pwc.madison.core.beans.FileManagementReportRow;
import com.pwc.madison.core.reports.models.FileManagementReport;

@Model(adaptables = SlingHttpServletRequest.class, adapters = FileManagementReport.class)
public class FileManagementReportModelImpl implements FileManagementReport {

    /**
     * Request Attribute result
     */
    @RequestAttribute
    private Resource result;

    /**
     * ReourceResolver reference
     */
    @SlingObject
    private ResourceResolver resourceResolver;

    @Self
    private SlingHttpServletRequest request;

    @ValueMapValue
    private String value;

    /**
     * Page References of Asset
     */
    private FileManagementReportRow eachRow;

    /**
     * Init Method
     * 
     * @throws RepositoryException
     */
    @PostConstruct
    private void init() {
        List<String> referenceList = null;
        if (null != request && null != request.getAttribute("reportMap")) {
            Map<String, FileManagementReportRow> reportMap = (Map<String, FileManagementReportRow>) request
                    .getAttribute("reportMap");
            if (null != reportMap && null != value) {
                eachRow = reportMap.get(result.getPath());
            }
        }
    }

    /**
     * @return the eachRow
     */
    public FileManagementReportRow getEachRow() {
        return eachRow;
    }
}
