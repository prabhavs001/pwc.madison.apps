package com.pwc.madison.core.servlets;


import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.day.cq.wcm.api.WCMMode;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pwc.madison.core.constants.DITAConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.engine.SlingRequestProcessor;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 *
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Lazy Loading Servlet for Sidebar Items",
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=pwc-madison/components/structure/page",
                ServletResolverConstants.SLING_SERVLET_SELECTORS + "=templates",
                ServletResolverConstants.SLING_SERVLET_SELECTORS + "=relatedContent",
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
        })
@Designate(ocd=SidebarServlet.Configuration.class)
public class SidebarServlet extends SlingSafeMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(SidebarServlet.class);

    private static String NT_TEMPLATES = "templates-links";
    private static String PN_LINK = "link";
    private static String PN_NODE_TYPE = "type";

    /** Service to create HTTP Servlet requests and responses */
    @Reference
    private RequestResponseFactory requestResponseFactory;

    /** Service to process requests through Sling */
    @Reference
    private SlingRequestProcessor requestProcessor;

    private String relatedLinksPath;

    @ObjectClassDefinition(name = "PwC Viewpoint Sidebar Servlet")
    public @interface Configuration {
        @AttributeDefinition(
                name = "Related Links JCR Path",
                description = "Relative path to the node where we read the templates from"
        )
        String related_links_path() default "root/container/maincontainer/readerrow/docreader/topicnode/topicbody/pwc-topic/related-links";

    }

    @Activate
    @Modified
    protected void activate(Configuration config) {
        relatedLinksPath = config.related_links_path();

    }


    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws ServletException, IOException {

        log.debug("in sidebar servlet get request");

        RequestPathInfo pathInfo = request.getRequestPathInfo();
        final Gson gson = new Gson();
        Resource currentResource = request.getResource();

        String[] selectors = pathInfo.getSelectors();

        if (selectors.length != 3) {
            //build error for invalid upper/lower bounds
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{ \"status\" : \"error - invalid argument length\"}");
            return;
        }

        String sidebarType = selectors[0];
        int lowerBound = Integer.parseInt(selectors[1]);
        int upperBound = Integer.parseInt(selectors[2]);

        Resource sideBarResource = null;
        sideBarResource = currentResource.getChild(relatedLinksPath);

        if (null != sideBarResource) {
            //iterate over children of template and get the paths and then get the HTML responses from those paths
            Resource templateResource;
            Iterator<Resource> sidebarChildren = sideBarResource.listChildren();
            JsonArray resultArray = new JsonArray();
            String pagePath = null;
            while (sidebarChildren.hasNext()) {
                templateResource = sidebarChildren.next();
                ValueMap templateResourceProps = templateResource.getValueMap();
                if (templateResourceProps.get(PN_NODE_TYPE, String.class).equals(NT_TEMPLATES)) {
                    Iterator<Resource> templateChildren = templateResource.listChildren();
                    Resource templateChildResource;
                    int i = 1;
                    // Will need to revisit this to see what happens when the link is pointing to a dita page and not an AEM page
                    while (templateChildren.hasNext()) {
                        templateChildResource = templateChildren.next();
                        if (i >= lowerBound && i <= upperBound) {
                            ValueMap childValueMap = templateChildResource.getValueMap();
                            pagePath = childValueMap.get(PN_LINK, String.class);
                            if (null != pagePath) {
                                resultArray.add(pagePath + DITAConstants.HTML_EXT);
                            }
                        }
                        if (null != pagePath) {
                            i++;
                        }
                    }
                }
            }
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("results", resultArray);
            response.getWriter().write(jsonObject.toString());
        }

    }

}
