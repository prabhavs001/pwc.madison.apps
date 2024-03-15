package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Servlet to return docstate property of an asset
 */
@Component(service = Servlet.class,
           property = { Constants.SERVICE_DESCRIPTION + "=Servlet to get the subsection of an page",
                   "sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.paths=/bin/pwc/getsubsection" },
           configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class GetSubSection extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetSubSection.class);
    private static final String PATH = "path";

    @Reference
    private XSSAPI xssapi;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        String subSection = StringUtils.EMPTY;
        String country = StringUtils.EMPTY;
        try {
            RequestParameterMap requestParameterMap = request.getRequestParameterMap();
            ResourceResolver resourceResolver = request.getResourceResolver();
            if (requestParameterMap.containsKey(PATH)) {
                String path = requestParameterMap.getValue(PATH).getString();
                if(path.contains(MadisonConstants.HTML_EXTN)){
                    path = StringUtils.substringBefore(path, MadisonConstants.HTML_EXTN);
                }
                subSection = DITAUtils.getConditionalSectionTitle(path, resourceResolver, xssapi);
                Resource pageResource = resourceResolver.getResource(path);
                if(null != pageResource){
                    Page page = pageResource.adaptTo(Page.class);
                    Locale locale = page.getLanguage(false);
                    ResourceBundle resourceBundle = request.getResourceBundle(locale);
                    I18n i18n = new I18n(resourceBundle);
                    subSection = i18n.get(subSection);
                }
                country = MadisonUtil.getTerritoryCodeForPath(path);

            }
            response.getWriter().print(country.toUpperCase() + " " + subSection);
        }catch (Exception e){
            LOGGER.error("Error while gettting docstate", e);
        }
    }

}
