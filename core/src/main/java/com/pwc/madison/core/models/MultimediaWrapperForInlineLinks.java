package com.pwc.madison.core.models;

import java.util.Objects;

import javax.annotation.PostConstruct;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.DITAConstants;

/**
 * Multimedia wrapper model for Inline Links
 */
@Model(adaptables = Resource.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class MultimediaWrapperForInlineLinks {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultimediaWrapperForInlineLinks.class);

    private static final String DM_COMPONENT_RELATIVE_PATH_FROM_PAGE = "root/maincontainer/readerrow/bodycontainer/docreader/contentbody/topicbody/pwc-topic/pwc-body/bodydiv/object";

    @SlingObject
    private ResourceResolver resourceResolver;

    @ValueMapValue(name = DITAConstants.PROPERTY_LINK)
    private String link;

    private String dmComponentPath = "";
    private Page wrapperPage;

    @PostConstruct
    protected void init() {

        if (null == link) {
            return;
        }

        final Resource wrapperResource = resourceResolver.getResource(link);

        if (Objects.isNull(wrapperResource)) {
            return;
        }

        wrapperPage = wrapperResource.adaptTo(Page.class);

        if (Objects.isNull(wrapperPage)) {
            return;
        }

        final Resource dynamicMediaResource = wrapperPage.getContentResource(DM_COMPONENT_RELATIVE_PATH_FROM_PAGE);
        if (Objects.nonNull(dynamicMediaResource)) {
            dmComponentPath = dynamicMediaResource.getPath();
        }
    }

    public String getDmComponentPath() {
        return dmComponentPath;
    }

}
