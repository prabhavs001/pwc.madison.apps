package com.pwc.madison.core.reports;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.adobe.acs.commons.reports.api.ReportException;
import com.adobe.acs.commons.reports.api.ReportExecutor;
import com.adobe.acs.commons.reports.api.ResultsPage;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.text.csv.Csv;
import com.pwc.madison.core.reports.columns.FileManagementReportCSVExporter;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The Class is an extension/override of the ACS Commons Export CSV Servlet. This servlet gets the report executor
 * configured in the report instead of getting the default QueryReportExecutor class, as implemented in ACS Commons
 * Export CSV Servlet.
 */
@Component(
    service = Servlet.class,
    property = { Constants.SERVICE_RANKING + ":Integer=100000",
            "sling.servlet.resourceTypes=acs-commons/components/utilities/report-builder/report-page",
            "sling.servlet.selectors=report", "sling.servlet.extensions=csv", "sling.servlet.methods=GET" })
public class MadisonReportCSVExportServlet extends SlingSafeMethodsServlet {

    private static final String SEE_ALSO_TOPIC_PATH_CSV_EXPORTER = "SeeAlsoTopicPathCSVExporter";

	private static final String SEE_ALSO_BROKEN_LINKS_PATH_CSV_EXPORTER = "SeeAlsoBrokenLinksPathCSVExporter";

	private static final Logger LOGGER = LoggerFactory.getLogger(MadisonReportCSVExportServlet.class);

    private static final String UNUSED_REPORTS_PATH = "/var/acs-commons/reports/unused-assets-report";
    private static final String SYNDICATION_SOURCE_PATH_EXPORTER = "SyndicationSourceContentPathCSVExporter";
    private static final String SYNDICATION_SOURCE_DOCUMENT_STATE_EXPORTER = "SyndicationSourceDocStatePathCSVExporter";
    private static final String FILE_MANAGEMENT_CSV_EXPORTER = "FileManagementReportCSVExporter";


