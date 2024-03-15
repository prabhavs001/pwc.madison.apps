package com.pwc.madison.core.servlets;

import java.io.IOException;

import javax.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.services.AutoGenerateRCLService;

/**
 * Servlet that auto generate rcls section in a dita topic.
 * 
 * @author sevenkat
 *
 */
@Component(
    service = Servlet.class,
    property = {
            Constants.SERVICE_DESCRIPTION + "=Servlet used to auto generate inline  links section for a dita topic",
            "sling.servlet.methods=" + HttpConstants.METHOD_GET,
            "sling.servlet.paths=/bin/pwc-madison/auto-generate-rcl" },
    configurationPolicy = ConfigurationPolicy.REQUIRE)

public class AutoGenerateRCLServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 3060030368422680556L;
    private static final Logger LOG = LoggerFactory.getLogger(AutoGenerateRCLServlet.class);
    private static final String EQUAL = "=";
    public static final String SOURCE = "src";

    @Reference
    transient AutoGenerateRCLService dataInlineLinksService;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {
        String path = request.getParameter(DITAConstants.BASE_PATH);
        if (StringUtils.isNotBlank(path) && path.contains(SOURCE)) {
            String[] requiredPaths = path.split(SOURCE + EQUAL);
            String ditaPath = requiredPaths[1];
            LOG.debug("DitaPath is {}", ditaPath);
            ResourceResolver resolver = request.getResourceResolver();
            if(StringUtils.isNotBlank(ditaPath) && null!=resolver) {
                dataInlineLinksService.updateInlineLinks(ditaPath, resolver);
            }
            LOG.debug("AutoGenerateRCLServlet :: Auto Generation of RCL's Ended");
        }
    }
}
