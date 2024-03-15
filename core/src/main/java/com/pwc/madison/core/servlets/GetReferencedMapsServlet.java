package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.Servlet;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.models.DitaMapDetails;
import com.pwc.madison.core.util.DITAUtils;

/**
 * Servlet that returns the list of ditamaps referenced inside a given ditamap
 */
@Component(service = Servlet.class,
           property = { Constants.SERVICE_DESCRIPTION + "=This servlet is called on load of the review workflow forms",
                   "sling.servlet.methods=" + "GET", "sling.servlet.paths=" + "/bin/pwc/referencedMaps",
                   "sling.servlet.extensions=" + "json" })
public class GetReferencedMapsServlet extends SlingSafeMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(GetReferencedMapsServlet.class);
    private static final long serialVersionUid = 1L;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {
        String parentMap = StringUtils.EMPTY;
        List<DitaMapDetails> ditaMapsRefs = Collections.EMPTY_LIST;
        ResourceResolver resourceResolver = request.getResourceResolver();

        RequestParameterMap requestParameterMap = request.getRequestParameterMap();

        if (requestParameterMap.containsKey(DITAConstants.DITAMAP)) {
            parentMap = requestParameterMap.getValue(DITAConstants.DITAMAP).getString();
        }
        if (!parentMap.isEmpty()) {
            ditaMapsRefs = DITAUtils.getDitaMapsRefs(parentMap, resourceResolver, null);
            Comparator<DitaMapDetails> compareByLastModified = (DitaMapDetails d1, DitaMapDetails d2) -> d1.getLastModifiedDate().compareTo( d2.getLastModifiedDate() );
            Collections.sort(ditaMapsRefs, compareByLastModified.reversed());
        }
        Gson gson = new Gson();
        gson.toJson(ditaMapsRefs, response.getWriter());
    }
}
