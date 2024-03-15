package com.pwc.madison.core.models;

import java.util.GregorianCalendar;

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
 * Render condition that determines whether to render the unpublish button for a ditamap
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class UnpublishRenderCondition {

    public static final Logger LOGGER = LoggerFactory.getLogger(UnpublishRenderCondition.class);
    private static final String YES_TEXT = "yes";
    private static final String NO_TEXT = "no";
    private static final String LAST_PUBLISHED = "fmdita-lastPublished";

    @Self
    private SlingHttpServletRequest request;

    private ResourceResolver madisonServiceUserResolver;

    private boolean isRendered = false;

    @OSGiService
    private ResourceResolverFactory resolverFactory;

    @PostConstruct
    protected void init() {

        // get the service resource resolver for reading the users under /home etc
        madisonServiceUserResolver = MadisonUtil.getResourceResolver(resolverFactory,
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
            if (!YES_TEXT.equals(isPublishingPoint)) {
                request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(false));
                return;
            }
            final Resource aemSiteResource = madisonServiceUserResolver
                    .getResource(assetPath + DITAConstants.AEMSITE_PRESETS_NODE);
            if (aemSiteResource == null) {
                request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(false));
                return;
            }
            final ValueMap presetProperties = aemSiteResource.adaptTo(ValueMap.class);
            if (presetProperties == null) {
                request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(false));
                return;
            }
            if (presetProperties.containsKey(LAST_PUBLISHED)) {
                isRendered = true;
            }
            if (properties.containsKey(DITAConstants.PN_METADATA_LAST_UNPUBLISHED)) {
                final GregorianCalendar lastUnpublishedDate = (GregorianCalendar) properties
                        .get(DITAConstants.PN_METADATA_LAST_UNPUBLISHED);
                final GregorianCalendar lastPublishedDate = (GregorianCalendar) presetProperties.get(LAST_PUBLISHED);
                if (lastPublishedDate.before(lastUnpublishedDate)) {
                    isRendered = false;
                }
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
