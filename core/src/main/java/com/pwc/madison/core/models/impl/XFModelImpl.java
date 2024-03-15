package com.pwc.madison.core.models.impl;

import com.pwc.madison.core.models.XfModel;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Model(adaptables = Resource.class,adapters = XfModel.class)
public class XFModelImpl implements XfModel{
    private static final String ROOT_RESOURCE = "root";
    private final List<String> childrenResourcePath = new ArrayList<>();

    /** Default Logger */
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    @SlingObject
    private Resource currentResource;

    @PostConstruct
    public void init() {
        final Resource rootResource = this.currentResource.getChild(ROOT_RESOURCE);
        if (rootResource.hasChildren()){
            for (final Resource resource:
                 rootResource.getChildren()) {
                if (Objects.nonNull(resource)) {
                    this.childrenResourcePath.add(resource.getPath());
                }
            }
        }
    }

    @Override
    public List<String> getChildrenResourcePath() {
        return this.childrenResourcePath;
    }
}
