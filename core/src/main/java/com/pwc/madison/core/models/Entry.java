package com.pwc.madison.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a table of content entry and provides the children entries.
 */
@Model(adaptables=Resource.class)
public class Entry {

    public static final Logger LOG = LoggerFactory.getLogger(Entry.class);

    private Resource resource;

    @Inject @Optional
    private String title;

    @Inject
    @Optional
    private String link;

    /**
     * Constructs an entry
     * @param resource the resource
     */
    public Entry(Resource resource) {
        this.resource = resource;
    }

    /**
     * Gets the title
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the link
     * @return the link
     */
    public String getLink() {
        return link;
    }

    /**
     * Gets a list of entries
     * @return the list of entries
     */
    public List<Entry> getEntries() {
        List<Entry> entries = new ArrayList<>();
        Iterator<Resource> iResource = resource.getChildren().iterator();
        while (iResource.hasNext()) {
            Resource child = iResource.next();
            Entry entry = child.adaptTo(Entry.class);
            if (entry.getTitle() != null) {
                entries.add(child.adaptTo(Entry.class));
            }
        }
        return entries;
    }
}
