package com.pwc.madison.core.reports;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.reports.api.ReportException;
import com.adobe.acs.commons.reports.api.ReportExecutor;
import com.adobe.acs.commons.reports.api.ResultsPage;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.reports.configs.FileManagementReportConfig;
import com.pwc.madison.core.services.ActivitiesAuditReportService;

/**
 * @author sevenkat
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class ActivitiesAuditReportExecutor implements ReportExecutor {

    private FileManagementReportConfig config;

    private int page;

    private final SlingHttpServletRequest request;

    private static final Logger log = LoggerFactory.getLogger(ActivitiesAuditReportExecutor.class);

    @Inject
    private ActivitiesAuditReportService activitiesAuditReportService;

    /**
     * FileManagementReportService constructor
     *
     * @param request the request
     */
    public ActivitiesAuditReportExecutor(SlingHttpServletRequest request) {
        this.request = request;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.adobe.acs.commons.reports.api.ReportExecutor#getAllResults()
     */
    @Override
    public ResultsPage getAllResults() throws ReportException {
        return fetchPwCActivitiesAuditReport(Integer.MAX_VALUE, 0);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.adobe.acs.commons.reports.api.ReportExecutor#getResults()
     */
    @Override
    public ResultsPage getResults() throws ReportException {
        if (page != -1) {
            return fetchPwCActivitiesAuditReport(config.getPageSize(), page);
        } else {
            return getAllResults();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.adobe.acs.commons.reports.api.ReportExecutor#setConfiguration(org.apache.sling.api.resource.Resource)
     */
    @Override
    public void setConfiguration(Resource config) {
        this.config = config.adaptTo(FileManagementReportConfig.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.adobe.acs.commons.reports.api.ReportExecutor#setPage(int)
     */
    @Override
    public void setPage(int page) {
        this.page = page;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.adobe.acs.commons.reports.api.ReportExecutor#getDetails()
     */
    @Override
    public String getDetails() throws ReportException {
        return StringUtils.EMPTY;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.adobe.acs.commons.reports.api.ReportExecutor#getParameters()
     */
    @Override
    public String getParameters() throws ReportException {
        return StringUtils.EMPTY;
    }

    /**
     * Method to get Page References for the Assets under given path.
     *
     * @param limit  page limit
     * @param offset offset index
     * @return Vital Stats
     */
    private ResultsPage fetchPwCActivitiesAuditReport(int limit, int offset) {
        List<Object> results = new ArrayList<>();
        try {
            final String path = request.getParameter(MadisonConstants.SYNDICATION_INPUT_PATH);
            if (StringUtils.isNotBlank(path)) {
                if (activitiesAuditReportService.getGlobalPath().equals(path)) {
                    results = activitiesAuditReportService.getGlobalResults();
                } else {
                    activitiesAuditReportService.getActivityReport(path, request, limit, offset, results);
                }
            }
            if(Integer.MAX_VALUE!=limit) {
                results = results.subList(offset * limit, Math.min((offset + 1) * limit, results.size()));
            }
        } catch (final Exception e) {
            log.error("Error occurred while getting fetchPwCFileManagementReport: ", e);
        }
        return new ResultsPage(results, config.getPageSize(), page);
    }
}
