package com.pwc.madison.core.reports;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;

import com.pwc.madison.core.constants.DITAConstants;

@Model(adaptables = SlingHttpServletRequest.class)
public class AssetTypeReportCellValue {

    private String assetType;

    @Inject
    @Optional
    private Resource result;

    @PostConstruct
    private void init() {
        assetType = StringUtils.EMPTY;
        if (result != null) {
            String path = result.getPath();
            if (path.endsWith(DITAConstants.DITA_EXTENSION)) {
                assetType = "Topic";
            } else if (path.endsWith(DITAConstants.DITAMAP_EXT)) {
                assetType = "DITA Map";
            } else {
                String extension = path.substring(path.lastIndexOf('.') + 1);
                assetType = extension.toUpperCase();
            }

        }

    }

    public String getAssetType() {
        return assetType;
    }

}
