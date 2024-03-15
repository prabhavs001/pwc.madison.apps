/**
 * 
 */
package com.pwc.madison.core.reports.columns;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.pwc.madison.core.beans.FileManagementReportRow;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;

/**
 * This CSV exporter reads the value from request attribute and based on the selection of dropdown value fetches the
 * results from request object and export to a column in an Spread sheet.
 */
@Model(adaptables = Resource.class)
public class FileManagementReportCSVExporter implements ReportCellCSVExporter {

    /**
     * Resource Resolver Reference
     */
    @SlingObject
    private ResourceResolver resolver;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String value;

    @Override
    public String getValue(Object result) {
        return null;
    }

    public String populateRowValue(Object result, SlingHttpServletRequest request) {
        List<String> referenceList = null;
        String rowValue = StringUtils.EMPTY;
        if (null!=result && null != request && null != request.getAttribute("reportMap") && null != value) {
            Map<String, FileManagementReportRow> reportMap = (Map<String, FileManagementReportRow>) request
                    .getAttribute("reportMap");
            Resource res = (Resource) result;
                    FileManagementReportRow eachRow = reportMap.get(res.getPath());
                    switch (value) {
                        case MadisonConstants.AEM_PAGES:
                            referenceList = eachRow.getAssetPath();
                            break;
                        case MadisonConstants.ANCHORS_COLUMN:
                            referenceList = eachRow.getReferenceUrls();
                            break;
                        case DITAConstants.STATUS_PROP_NAME:
                            referenceList = eachRow.getPageStatus();
                            break;
                        default:
                            referenceList = null;
                            break;
                    }

                    if (CollectionUtils.isNotEmpty(referenceList)) {
                        rowValue = StringUtils.join(referenceList, ";");
                    } else {
                        rowValue = StringUtils.EMPTY;
                    }
        }
        return rowValue;
    }
}
