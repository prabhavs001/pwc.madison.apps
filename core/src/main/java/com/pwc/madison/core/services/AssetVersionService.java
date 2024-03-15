/**
 * 
 */
package com.pwc.madison.core.services;

import com.adobe.granite.asset.api.AssetVersionManager;

/**
 * @author kartikkarnayil
 *
 */
public interface AssetVersionService {

    /**
     * Method to get Asset version for input Asset Path
     * 
     * @param versionManager
     *            version manager service
     * @param assetPath
     *            asset path
     * @return version number
     */
    Object getAssetVersion(AssetVersionManager versionManager, String assetPath);

}
