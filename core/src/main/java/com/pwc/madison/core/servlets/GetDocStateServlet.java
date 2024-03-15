package com.pwc.madison.core.servlets;

import com.day.cq.commons.jcr.JcrConstants;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to return docstate property of an asset
 */
@Component(service = Servlet.class,
           property = { Constants.SERVICE_DESCRIPTION + "=Servlet to get the docstate property of an asset",
                   "sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.paths=/bin/pwc/getDocState" },
           configurationPolicy = ConfigurationPolicy.REQUIRE)
public class GetDocStateServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetDocStateServlet.class);
    private static final String PATH = "path";

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        String docState = StringUtils.EMPTY;
        try {
            RequestParameterMap requestParameterMap = request.getRequestParameterMap();
            ResourceResolver resourceResolver = request.getResourceResolver();
            if (requestParameterMap.containsKey(PATH)) {
                String path = requestParameterMap.getValue(PATH).getString();
                Resource assetMetaResource = resourceResolver.getResource(path.concat(MadisonConstants.METADATA_PATH));
                if (null != assetMetaResource) {
                    ValueMap assetMetaProperties = assetMetaResource.getValueMap();
                    docState = assetMetaProperties.containsKey(DITAConstants.PN_METADATA_DOCSTATE) ? assetMetaProperties.get(DITAConstants.PN_METADATA_DOCSTATE).toString() : StringUtils.EMPTY;
                }
            }
            response.getWriter().print(docState);
        }catch (Exception e){
            LOGGER.error("Error while gettting docstate", e);
        }
    }
}
