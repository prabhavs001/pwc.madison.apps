package com.pwc.madison.core.models;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

/**
 * Provides the table of contents list for the navigation on the detail page.
 */
@Model(adaptables=Resource.class)
public class DetailNavigation {

    public static final Logger LOG = LoggerFactory.getLogger(DetailNavigation.class);

    private Page basePage;
    private Resource toc;

    /**
     * Contructs a detail navigation which provides all of the table of content entries
     * @param resource the resource
     */
    public DetailNavigation(Resource resource) {
        ResourceResolver rr = resource.getResourceResolver();
        PageManager pm = rr.adaptTo(PageManager.class);
        Page currentPage = pm.getContainingPage(resource);
        if (currentPage != null) {
            String basePath =  currentPage.getProperties().get("basePath","");
            String tocPath = currentPage.getProperties().get("tocPath", "");
            this.basePage = pm.getContainingPage(basePath);
            this.toc = rr.resolve(basePath + "/" + tocPath);
        }
    }

    /**
     * Gets the base page title
     * @return the base page title
     */
    public String getBasePageTitle() {
        return basePage.getTitle();
    }

    /**
     * Gets the list of entries
     * @return the list of entries
     */
    public List<Entry> getEntries() {
        List<Entry> entries = new ArrayList<>();
        if (toc != null) {
            Iterator<Resource> iResource = toc.getChildren().iterator();
            while (iResource.hasNext()) {
                Resource child = iResource.next();
                Entry entry = child.adaptTo(Entry.class);
                if (entry.getTitle() != null) {
                    entries.add(child.adaptTo(Entry.class));
                }
            }
        }
        return entries;
    }
}
