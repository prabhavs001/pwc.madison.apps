package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
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

import com.pwc.madison.core.util.DITAUtils;

@Component(
    service = Servlet.class,
    property = { Constants.SERVICE_DESCRIPTION + "=Get Page Ref From DITA Servlet",
            "sling.servlet.methods=" + HttpConstants.METHOD_GET,
            "sling.servlet.paths=" + "/bin/pwc-madison/getPageRef" },
    configurationPolicy = ConfigurationPolicy.REQUIRE)
public class PageRefFromDitaServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Reference
    private transient XSSAPI xssApi;

    @Override
    protected void doGet(@Nonnull final SlingHttpServletRequest request,
            @Nonnull final SlingHttpServletResponse response) throws ServletException, IOException {

        final ResourceResolver resourceResolver = request.getResourceResolver();
        final PrintWriter writer = response.getWriter();

        if (null == resourceResolver) {
            writer.write("Unable to get service resolver to process data. Please check the permissions\n");
            writer.close();
            return;
        }

        final String source = request.getParameter("src");
        final String getAll = request.getParameter("getAll");
        log.debug("Dita Path : {}", source);
        if (StringUtils.isBlank(source)) {
            response.setStatus(500);
            writer.write(StringUtils.EMPTY);
            writer.close();
            return;
        }
        final String pagePath;
        if (StringUtils.isNotBlank(getAll) && "true".equalsIgnoreCase(getAll)) {
            final List<String> pagePathsFromDita = DITAUtils.getPagePathsFromDita(source, resourceResolver);
            pagePath = Arrays.toString(pagePathsFromDita.toArray());
        } else {
            pagePath = DITAUtils.getPageFromXrefDita(source, resourceResolver, xssApi);
        }
        writer.write(pagePath);
        writer.close();
    }

}
