package com.pwc.madison.core.models.impl;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.api.Rendition;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.dam.scene7.api.constants.Scene7Constants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.DynamicMediaAudioVideo;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.Objects;

@Model(adaptables = SlingHttpServletRequest.class, adapters = DynamicMediaAudioVideo.class)
public class DynamicMediaAudioVideoImpl implements DynamicMediaAudioVideo {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicMediaAudioVideo.class);

    /**
     * Scene7 URL for assets except image
     */
    private static final String CONTENT_URL = "is/content/";

    private static final String AUDIO_TYPE = "audio";
    private static final String BACKGROUND_IMAGE_NAME = "poster.png";

    @SlingObject
    private Resource currentResource;

    @ValueMapValue
    @Optional
    private String fileReference;

    @OSGiService
    private ResourceResolverFactory resourceResolverFactory;

    private boolean audioFile;
    private String s7PublishUrl;
    private String thumbnailPath;

    @PostConstruct
    protected void init() {
        ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory, MadisonConstants.MADISON_READ_SUB_SERVICE);
        if (StringUtils.isNotBlank(fileReference)) {
            Resource assetResource = resourceResolver.getResource(fileReference);
            if (Objects.isNull(assetResource)) {
                LOGGER.debug("Resource doesn't exist, invalid fileReference property {} on {}", fileReference, currentResource.getPath());
                return;
            }

            Asset asset = DamUtil.resolveToAsset(assetResource);
            if (Objects.isNull(asset)) {
                LOGGER.debug("fileReference property {} on {} doesn't points to a valid asset resource.", fileReference, currentResource.getPath());
                return;
            }

            String assetFormat = asset.getMetadataValueFromJcr(DamConstants.DC_FORMAT);
            audioFile = assetFormat.startsWith(AUDIO_TYPE);

            String scene7Domain = asset.getMetadataValueFromJcr(Scene7Constants.PN_S7_DOMAIN);
            String scene7Folder = asset.getMetadataValueFromJcr(Scene7Constants.PN_S7_FOLDER);
            s7PublishUrl = scene7Domain + CONTENT_URL + scene7Folder + asset.getName();

            Rendition assetBackgroundImageRendition = asset.getRendition(BACKGROUND_IMAGE_NAME);
            if (Objects.nonNull(assetBackgroundImageRendition)) {
                thumbnailPath = assetBackgroundImageRendition.getPath();
            }

            LOGGER.debug("Asset: {}, scene7 publish url: {}, thumbnailPath: {}", new Object[]{fileReference, s7PublishUrl, thumbnailPath});
        }
        resourceResolver.close();
    }

    @Override
    public boolean isAudioFile() {
        return audioFile;
    }

    @Override
    public String getS7PublishUrl() {
        return s7PublishUrl;
    }

    @Override
    public String getThumbnailPath() {
        return thumbnailPath;
    }
}
