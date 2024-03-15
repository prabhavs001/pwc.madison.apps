package com.pwc.madison.core.services.impl;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.Scene7SyncFilterService;

@Component(service = Scene7SyncFilterService.class, immediate = true)
@Designate(ocd = Scene7SyncFilterServiceImpl.Scene7SyncFilterConfig.class)
public class Scene7SyncFilterServiceImpl implements Scene7SyncFilterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Scene7SyncFilterService.class);
    private String[] syncedPaths;
    private String[] syncedMimeTypes;

    @Activate
    public void activate(Scene7SyncFilterConfig config) {
        syncedPaths = config.scene7SyncedPaths();
        syncedMimeTypes = config.scene7SyncedMimeTypes();
    }

    @Override
    public String[] getSyncedPaths() {
        return syncedPaths;
    }

    @Override
    public String[] getSyncedMimeTypes() {
        return syncedMimeTypes;
    }

    @Override
    public boolean isAssetAllowed(String path, @Nonnull ResourceResolver resourceResolver) {
        if (StringUtils.isBlank(path)) {
            return false;
        }
        Resource assetResource = resourceResolver.getResource(path);
        return isAssetAllowed(assetResource);
    }

    /**
     * Check if the given resource is allowed to be synced with scene7 server and resolves to an asset with allowed mime type.
     * All files under {@value DM_CSS_FOLDER_PATH} and {@value DM_DMSAMPLE_FOLDER_PATH} are allowed;
     *
     * @param assetResource asset resource (any rendition path will work.)
     * @return true if the asset is under the allowed path with allowed mime type, false otherwise
     * @see com.day.cq.dam.commons.util.DamUtil#resolveToAsset(Resource)
     */
    public boolean isAssetAllowed(Resource assetResource) {
        if (Objects.nonNull(assetResource)) {
            String assetPath = assetResource.getPath();
            if (assetPath.startsWith(DM_CSS_FOLDER_PATH) || assetPath.startsWith(DM_DMSAMPLE_FOLDER_PATH)) {
                return true;
            }
            if (isPathAllowed(assetPath)) {
                Asset asset = DamUtil.resolveToAsset(assetResource);
                if (Objects.nonNull(asset)) {
                    String assetMimeType = asset.getMimeType();
                    if (isMimeTypeSupported(assetMimeType)) {
                        LOGGER.debug("Uploading {} with mime type {} to scene7", assetResource.getPath(), assetMimeType);
                        return true;
                    }
                }
                LOGGER.debug("Not uploading {} to scene7 server", assetResource.getPath());
            }
        }
        return false;
    }

    /**
     * Check if the path is allowed to sync with scene7 server.
     *
     * @param path asset path
     * @return true if path is allowed, false otherwise
     */
    public boolean isPathAllowed(String path) {
        if (StringUtils.isNotBlank(path)) {
            for (String syncedPath : syncedPaths) {
                if (path.startsWith(syncedPath)) {
                    return true;
                }
            }
        }
        LOGGER.debug("{} is not allowed in scene7 synced paths.", path);
        return false;
    }

    /**
     * Check if the mime type is allowed to sync with scene7 server.
     *
     * @param assetMimeType asset mime type
     * @return true if mime type is allowed, false otherwise
     */
    public boolean isMimeTypeSupported(String assetMimeType) {
        if (StringUtils.isNotBlank(assetMimeType)) {
            String assetType = assetMimeType.substring(0, assetMimeType.indexOf(MadisonConstants.FORWARD_SLASH)) + "/*";
            for (String mimeType : syncedMimeTypes) {
                if (assetMimeType.equalsIgnoreCase(mimeType) || assetType.equalsIgnoreCase(mimeType))
                    return true;
            }
        }
        LOGGER.debug("{} is not allowed in scene7 synced mime types.", assetMimeType);
        return false;
    }

    @ObjectClassDefinition(name = "PwC Viewpoint Scene7 Sync Filter Configuration")
    public @interface Scene7SyncFilterConfig {
        @AttributeDefinition(name = "Sync Paths", description = "Dam paths to be synced with scene7 server." +
            "All sub folders and files(with allowed mime types) will be synced.")
        String[] scene7SyncedPaths() default {};

        @AttributeDefinition(name = "Sync Mime Types", description = "Mime types to be synced with scene7 server. Mime types" +
            "filter is an add-on to path filtration. To support all types use /*, eg 'video/*'.")
        String[] scene7SyncedMimeTypes() default {};
    }
}
