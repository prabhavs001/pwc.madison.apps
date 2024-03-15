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
import com.pwc.madison.core.beans.VitalStatsReportRow;
import com.pwc.madison.core.constants.MadisonConstants;

/**
 * @author kartikkarnayil
 * 
 *         Class to handle exporting of column data for CSV exporting.
 *
 */
@Model(adaptables = Resource.class)
public class PWCVitalStatsCSVExporter implements ReportCellCSVExporter {

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String value;

    @Override
    public String getValue(Object result) {

        String cellValue = StringUtils.EMPTY;

        // Depending on the column type value, return value out of vital stat
        if (null != result && !StringUtils.isBlank(value)) {
            final VitalStatsReportRow vitalStatObj = (VitalStatsReportRow) result;
            switch (value) {
                case MadisonConstants.ASSET_PATH_COLUMN:
                    cellValue = vitalStatObj.getAssetPath();
                    break;
                case MadisonConstants.WORDS_COLUMN:
                    cellValue = String.valueOf(vitalStatObj.getWords());
                    break;
                case MadisonConstants.PARAGRAPHS_COLUMN:
                    cellValue = String.valueOf(vitalStatObj.getParagraphs());
                    break;
                case MadisonConstants.TABLES_COLUMN:
                    cellValue = String.valueOf(vitalStatObj.getTables());
                    break;
                case MadisonConstants.LINKS_COLUMN:
                    cellValue = String.valueOf(vitalStatObj.getLinks());
                    break;
                case MadisonConstants.ANCHORS_COLUMN:
                    cellValue = vitalStatObj.getAnchors();
                    break;
                case MadisonConstants.DAMSHA_COLUMN:
                    cellValue = vitalStatObj.getDamSha1();
                    break;
                default:
                    cellValue = StringUtils.EMPTY;
                    break;
            }
        }
        return cellValue;
    }
}
