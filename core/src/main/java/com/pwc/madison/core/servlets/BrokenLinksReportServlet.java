package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import com.pwc.madison.core.beans.BrokenLinksReportRow;
import com.pwc.madison.core.beans.PublishListenerReport;
import com.pwc.madison.core.beans.Xref;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.BrokenLinksReportService;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Servlet that returns the HTML listing users and groups belonging to parent group specific to a territory.
 */
@Component(service = Servlet.class,
           property = { Constants.SERVICE_DESCRIPTION + "=This servlet is called in Broken Links Reports page.",
                   "sling.servlet.methods=" + "POST", "sling.servlet.paths=" + "/bin/pwc/brokenlinks",
                   "sling.servlet.extensions=" + "json", "sling.servlet.extensions=" + "csv"  })
public class BrokenLinksReportServlet extends SlingAllMethodsServlet {
    private static final String REPORT = "report";
    private static final long serialVersionUID = 1L;
    private static final String CSV = "csv";
    private static final String ATTACHMENT_FILENAME_BROKEN_LINKS_REPORT_CSV = "attachment; filename=Broken_Links_Report.csv";
    private static final String TEXT_CSV = "text/csv";
    private static final String ATTACHMENT_FILENAME_BROKEN_LINKS_REPORT_JSON = "attachment; filename=Broken_Links_Report.json";
    private static final String CONTENT_DISPOSITION = "Content-disposition";
    private static final String APPLICATION_JSON = "application/json";
    private static final String JSON = "json";
    private static final String FMT = "fmt";
    private static final Logger LOG = LoggerFactory.getLogger(BrokenLinksReportServlet.class);
    private static final long serialVersionUid = 1L;
    @Reference
    transient BrokenLinksReportService brokenLinksReportService;
    Map<String, List<Xref>> topicsMap = Collections.EMPTY_MAP;
    
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
        request.getCookie("login-token");
        String type = StringUtils.EMPTY;
        String path = StringUtils.EMPTY;
        String downloadFormat = StringUtils.EMPTY;

        if (requestParameterMap.containsKey(MadisonConstants.BROKEN_LINKS_INPUT_TYPE)) {
            type = requestParameterMap.getValue(MadisonConstants.BROKEN_LINKS_INPUT_TYPE).getString();
        }
        if (requestParameterMap.containsKey(MadisonConstants.BROKEN_LINKS_INPUT_PATH)) {
            path = requestParameterMap.getValue(MadisonConstants.BROKEN_LINKS_INPUT_PATH).getString();
        }
        if (requestParameterMap.containsKey(FMT)) {
            downloadFormat = requestParameterMap.getValue(FMT).getString();
        }
        
        Gson gson = new Gson();
        if (type.equals(MadisonConstants.STR_FOLDER.toLowerCase()) && !path.isEmpty()) {
            topicsMap = brokenLinksReportService.populateTopicsMapFromFolder(path, resourceResolver);
        } else if (type.equals(MadisonConstants.STR_TOPICS) && !path.isEmpty()) {
            topicsMap = brokenLinksReportService.populateTopicsMapFromTopics(path, resourceResolver);
        }

        String extension = request.getRequestPathInfo().getExtension();
        if (extension != null && !path.isEmpty()) {
            if (extension.equals(JSON) && StringUtils.isEmpty(downloadFormat)) {
                response.setContentType(APPLICATION_JSON);
                gson.toJson(topicsMap, response.getWriter());
            } 
            else if (extension.equals(JSON) && StringUtils.equalsIgnoreCase(downloadFormat, JSON)) {
                response.setContentType(APPLICATION_JSON);
                response.setHeader(CONTENT_DISPOSITION, ATTACHMENT_FILENAME_BROKEN_LINKS_REPORT_JSON);
                List<BrokenLinksReportRow> rp = brokenLinksReportService.getBrokenLinks(resourceResolver, topicsMap,
                        MadisonUtil.getTokenCookieValue(request));
                gson.toJson(rp,response.getWriter());
            }
            else if (extension.equals(JSON) && StringUtils.equalsIgnoreCase(downloadFormat, REPORT)) {
                response.setContentType(APPLICATION_JSON);
                PublishListenerReport publishListenerReport = brokenLinksReportService
                        .getPublishListenerReport(resourceResolver, topicsMap,
                                MadisonUtil.getTokenCookieValue(request));
                if(publishListenerReport != null) {
                    gson.toJson(publishListenerReport,response.getWriter());
                }
            }
            else if (extension.equals(CSV)) {
                response.setContentType(TEXT_CSV);
                response.setHeader(CONTENT_DISPOSITION, ATTACHMENT_FILENAME_BROKEN_LINKS_REPORT_CSV);
                Writer writer = response.getWriter();
                final Csv csv = new Csv();
                csv.writeInit(writer);
                brokenLinksReportService.getBrokenLinksCsvReport(brokenLinksReportService
                        .getBrokenLinks(resourceResolver, topicsMap, MadisonUtil.getTokenCookieValue(request)), csv,
                        writer);
                csv.close();
            }

        }
    }
    
}
