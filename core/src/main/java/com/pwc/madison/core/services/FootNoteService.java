package com.pwc.madison.core.services;

import com.day.cq.search.result.SearchResult;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * The interface FootNoteService provides HTML of a Footnote
 */
public interface FootNoteService {

    void updateFootnoteHTMLString(String path);

    String getHtml(final String nodePath, final Resource resource) throws ServletException, IOException;

    SearchResult getFootNotesNodes(String path, ResourceResolver resourceResolver);
}
