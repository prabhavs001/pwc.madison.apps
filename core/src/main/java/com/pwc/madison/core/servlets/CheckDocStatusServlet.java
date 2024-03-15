package com.pwc.madison.core.servlets;

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

import com.day.cq.commons.jcr.JcrConstants;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;

/**
 * Servlet to check the asset's (ditamap's) doc status for the fullcycle and simple workflows
 */
@Component(service = Servlet.class,
           property = { Constants.SERVICE_DESCRIPTION + "=Servlet to get the document status of the selected ditamap",
                   "sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.paths=/bin/pwc/getDocStatus" },
           configurationPolicy = ConfigurationPolicy.REQUIRE)
public class CheckDocStatusServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckDocStatusServlet.class);

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        String inReview = StringUtils.EMPTY;
        RequestParameterMap requestParameterMap = request.getRequestParameterMap();
        ResourceResolver resourceResolver = request.getResourceResolver();
        String ditamapPath = requestParameterMap.containsKey(DITAConstants.DITAMAP)?requestParameterMap.getValue(DITAConstants.DITAMAP).getString():StringUtils.EMPTY;
        Resource ditaMapMetaRes = resourceResolver.getResource(ditamapPath + MadisonConstants.METADATA_PATH);
        Resource ditaJcrRes = resourceResolver.getResource(ditamapPath + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
        if (null != ditaMapMetaRes) {
            ValueMap mapMetaProperties = ditaMapMetaRes.getValueMap();
            ValueMap mapJcrProperties = ditaJcrRes.getValueMap();
            Long status = mapJcrProperties.containsKey(DITAConstants.STATUS_PROP_NAME)?mapJcrProperties.get(DITAConstants.STATUS_PROP_NAME, Long.class):0L;
            if (mapMetaProperties.containsKey(DITAConstants.PN_METADATA_DOCSTATE)) {
                String docState = mapMetaProperties.get(DITAConstants.PN_METADATA_DOCSTATE, String.class);
                if (status == 0L || status == 2L) {
                    inReview = DITAConstants.COMPLETE_STATUS;
                } else if(status == 1L){
                    inReview = DITAConstants.REV_IN_PROGRESS;
                } else if(status == 3L){
                    inReview = DITAConstants.COLLAB_IN_PROGRESS;
                }
            }
        }
        response.getWriter().print(inReview);
    }
}
