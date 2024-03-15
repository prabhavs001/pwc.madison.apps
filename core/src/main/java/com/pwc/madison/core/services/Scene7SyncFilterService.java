package com.pwc.madison.core.services;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Configuration to define and check scene7 sync filters.
 */
public interface Scene7SyncFilterService {

    String DM_CSS_FOLDER_PATH = "/content/dam/_CSS";
    String DM_DMSAMPLE_FOLDER_PATH = "/content/dam/_DMSAMPLE";

    /**
     * @return paths enabled for scene7 sync
     */
    String[] getSyncedPaths();

    /**
     * @return mime types enabled for scene 7 sync
     */
    String[] getSyncedMimeTypes();

    /**
     * Check if the given path is allowed to be synced with scene7 server and resolves to an asset with allowed mime type.
     * All files under {@value DM_CSS_FOLDER_PATH} and {@value DM_DMSAMPLE_FOLDER_PATH} are allowed;
     *
     * @param path             asset path (any rendition path will work.)
     * @param resourceResolver {@link ResourceResolver}
     * @return true if the asset is under the allowed path with allowed mime type, false otherwise
     * @see com.day.cq.dam.commons.util.DamUtil#resolveToAsset(Resource)
     */
    boolean isAssetAllowed(String path, ResourceResolver resourceResolver);

}
