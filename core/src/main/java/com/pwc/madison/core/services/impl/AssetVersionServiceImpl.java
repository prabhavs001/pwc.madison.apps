/**
 * 
 */
package com.pwc.madison.core.services.impl;

import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.asset.api.AssetException;
import com.adobe.granite.asset.api.AssetVersion;
import com.adobe.granite.asset.api.AssetVersionManager;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.AssetVersionService;

/**
 * @author kartikkarnayil
 *
 */
@Component(service = { AssetVersionService.class }, immediate = true)
public class AssetVersionServiceImpl implements AssetVersionService {

    /**
     * Logger Reference
     */
    private static final Logger LOG = LoggerFactory.getLogger(AssetVersionServiceImpl.class);

    @Override
    public Object getAssetVersion(final AssetVersionManager versionManager, final String assetPath) {

        // if path not empty and versionManger not null
        if (!StringUtils.isBlank(assetPath) && null != versionManager) {

            try {

                // get list of versions
                Iterator<? extends AssetVersion> versions = versionManager.listVersions(assetPath);

                while (null != versions && versions.hasNext()) {
                    AssetVersion version = versions.next();
                    String[] labels = version.getLabels();
                    if (null != labels && labels.length > 0) {

                        // iterate through labels
                        for (String label : labels) {

                            // return version number if the label is 'Published'
                            if (DITAConstants.DITA_DOCUMENTSTATE_DONE.equalsIgnoreCase(label)) {
                                return version.getName();
                            }
                        }
                    }
                }

            } catch (AssetException e) {
                LOG.error("Error while getting version number for asset", e);
            }
        }

        // if not version found, set version number as 'n/a'
        return MadisonConstants.NOT_AVAILABLE;
    }

}
