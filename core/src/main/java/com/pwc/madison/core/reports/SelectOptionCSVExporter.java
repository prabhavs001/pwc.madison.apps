/**
 * 
 */
package com.pwc.madison.core.reports;

import javax.inject.Inject;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.adobe.acs.commons.reports.models.ReportCellValue;
import com.pwc.madison.core.services.ReportService;

// TODO: Auto-generated Javadoc
/**
 * An exporter for exporting formatted string values.
 */
@Model(adaptables = Resource.class)
public class SelectOptionCSVExporter implements ReportCellCSVExporter {

    /** Logger Reference. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SelectOptionCSVExporter.class);

    /** Property value. */
    @Inject
    private String property;

    /** Data Source value. */
    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String datasource;

    /** ResourceResolver reference. */
    @SlingObject
    private ResourceResolver resourceResolver;

    /** ReportService Reference. */
    @Inject
    private ReportService reportService;

    /* (non-Javadoc)
     * @see com.adobe.acs.commons.reports.api.ReportCellCSVExporter#getValue(java.lang.Object)
     */
    @Override
    public String getValue(Object result) {
        Resource resource = (Resource) result;
        ReportCellValue val = new ReportCellValue(resource, property);
        String cellValue = StringUtils.EMPTY;
        try {
            if (val.getValue() != null) {
                cellValue = getResourceValue(val);
            }
        } catch (RepositoryException e) {
            LOGGER.error("Error getting value", e);
        }
        return cellValue;
    }

    
    /**
     * Gets the resource value.
     *
     * @param val the val
     * @return the resource value
     * @throws RepositoryException the repository exception
     */
    private String getResourceValue(ReportCellValue val) throws RepositoryException {
        String cellValue;
        if (val.isArray() && ArrayUtils.isNotEmpty(val.getMultipleValues())) {
            cellValue = reportService.getOptionTextValue(resourceResolver, datasource, val.getMultipleValues());
        }
        else {
            // get Display Text for Select option using it's value
            cellValue = reportService.getOptionTextValue(resourceResolver, datasource,
                    new String[] { val.getSingleValue() });
          }
        return cellValue;
    }
}
