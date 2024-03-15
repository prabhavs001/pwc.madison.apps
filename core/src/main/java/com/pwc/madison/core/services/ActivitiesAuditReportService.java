package com.pwc.madison.core.services;

import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;

/**
 * The Interface ActivitiesAuditReportService is used to get the activity fields such as user login, modified date and
 * activity.
 */
public interface ActivitiesAuditReportService {

    public List<Object> getActivityReport(String path, SlingHttpServletRequest request, int limit, int offset,
            List<Object> results);
    public String getGlobalPath() ;
    public List<Object> getGlobalResults();
}
