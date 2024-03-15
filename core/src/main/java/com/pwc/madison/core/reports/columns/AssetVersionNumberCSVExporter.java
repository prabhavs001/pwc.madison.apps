/**
 * 
 */
package com.pwc.madison.core.reports.columns;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.adobe.granite.asset.api.AssetVersionManager;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.AssetVersionService;

/**
 * @author kartikkarnayil
 * 
 *         CSV Exporter class for Asset version
 */
@Model(adaptables = Resource.class)
public class AssetVersionNumberCSVExporter implements ReportCellCSVExporter {

    /**
     * Resource Resolver Reference
     */
    @SlingObject
    private ResourceResolver resolver;

    /**
     * AssetVersionService reference
     */
    @Inject
    private AssetVersionService assetVersionService;

    @Override
    public String getValue(Object result) {
        String assetVersion = MadisonConstants.NOT_AVAILABLE;

        if (null != result) {
            Resource resource = (Resource) result;
            final String assetPath = resource.getPath();
            final AssetVersionManager versionManager = resolver.adaptTo(AssetVersionManager.class);
            assetVersion = String.valueOf(assetVersionService.getAssetVersion(versionManager, assetPath));
        }

        return assetVersion;
    }

}
