package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.Servlet;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.text.csv.Csv;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.SyndicationFailureReportService;

/**
 * Servlet that returns last published time of publishing point maps for source and syndicated map
 */
@Component(service = Servlet.class, property = {
		Constants.SERVICE_DESCRIPTION + "=This servlet is called in Syndication Publids details Reports page.",
		"sling.servlet.methods=" + "GET", "sling.servlet.paths=" + "/bin/pwc/syndication/publish/report",
		"sling.servlet.extensions=" + "csv" })
public class SyndicationPublishReportServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(SyndicationPublishReportServlet.class);
	@Reference
	transient SyndicationFailureReportService syndicationFailureReportService;
	
    @Reference
    private transient XSSAPI xssApi;

	@Override
	protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
			throws IOException {
		doPost(request, response);
	}

	@Override
	protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
			throws IOException {
		try {
			String path = request.getParameter(MadisonConstants.SYNDICATION_INPUT_PATH);
			LOG.debug("Syndication publish report executes for path: {}", xssApi.encodeForHTML(path));

			String extension = request.getRequestPathInfo().getExtension();

			if (extension != null && !path.isEmpty()) {
				if (extension.equals(MadisonConstants.CSV_EXTENSION)) {
					response.setContentType(MadisonConstants.CSV_CONTENT_TYPE);
					response.setHeader(MadisonConstants.CONTENT_DISPOSITION, "attachment; filename=Syndication_Publish_Report.csv");
					Writer writer = response.getWriter();
					final Csv csv = new Csv();
					csv.writeInit(writer);
					syndicationFailureReportService.writePublishReport(path, csv, writer);
					csv.close();
				}
			}
		} catch (final Exception e) {
			LOG.error("Error in SyndicationPublishReportServlet : {}", e);
			response.setContentType(ContentType.TEXT_HTML.getMimeType());
			response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().write("Error in Syndication Publish Report Servlet");
		}
	}
}
