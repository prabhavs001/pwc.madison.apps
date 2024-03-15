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
import com.pwc.madison.core.reports.configs.VitalStatsReportConfig;
import com.pwc.madison.core.services.VitalStatsReportService;

/**
 * 
 * @author kartikkarnayil
 * 
 *         Class for getting Vital Stats Report for the Assets under given path.
 *
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class PWCVitalStatsReportExecutor implements ReportExecutor {

    private VitalStatsReportConfig config;

    private int page;

    private final SlingHttpServletRequest request;

    private static final Logger log = LoggerFactory.getLogger(PWCVitalStatsReportExecutor.class);

    @Inject
    private VitalStatsReportService vitalStatsReportService;

    /**
     * PWCVitalStatsReportExecutor constructor
     *
     * @param request
     *            the request
     */
    public PWCVitalStatsReportExecutor(SlingHttpServletRequest request) {
        this.request = request;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.adobe.acs.commons.reports.api.ReportExecutor#getAllResults()
     */
    @Override
    public ResultsPage getAllResults() throws ReportException {
        return fetchPWCVitalStatsReport(Integer.MAX_VALUE, 0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.adobe.acs.commons.reports.api.ReportExecutor#getResults()
     */
    @Override
    public ResultsPage getResults() throws ReportException {
        if (page != -1) {
            return fetchPWCVitalStatsReport(config.getPageSize(), config.getPageSize() * page);
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
        this.config = config.adaptTo(VitalStatsReportConfig.class);
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
     * Method to get Vital Stats for the Assets under given path.
     * 
     * @param limit
     *            page limit
     * @param offset
     *            offset index
     * @return Vital Stats
     */
    private ResultsPage fetchPWCVitalStatsReport(int limit, int offset) {
        final List<Object> results = new ArrayList<>();

        try {
            final String path = request.getParameter(MadisonConstants.SYNDICATION_INPUT_PATH);

            if (!StringUtils.isBlank(path)) {
                vitalStatsReportService.getVitalStatsReportWithPagination(path, request.getResourceResolver(), limit,
                        offset, results);
            }

        } catch (final Exception e) {
            log.error("Error occured while getting PWC Vital Stats: ", e);
        }
        return new ResultsPage(results, config.getPageSize(), page);
    }
}
