package com.pwc.madison.core.reports;

import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.DITAUtils;
import org.apache.commons.lang.StringUtils;
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
import javax.jcr.Session;

@Model(adaptables = SlingHttpServletRequest.class)
public class SourceDocumentStateCellValue {
    private static final Logger LOG = LoggerFactory.getLogger(SourceDocumentStateCellValue.class);

    private String docState;

    @Self
    SlingHttpServletRequest request;

    @Inject
    @Optional
    private Resource result;

    @PostConstruct
    private void init() {
        docState = StringUtils.EMPTY;
        if (result != null) {
            ResourceResolver resolver = request.getResourceResolver();
            Session session = resolver.adaptTo(Session.class);
            String[] subscriberLists = (String[]) request.getAttribute(MadisonConstants.SUBSCRIBER_LIST);
            String inputPath = request.getParameter(MadisonConstants.SYNDICATION_INPUT_PATH);
            String currentResourcePath = result.getPath();
            String sourcePath = DITAUtils.getSourcePath(result.getPath(), subscriberLists, resolver, inputPath);
            docState = DITAUtils.getDocState(sourcePath, session);
        }
    }

    public String getDocState() {
        return docState;
    }

}
