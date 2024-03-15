package com.pwc.madison.core.models;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Render condition that renders the publish button only for publishing point ditamaps
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class PublishRenderCondition {

    public static final Logger LOGGER = LoggerFactory.getLogger(PublishRenderCondition.class);
    private static final String YES_TEXT = "yes";
    private static final String NO_TEXT = "no";

    @Self
    private SlingHttpServletRequest request;

    private boolean isRendered = false;

    @OSGiService
    private ResourceResolverFactory resolverFactory;

    @PostConstruct
    protected void init() {

        final ResourceResolver madisonServiceUserResolver = MadisonUtil.getResourceResolver(resolverFactory,
                MadisonConstants.MADISON_GENERIC_SUB_SERVICE);

        try {
            String assetPath = StringUtils.EMPTY;
            if (request.getPathInfo() != null) {
                final String fullPath = request.getPathInfo();
                assetPath = fullPath.substring(fullPath.indexOf(MadisonConstants.PWC_MADISON_DAM_BASEPATH));
            }
            if (!assetPath.endsWith(DITAConstants.DITAMAP_EXT)) {
                request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(false));
                return;
            }
            final Resource metadataResource = madisonServiceUserResolver
                    .getResource(assetPath + MadisonConstants.METADATA_PATH);

            if (metadataResource == null) {
                request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(false));
                return;
            }

            final ValueMap properties = metadataResource.adaptTo(ValueMap.class);
            if (properties == null) {
                request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(false));
                return;
            }
            final String isPublishingPoint = properties.containsKey(DITAConstants.PN_IS_PUBLISHING_POINTS)
                    ? properties.get(DITAConstants.PN_IS_PUBLISHING_POINTS).toString()
                    : NO_TEXT;
            if (YES_TEXT.equals(isPublishingPoint)) {
                isRendered = true;
            }

        } catch (final Exception e) {
            LOGGER.error("Error in evaluating render condition", e);
        } finally {
            // close the service user resolver
            if (madisonServiceUserResolver != null && madisonServiceUserResolver.isLive()) {
                madisonServiceUserResolver.close();
            }
        }
        // set the render condition appropriately
        request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(isRendered));
    }

}
