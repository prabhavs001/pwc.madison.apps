package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.Nonnull;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.services.ExplicitRecordsGenerationService;

/**
 * Servlet for start records XML creation. 
 * curl -u <user>:<password> <host>/bin/pwc-madison/recordsgenerator
 */
@Component(
        immediate = true,
        service = Servlet.class,
        enabled = true,
        property = { Constants.SERVICE_DESCRIPTION + "= Records XML Generation Servlet",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                "sling.servlet.paths=" + "/bin/pwc-madison/recordsgenerator" })
public class RecordsXmlGeneratorServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordsXmlGeneratorServlet.class);

    @Reference
    private ExplicitRecordsGenerationService explicitRecordsGenerationervice;

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response)
            throws ServletException, IOException {
        final Session session = request.getResourceResolver().adaptTo(Session.class);
        final PrintWriter printWriter = response.getWriter();
        LOGGER.info("RecordsXmlGeneratorServlet : Servlet hit by User ID {} at time {}", session.getUserID(),
                java.time.LocalDate.now() + " " + java.time.LocalTime.now());
        explicitRecordsGenerationervice.generateRecordsXml(printWriter);
    }
}
