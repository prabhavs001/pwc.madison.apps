package com.pwc.madison.core.models;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;

/**
 * Render condition that determines whether to render "Is publishing point" field on the metadata schema
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class PublishingPointButtonRenderCondition {

    public static final Logger LOGGER = LoggerFactory.getLogger(PublishingPointButtonRenderCondition.class);

    @Self
    private SlingHttpServletRequest request;

    private ResourceResolver requestResolver;

    private boolean isRendered = false;

    @OSGiService
    private ResourceResolverFactory resolverFactory;

    @PostConstruct
    protected void init() {
        String assetPath = StringUtils.EMPTY;

        requestResolver = request.getResourceResolver();
        RequestParameterMap requestParameterMap = request.getRequestParameterMap();
        if (requestParameterMap.containsKey(MadisonConstants.PN_ITEM)) {
            assetPath = requestParameterMap.get(MadisonConstants.PN_ITEM)[0].toString();
        }
        if (assetPath.endsWith(DITAConstants.DITAMAP_EXT)) {
            isRendered = true;
        }
        // set the render condition appropriately
        request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(isRendered));
    }

}
