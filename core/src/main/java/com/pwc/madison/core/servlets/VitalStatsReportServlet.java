package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import javax.servlet.Servlet;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.text.csv.Csv;
import com.google.gson.Gson;
import com.pwc.madison.core.beans.VitalStatsReportRow;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.VitalStatsReportService;

/**
 * Servlet that returns the Conversion Status report for AICPA documents.
 */
@Component(service = Servlet.class, property = {
		Constants.SERVICE_DESCRIPTION + "=This servlet is called in Vital Stats Reports page.",
		"sling.servlet.methods=" + "POST", "sling.servlet.paths=" + "/bin/pwc/vitalstats",
		"sling.servlet.extensions=" + "json", "sling.servlet.extensions=" + "csv" })
public class VitalStatsReportServlet extends SlingAllMethodsServlet {

	private static final Logger LOG = LoggerFactory.getLogger(VitalStatsReportServlet.class);
	private static final long serialVersionUid = 1L;

	@Reference
	transient VitalStatsReportService vitalStatsReportService;

	@Override
	protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
			throws IOException {
		doPost(request, response);
	}

	@Override
	protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
			throws IOException {

		ResourceResolver resourceResolver = request.getResourceResolver();
		RequestParameterMap requestParameterMap = request.getRequestParameterMap();
		String path = StringUtils.EMPTY;
		if (requestParameterMap.containsKey(MadisonConstants.VITAL_STATS_INPUT_PATH)) {
			path = requestParameterMap.getValue(MadisonConstants.VITAL_STATS_INPUT_PATH).getString();
		}
		Gson gson = new Gson();
		List<VitalStatsReportRow> vitalStatsReport = null;
		String extension = request.getRequestPathInfo().getExtension();
		if (extension != null && !path.isEmpty()) {
			if (extension.equals("json")) {
				response.setContentType("application/json");
				vitalStatsReport = vitalStatsReportService.getVitalStatsReport(path, resourceResolver);
				gson.toJson(vitalStatsReport, response.getWriter());
			} else if (extension.equals("csv")) {
				response.setContentType("text/csv");
				response.setHeader("Content-disposition", "attachment; filename=Vital_Stats_Report.csv");
				Writer writer = response.getWriter();
				final Csv csv = new Csv();
				csv.writeInit(writer);
				vitalStatsReportService.getVitalStatsCsvReport(path, resourceResolver, csv, writer);
				csv.close();
			}

		}

	}

}
