package com.pwc.madison.core.services;

import com.day.cq.search.result.SearchResult;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * The interface XrefGroupService provides the anchor text of the URL
 */
public interface XrefGroupService {

    String getReferencedSectionAnchorText(Resource xrefResource);
}
