package com.pwc.madison.core.reports;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.pwc.madison.core.constants.DITAConstants;

/**
 * The Class AssetTypeReportCellCSVExporter is used to export the asset type report column into CSV.
 */
@Model(adaptables = Resource.class)
public class AssetTypeReportCellCSVExporter implements ReportCellCSVExporter {

    /*
     * (non-Javadoc)
     * 
     * @see com.adobe.acs.commons.reports.api.ReportCellCSVExporter#getValue(java.lang.Object) This method returns the
     * asset type based on the extension of the asset.
     */
    @Override
    public String getValue(Object result) {
        Resource resource = (Resource) result;
        if (resource != null) {
            String path = resource.getPath();
            if (path.endsWith(DITAConstants.DITA_EXTENSION)) {
                return "Topic";
            } else if (path.endsWith(DITAConstants.DITAMAP_EXT)) {
                return "DITA Map";
            } else {
                String extension = path.substring(path.lastIndexOf('.') + 1);
                return extension.toUpperCase();
            }

        }
        return StringUtils.EMPTY;

    }

}
