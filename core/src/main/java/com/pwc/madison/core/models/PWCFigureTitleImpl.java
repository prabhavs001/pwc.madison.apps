package com.pwc.madison.core.models;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.models.impl.PWCTopicHeaderImpl;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { PWCFigureTitle.class },
    resourceType = PWCFigureTitleImpl.RESOURCE_TYPE)
public class PWCFigureTitleImpl implements PWCFigureTitle {

    public static final String RESOURCE_TYPE = "pwc-madison/components/ditacontent/pwcfiguretitle";

    public static final Logger LOGGER = LoggerFactory.getLogger(PWCTopicHeaderImpl.class);

    @SlingObject
    private Resource resource;

    String figureID = StringUtils.EMPTY;
    String figureTitle = StringUtils.EMPTY;

    @PostConstruct
    private void initModel() {
        final Resource figTitleRes = resource.getChild("_text");
        if (null != figTitleRes) {
            final ValueMap valueMap = figTitleRes.getValueMap();
            figureTitle = valueMap.containsKey("text") ? valueMap.get("text", String.class) : StringUtils.EMPTY;
        }
        final Resource figRes = resource.getParent();
        if (null != figRes) {
            final ValueMap valueMap = figRes.getValueMap();
            figureID = valueMap.containsKey("id") ? valueMap.get("id", String.class) : StringUtils.EMPTY;
        }
    }

    @Override
    public String getFigureID() {
        return figureID;
    }

    @Override
    public String getFigureTitle() {
        return figureTitle;
    }

}
