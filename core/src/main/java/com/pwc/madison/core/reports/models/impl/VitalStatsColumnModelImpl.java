/**
 * 
 */
package com.pwc.madison.core.reports.models.impl;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.pwc.madison.core.beans.VitalStatsReportRow;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.reports.models.VitalStatsColumnModel;

/**
 * @author kartikkarnayil
 *
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = VitalStatsColumnModel.class)
public class VitalStatsColumnModelImpl implements VitalStatsColumnModel {

    @Self
    private SlingHttpServletRequest request;

    @ValueMapValue
    private String value;

    private String columnValue;

    private String assetPath;

    @PostConstruct
    private void init() {

        final VitalStatsReportRow row = (VitalStatsReportRow) request.getAttribute("result");

        if (null != row) {

            switch (value) {
                case MadisonConstants.ASSET_PATH_COLUMN:
                    assetPath = row.getAssetPath();
                    break;
                case MadisonConstants.TABLES_COLUMN:
                    columnValue = String.valueOf(row.getTables());
                    break;
                case MadisonConstants.WORDS_COLUMN:
                    columnValue = String.valueOf(row.getWords());
                    break;
                case MadisonConstants.PARAGRAPHS_COLUMN:
                    columnValue = String.valueOf(row.getParagraphs());
                    break;
                case MadisonConstants.ANCHORS_COLUMN:
                    columnValue = row.getAnchors();
                    break;
                case MadisonConstants.LINKS_COLUMN:
                    columnValue = String.valueOf(row.getLinks());
                    break;
                case MadisonConstants.DAMSHA_COLUMN:
                    columnValue = row.getDamSha1();
                    break;
                default:
                    columnValue = null;
                    break;
            }
        }
    }

    @Override
    public String getColumnValue() {
        return columnValue;
    }

    @Override
    public String getAssetPath() {
        return assetPath;
    }

}