    private static final String PN_EXECUTOR = "reportExecutor";

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
     * org.apache.sling.api.SlingHttpServletResponse)
     */
    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response)
            throws ServletException, IOException {

        LOGGER.trace("doGet");

        // set response parameters
        response.setContentType("text/csv");
        response.setHeader("Content-disposition", "attachment; filename="
                + URLEncoder.encode(request.getResource().getValueMap().get(JcrConstants.JCR_TITLE, "report"), "UTF-8")
                + ".csv");

        Writer writer = null;
        try {
            writer = response.getWriter();
            // initialize the csv
            final Csv csv = new Csv();
            csv.writeInit(writer);

            // write the headers
            List<ReportCellCSVExporter> exporters = writeHeaders(request, csv);

            Resource configCtr = request.getResource().getChild("config");

            if (configCtr != null && configCtr.listChildren().hasNext()) {
                Iterator<Resource> children = configCtr.listChildren();
                while (children.hasNext()) {
                    Resource config = children.next();
                    if (config != null) {
                        updateCSV(config, request, exporters, csv, writer);
                        LOGGER.debug("Successfully export report with configuration: {}", config);
                        break;
                    } else {
                        LOGGER.warn("Unable to export report for configuration: {}", config);
                    }
                }
                csv.close();
            } else {
                throw new IOException("No configurations found for " + request.getResource());
            }

        } catch (ReportException e) {
            LOGGER.error("Exception extracting report to CSV", e);
            throw new ServletException("Exception extracting report to CSV", e);
        }

    }

    /**
     * Write headers.
     *
     * @param request
     *            the request
     * @param csv
     *            the csv
     * @return the list
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private List<ReportCellCSVExporter> writeHeaders(SlingHttpServletRequest request, final Csv csv)
            throws IOException {
          List<String> row = new ArrayList<>();
          List<ReportCellCSVExporter> exporters = new ArrayList<>();
          for (Resource column : request.getResource().getChild("columns").getChildren()) {
            String className = column.getValueMap().get("exporter", String.class);
            if (!StringUtils.isEmpty(className)) {
              try {
                  LOGGER.debug("Finding ReportCellCSVExporter for {}", className);
                @SuppressWarnings("unchecked")
                Class<ReportCellCSVExporter> clazz = (Class<ReportCellCSVExporter>) getClass().getClassLoader()
                    .loadClass(className);
                ReportCellCSVExporter exporter = column.adaptTo(clazz);
                LOGGER.debug("Loaded ReportCellCSVExporter {}", exporter);
                if (exporter != null) {
                  exporters.add(exporter);
                  row.add(column.getValueMap().get("heading", String.class));
                } else {
                    LOGGER.warn("Retrieved null ReportCellCSVExporter for {}", className);
                }
              } catch (Exception e) {
                  LOGGER.warn("Unable to render column due to issue fetching ReportCellCSVExporter " + className, e);
              }
            }
          }
          csv.writeRow(row.toArray(new String[row.size()]));
          return exporters;
        }

    /**
     * Update CSV.
     *
     * @param config
     *            the config
     * @param request
     *            the request
     * @param exporters
     *            the exporters
     * @param csv
     *            the csv
     * @param writer
     *            the writer
     * @throws ReportException
     *             the report exception
     */
    private void updateCSV(Resource config, SlingHttpServletRequest request, List<ReportCellCSVExporter> exporters,
            Csv csv, Writer writer) throws ReportException {
        String reportExecutorClass = config.getValueMap().get(PN_EXECUTOR, String.class);
        if (StringUtils.isNotBlank(reportExecutorClass)) {
            LOGGER.debug("Loading class for: {}", reportExecutorClass);
            try {
                Class<?> exClass = getClass().getClassLoader().loadClass(reportExecutorClass);
                Object model = request.adaptTo(exClass);
                if (model instanceof ReportExecutor) {
                    ReportExecutor executor = (ReportExecutor) model;
                    executor.setConfiguration(config);
                    LOGGER.debug("Retrieved executor {}", executor);
                    ResultsPage queryResult = executor.getAllResults();
                    List<? extends Object> results = queryResult.getResults();
                    LOGGER.debug("Retrieved {} results", results.size());

                    writeCSV(results, exporters, csv, writer, request);
                }
            } catch (ClassNotFoundException e) {
                LOGGER.warn("Unable to find class ", e);
            } catch (Exception e) {
                LOGGER.warn("Unexpected exception executing report executor ", e);
            }
        }
        LOGGER.debug("Results written successfully");

    }

    /**
     * Write CSV.
     *
     * @param results
     *            the results
     * @param exporters
     *            the exporters
     * @param csv
     *            the csv
     * @param writer
     *            the writer
     * @param request
     *            the request
     */
    private void writeCSV(List<? extends Object> results, List<ReportCellCSVExporter> exporters, Csv csv,
            Writer writer, SlingHttpServletRequest request) {
        for (Object result : results) {
            List<String> row = new ArrayList<>();
            try {
                for (ReportCellCSVExporter exporter : exporters) {
                    if(exporter.toString().contains(SYNDICATION_SOURCE_PATH_EXPORTER)){
                        SyndicationSourceContentPathCSVExporter sourcePathReportCellCSVExporter = new SyndicationSourceContentPathCSVExporter();
                        row.add(sourcePathReportCellCSVExporter.getSourcePathValue(result, request));
                    }else if(exporter.toString().contains(SYNDICATION_SOURCE_DOCUMENT_STATE_EXPORTER)){
                        SyndicationSourceDocStatePathCSVExporter syndicationSourceDocStatePathCSVExporter = new SyndicationSourceDocStatePathCSVExporter();
                        row.add(syndicationSourceDocStatePathCSVExporter.getDocStateValue(result, request));
                    }else if(exporter.toString().contains(FILE_MANAGEMENT_CSV_EXPORTER)) {
                        FileManagementReportCSVExporter fileReportExecutor=(FileManagementReportCSVExporter)exporter;
                        row.add(fileReportExecutor.populateRowValue(result,request));
                    } else if(exporter.toString().contains(SEE_ALSO_BROKEN_LINKS_PATH_CSV_EXPORTER)) {
                    	SeeAlsoBrokenLinksPathCSVExporter seeAlsoBrokenLinksPathCSVExporter = new SeeAlsoBrokenLinksPathCSVExporter();
                        row.add(seeAlsoBrokenLinksPathCSVExporter.getSourcePathValue(result, request));
                    } else if(exporter.toString().contains(SEE_ALSO_TOPIC_PATH_CSV_EXPORTER)) {
                    	SeeAlsoTopicPathCSVExporter seeAlsoTopicPathCSVExporter = new SeeAlsoTopicPathCSVExporter();
                        row.add(seeAlsoTopicPathCSVExporter.getSourcePathValue(result, request));
                    }
                    else{
                        row.add(exporter.getValue(result));
                    }
                }
                csv.writeRow(row.toArray(new String[row.size()]));
                writer.flush();
            } catch (Exception e) {
                LOGGER.warn("Exception writing row: " + row, e);
            }
        }

    }
}


