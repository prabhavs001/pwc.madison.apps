package com.pwc.madison.core.models;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameterMap;
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
import com.day.cq.commons.jcr.JcrConstants;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;

/**
 * Render condition that determines whether to render "Edit Expiry Date" field on the metadata schema
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class EditExpiryDateButtonRenderCondition {

    public static final Logger LOGGER = LoggerFactory.getLogger(EditExpiryDateButtonRenderCondition.class);

    @Self
    private SlingHttpServletRequest request;

    private ResourceResolver requestResolver;

    private ResourceResolver madisonServiceUserResolver;

    private boolean isRendered = true;

    @OSGiService
    private ResourceResolverFactory resolverFactory;

    @PostConstruct
    protected void init() {
        String topicPath = StringUtils.EMPTY;

        requestResolver = request.getResourceResolver();
        RequestParameterMap requestParameterMap = request.getRequestParameterMap();
        if (requestParameterMap.containsKey(MadisonConstants.PN_ITEM)) {
            topicPath = requestParameterMap.get(MadisonConstants.PN_ITEM)[0].toString();
        }
        Resource topicRes = requestResolver.getResource(topicPath + "/" + JcrConstants.JCR_CONTENT);
        if (null != topicRes) {
            ValueMap valueMap = topicRes.getValueMap();
            if (valueMap.containsKey(DITAConstants.PN_FMDITA_CONREF)) {
                String[] conrefs = valueMap.get(DITAConstants.PN_FMDITA_CONREF,String[].class);
                if(conrefs.length>0){
                    isRendered = false;
                }
            }
        }
        // set the render condition appropriately
        request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(isRendered));
    }

}
