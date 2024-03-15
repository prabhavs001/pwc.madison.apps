package com.pwc.madison.core.models;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.designer.Style;

@Model(adaptables = SlingHttpServletRequest.class)
public class HeaderModel {

    private static final Logger LOG = LoggerFactory.getLogger(HeaderModel.class);

    private static final Object TITLE_TEXT = "title";
    private static final Object LOGO_IMAGE = "logoImage";
    private static final Object LINK_URL = "linkURL";
    private static final Object ALT_TEXT = "altText";

    @ScriptVariable
    private Style currentStyle;

    @SlingObject
    Resource currentResource;

    private String title;
    private String logoImgPath;
    private String linkUrl;
    private String altText;

    @PostConstruct
    public void init() {
        final ValueMap properties = currentResource.getValueMap();
        LOG.debug("properties {}", properties);
        LOG.debug("currentStyle {}", currentStyle);
        title = properties.containsKey(TITLE_TEXT) ? (String) properties.get(TITLE_TEXT)
                : (String) currentStyle.get(TITLE_TEXT);
        logoImgPath = properties.containsKey(LOGO_IMAGE) ? (String) properties.get(LOGO_IMAGE)
                : (String) currentStyle.get(LOGO_IMAGE);
        linkUrl = properties.containsKey(LINK_URL) ? (String) properties.get(LINK_URL)
                : (String) currentStyle.get(LINK_URL);
        altText = properties.containsKey(ALT_TEXT) ? (String) properties.get(ALT_TEXT)
                : (String) currentStyle.get(ALT_TEXT);

    }

    public String getTitle() {
        return title;
    }

    public String getLogImgPath() {
        return logoImgPath;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public String getAltText() {
        return altText;
    }

}
