package com.pwc.madison.core.reports;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class SyndicatedContentTerritoryCSVExporter is used to export the territory report column into CSV.
 */
@Model(adaptables = Resource.class)
public class SyndicatedContentTerritoryCSVExporter implements ReportCellCSVExporter {

    private static final Logger log = LoggerFactory.getLogger(SyndicatedContentTerritoryCSVExporter.class);

    /*
     * (non-Javadoc)
     * 
     * @see com.adobe.acs.commons.reports.api.ReportCellCSVExporter#getValue(java.lang.Object) This method returns the
     * Territory based on the the folder path.
     */
    @Override
    public String getValue(Object result) {
        String territory = StringUtils.EMPTY;
        if(null != result){
            try {
                Resource resource = (Resource) result;
                String path = resource.getPath();
                territory = MadisonUtil.getTerritoryCodeForPath(path);
            }catch (ClassCastException e){
                log.error("Error while exporting Syndicated Content Territory column : ", e);
            }
        }
        return territory;
    }
}
