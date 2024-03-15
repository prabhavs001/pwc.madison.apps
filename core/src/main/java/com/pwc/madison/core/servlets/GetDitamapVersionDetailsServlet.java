package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.Servlet;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.models.VersionDetails;

/**
 * Servlet that returns the list of linked topics/maps in different versions of a ditamap
 */
@Component(service = Servlet.class,
           property = { Constants.SERVICE_DESCRIPTION
                   + "=This servlet is called on load of the Review Map Revisions console",
                   "sling.servlet.methods=" + "POST", "sling.servlet.paths=" + "/bin/pwc/versiondetails",
                   "sling.servlet.extensions=" + "json" })
public class GetDitamapVersionDetailsServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(GetDitamapVersionDetailsServlet.class);
    private static final long serialVersionUid = 1L;

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {
        String versionsData = StringUtils.EMPTY;
        ResourceResolver resourceResolver = request.getResourceResolver();
        List<VersionDetails> versionDetailsList = Collections.EMPTY_LIST;

        RequestParameterMap requestParameterMap = request.getRequestParameterMap();

        if (requestParameterMap.containsKey(DITAConstants.VERSION_PARAM)) {
            versionsData = requestParameterMap.getValue(DITAConstants.VERSION_PARAM).getString();
            String[] versionDataArray = versionsData.split(",");
            if (versionDataArray.length > 0) {
                versionDetailsList = new ArrayList<VersionDetails>();
                populateVersionDetailsMap(versionDataArray, versionDetailsList, resourceResolver);
            }
        }
        Gson gson = new Gson();
        gson.toJson(versionDetailsList, response.getWriter());
    }

    /**
     * Populates the response with version specific details
     * @param versionsArray
     * @param versionDetailsList
     * @param resourceResolver
     */
    private void populateVersionDetailsMap(String[] versionsArray, List<VersionDetails> versionDetailsList,
            ResourceResolver resourceResolver) {
        for (String versionPath : versionsArray) {
            String version = StringUtils.EMPTY;
            Resource versionRes = resourceResolver.getResource(versionPath);
            if (null != versionRes) {
                version = versionRes.getName();
                Resource jcrRes = resourceResolver.getResource(versionPath + DITAConstants.VERSION_METADATA_NODE);
                if (null != jcrRes) {
                    ValueMap propertiesMap = jcrRes.getValueMap();
                    if (propertiesMap.containsKey(DITAConstants.PN_FMDITATOPICREFS)) {
                        VersionDetails versionDetails = new VersionDetails();
                        versionDetails.setVersion(version);
                        String[] topicRefs = propertiesMap.get(DITAConstants.PN_FMDITATOPICREFS, String[].class);
                        versionDetails.setTopicRefs(topicRefs);
                        versionDetails.setFrozenNodePath(versionPath+DITAConstants.VERSION_FROZEN_NODE_PATH);
                        versionDetailsList.add(versionDetails);
                    }
                }
            }
        }
    }
}
