/**
 *
 */
package com.pwc.madison.core.reports.columns;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.OutputPagesService;

/**
 *
 * Class to handle exporting of column data for CSV exporting.
 *
 */
@Model(adaptables = Resource.class)
public class OutputPagesCSVExporter implements ReportCellCSVExporter {

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String value;

    /**
     * OutputPagesService reference
     */
    @Inject
    private OutputPagesService outputPagesService;

    @Override
    public String getValue(final Object result) {

        String cellValue = StringUtils.EMPTY;

        // Depending on the column type value, return the value
        if (null != result && !StringUtils.isBlank(value)) {
            final Resource resource = (Resource) result;

            switch (value) {
                case MadisonConstants.TOPIC:
                    cellValue = String.valueOf(
                            outputPagesService.getTopicsCount(resource.getResourceResolver(), resource.getPath()));
                    break;
                case MadisonConstants.PAGE:
                    cellValue = String.valueOf(
                            outputPagesService.getNumberOfPages(resource.getResourceResolver(), resource.getPath()));
                    break;
                default:
                    cellValue = StringUtils.EMPTY;
                    break;
            }
        }
        return cellValue;
    }
}
