package com.pwc.madison.core.models.impl;

import com.pwc.madison.core.models.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;

@Model(adaptables = SlingHttpServletRequest.class, adapters = RelatedContentModel.class)
public class RelatedContentModelImpl implements RelatedContentModel {

    @Inject
    SlingHttpServletRequest request;

    /** Default Logger*/
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private static final Integer LOWER_LIMIT = 1;
    private static final Integer UPPER_LIMIT = 12;


    private FeaturedContentModel featuredContentModel;
    private String heading;

    @PostConstruct
    protected void init(){
        request.setAttribute("sortList",false);
        request.setAttribute("upperLimit",UPPER_LIMIT);
        request.setAttribute("lowerLimit",LOWER_LIMIT);
        heading = request.getResource().getValueMap().get("heading",String.class);
        featuredContentModel = request.adaptTo(FeaturedContentModel.class);
    }

    @Override
    public List<FeaturedContentItem> getFilteredList() {
        return featuredContentModel.getFilteredList();
    }

    @Override
    public String getHeading() {
        return heading;
    }
}
