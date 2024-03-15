package com.pwc.madison.core.models;

import com.day.cq.wcm.api.Page;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;

/**
 * Redirects to the first entry page of the table of contents.
 */
@Model(adaptables=SlingHttpServletRequest.class)
public class ChildRedirect {

    public static final Logger LOG = LoggerFactory.getLogger(ChildRedirect.class);

    @SlingObject
    private SlingHttpServletResponse response;
    @SlingObject
    private ResourceResolver resourceResolver;
    @ScriptVariable
    private Page currentPage;
    private String redirectUrl;

    @PostConstruct
    private void activate() throws IOException {
        String redirectUrl = getRedirectTarget();
        if (redirectUrl != null) {
            LOG.trace("Redirecting ({}) to {}", 301, redirectUrl);
            response.sendRedirect(redirectUrl);
        }
    }

    /**
     * Uses the document table of contents to find the first entry and sets that as the redirect target
     * @return the redirect target
     */
    public final String getRedirectTarget() {
        String redirectTarget = null;
        Resource resource = currentPage.adaptTo(Resource.class);
        DetailNavigation toc = new DetailNavigation(resource);
        List<Entry> entries = toc.getEntries();
        if (entries != null) {
            if (entries.size() > 0) {
                redirectTarget = entries.get(0).getLink();
            }
        }
        return redirectTarget;
    }
}
