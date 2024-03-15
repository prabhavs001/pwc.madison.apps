package com.pwc.madison.core.services;

import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.sling.api.SlingHttpServletRequest;

/**
 * The Interface ReportService is used to configure Custom Reports Execution
 */
public interface WorkflowReportService {

    public List<Object> populateQueryResults(String assetPath, SlingHttpServletRequest request, int limit, int offset, List<Object> results)
            throws RepositoryException;

}
