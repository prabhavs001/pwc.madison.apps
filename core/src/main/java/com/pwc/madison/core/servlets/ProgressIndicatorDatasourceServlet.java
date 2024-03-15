package com.pwc.madison.core.servlets;

import com.google.gson.Gson;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Iterate and fetch HTML ID attributes under a given path
 */
@Component(service = Servlet.class, immediate = true, property = {Constants.SERVICE_DESCRIPTION + "= Get HTML ID attribute from Page",
        "sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.paths=" + "/bin/sectionIdList"})
public class ProgressIndicatorDatasourceServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressIndicatorDatasourceServlet.class);

    private static final String PN_ID = "id";

    private static final String PN_TITLE = "title";

    private static final String NODE_PROGRESS_INDICATOR_LINKS = "progressIndicatorLinks";

    private static final String RESOURCE_TYPE = "pwc-madison/components/inloop/progress-indicator";

    @Override
    protected void doGet(@Nonnull final SlingHttpServletRequest request, @Nonnull final SlingHttpServletResponse response) throws IOException {
        final String path = request.getParameter("path");
        final Resource resource = request.getResourceResolver().getResource(path);
        ArrayList<String> idList = new ArrayList<>();
        ArrayList<String> existingIdList = new ArrayList<>();
        if (resource != null && !ResourceUtil.isNonExistingResource(resource))
            findIdAttribute(resource, idList, existingIdList);
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("idList", idList);
            jsonObject.put("existingIdList", existingIdList);
            response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
            LOGGER.debug("JSON value is {} ", jsonObject);
            response.getWriter().println(new Gson().toJson(jsonObject));
        } catch (JSONException e) {
            LOGGER.error("JSONException occurred: ", e);
        }

    }

    private void findIdAttribute(Resource resource, ArrayList<String> idList, ArrayList<String> existingIdList) {
        if (resource.hasChildren()) {
            final Iterable<Resource> resourceIterator = resource.getChildren();
            for (final Resource res : resourceIterator) {
                if (res.getResourceType().equals(RESOURCE_TYPE)) {
                    getCurrentIdList(res, existingIdList);
                    continue;
                }
                if (res.getValueMap().containsKey(PN_ID))
                    idList.add(res.getValueMap().get(PN_ID, String.class));
                findIdAttribute(res, idList, existingIdList);
            }
        }
    }

    private void getCurrentIdList(Resource res, ArrayList<String> existingIdList) {
        if (res.hasChildren()) {
            Resource linksResource = res.getChild(NODE_PROGRESS_INDICATOR_LINKS);
            if (linksResource != null && !ResourceUtil.isNonExistingResource(linksResource)) {
                final Iterable<Resource> itemIterator = linksResource.getChildren();
                for (final Resource itemResource : itemIterator) {
                    existingIdList.add(itemResource.getValueMap().get(PN_ID, String.class));
                }
            }
        }
    }

}
