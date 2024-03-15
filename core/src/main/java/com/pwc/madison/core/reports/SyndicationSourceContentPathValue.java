package com.pwc.madison.core.reports;

import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.DITAUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Model(adaptables = SlingHttpServletRequest.class)
public class SyndicationSourceContentPathValue {

    private static final Logger log = LoggerFactory.getLogger(SyndicationSourceContentPathValue.class);

    private String path = StringUtils.EMPTY;

    private String assetDetailsPath = StringUtils.EMPTY;

    @Self
    private SlingHttpServletRequest request;

    @Inject
    @Optional
    private Resource result;

    @PostConstruct
    private void init() {
        try {
            ResourceResolver resolver = request.getResourceResolver();
            String[] subscriberLists = (String[]) request.getAttribute(MadisonConstants.SUBSCRIBER_LIST);
            String sourcePath = request.getParameter(MadisonConstants.SYNDICATION_INPUT_PATH);
            String currentResourcePath = result.getPath();
            path = DITAUtils.getSourcePath(result.getPath(), subscriberLists, resolver, sourcePath);
            assetDetailsPath = MadisonConstants.DITA_EDITOR_PATH + path.replace(":","%3A");
        }catch (SlingException  | IllegalStateException e) {
            log.error("Error while getting source syndication path: ", e);
        }
    }

    public String getPath() {
        return path;
    }

    public String getAssetDetailsPath() {
        return assetDetailsPath;
    }
}
