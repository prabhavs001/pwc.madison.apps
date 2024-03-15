package com.pwc.madison.core.servlets;

import com.pwc.madison.core.schedulers.DeleteSitemapGeneratorScheduler;
import com.pwc.madison.core.schedulers.IncrementalSitemapXmlGeneratorScheduler;
import com.pwc.madison.core.schedulers.SitemapXmlGeneratorScheduler;
import com.pwc.madison.core.services.ExplicitSitemapGenerationService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Servlet for creating full sitemap
 * in case of full or incremental sitemap
 * curl -u <user>:<password> <host>/bin/pwc-madison/sitemapgenerator?type=<full/incremental>&territory=<territory>
 * in case of delete sitemap
 * curl -u <user>:<password> <host>/bin/pwc-madison/sitemapgenerator?type=delete&language=<language>
 *
 * @author aditya.vijayvargia
 */
@Component(
        immediate = true,
        service = { Servlet.class },
        enabled = true,
        property = { Constants.SERVICE_DESCRIPTION + "= Recently Viewed Servlet",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                "sling.servlet.paths=" + "/bin/pwc-madison/sitemapgenerator" })
public class SitemapXmlGeneratorServlet extends SlingAllMethodsServlet {
    private static final long serialVersionUID = 1L;
    @Reference(service = SitemapXmlGeneratorScheduler.class)
    private ExplicitSitemapGenerationService fullExplicitService;
    @Reference(service = DeleteSitemapGeneratorScheduler.class)
    private ExplicitSitemapGenerationService deleterExplicitService;
    @Reference(service = IncrementalSitemapXmlGeneratorScheduler.class)
    private ExplicitSitemapGenerationService incrementalExplicitService;

    @Reference
    private XSSAPI xssapi;

    private static final Logger LOGGER = LoggerFactory.getLogger(SitemapXmlGeneratorServlet.class);
    private static final String DELETE = "delete";
    private static final String INCREMENTAL = "incremental";
    private static final String FULL = "full";

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws ServletException, IOException {
        ExplicitSitemapGenerationService explicitService = null;
        String territoryOrLang;
        String type = request.getParameter("type") != null ? request.getParameter("type") : "";
        String territory = request.getParameter("territory")!=null?request.getParameter("territory"):"";
        String language = request.getParameter("language")!=null?request.getParameter("language"):"";
        territoryOrLang=territory;
        PrintWriter printWriter = response.getWriter();
        switch (type) {
            case DELETE:
                explicitService = deleterExplicitService;
                break;
            case INCREMENTAL:
                explicitService = incrementalExplicitService;
                break;
            case FULL:
                explicitService = fullExplicitService;
                break;
            default:
                printWriter.println("Please enter type of sitemap");
                break;
        }
        if (explicitService != null) {
            ResourceResolver resolver = request.getResourceResolver();
            Session session = resolver.adaptTo(Session.class);
            LOGGER.info("SitemapXmlGeneratorServlet : Servlet hit by User ID {} at time {}",
                    session.getUserID(), java.time.LocalDate.now() + " " + java.time.LocalTime.now());
            if(type.equals(DELETE)){
                printWriter.println("Delete sitemap");
                territoryOrLang=language;
            }
            try {
                explicitService.generateExplicitSitemap(territoryOrLang, printWriter);
                printWriter.println(xssapi.encodeForHTML(territory+" "+type+" siteMap creation is success"));
            } finally {
                printWriter.println("sitemap generation ends here");
                LOGGER.info("SitemapXmlGeneratorServlet :  sitemap generation ends here");
            }
        }
    }
}
