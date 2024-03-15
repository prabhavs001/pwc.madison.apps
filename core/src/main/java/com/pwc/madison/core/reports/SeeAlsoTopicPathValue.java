package com.pwc.madison.core.reports;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.Self;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Model(adaptables = SlingHttpServletRequest.class)
public class SeeAlsoTopicPathValue {

    private String path = StringUtils.EMPTY;

    @Self
    private SlingHttpServletRequest request;

    @Inject
    @Optional
    private Resource result;

    @PostConstruct
    private void init() {
    	 path = result.getPath();
    }

    public String getPath() {
        return path;
    }
}
