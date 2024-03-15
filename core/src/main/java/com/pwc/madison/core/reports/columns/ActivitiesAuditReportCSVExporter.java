/**
 * 
 */
package com.pwc.madison.core.reports.columns;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.pwc.madison.core.beans.AuditReportRow;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;

/**
 * This CSV exporter reads the value from request attribute and based on the selection of dropdown value fetches the
 * results from request object and export to a column in an Spread sheet.
 */
@Model(adaptables = Resource.class)
public class ActivitiesAuditReportCSVExporter implements ReportCellCSVExporter {

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String value;

    @Override
    public String getValue(Object result) {
        String activityFieldValue = StringUtils.EMPTY;
        if (null != result && null != value) {
            final AuditReportRow eachRow = (AuditReportRow) result;
            switch (value) {
                case MadisonConstants.TITLE_NODE:
                    activityFieldValue = eachRow.getTitle();
                    break;
                case DITAConstants.PATH_PROP_NAME:
                    activityFieldValue = eachRow.getPath();
                    break;
                case MadisonConstants.PN_USERS:
                    activityFieldValue = eachRow.getCreatedBy();
                    break;
                case MadisonConstants.INPUT_DATE:
                    activityFieldValue = eachRow.getModifiedDate();
                    break;
                case MadisonConstants.ACTIVITY:
                    activityFieldValue = eachRow.getActivity();
                    break;
                default:
                    activityFieldValue = null;
                    break;
            }
        }
        return activityFieldValue;
    }
}
