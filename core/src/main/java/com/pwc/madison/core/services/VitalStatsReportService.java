package com.pwc.madison.core.services;

import java.io.Writer;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.ResourceResolver;

import com.day.text.csv.Csv;
import com.pwc.madison.core.beans.VitalStatsReportRow;

/**
 * The Interface VitalStatsReportService is an OSGi Service which gets the vital stats report as a list as well as csv.
 * 
 */
public interface VitalStatsReportService {

    /**
     * Gets the vital stats report.
     *
     * @param path
     *            the path
     * @param resourceResolver
     *            the resource resolver
     * @return the vital stats report
     */
    public List<VitalStatsReportRow> getVitalStatsReport(String path, ResourceResolver resourceResolver);

    /**
     * Gets the vital stats csv report.
     *
     * @param path
     *            the path
     * @param resourceResolver
     *            the resource resolver
     * @param csv
     *            the csv
     * @param writer
     *            the writer
     * @return the vital stats csv report
     */
    public Csv getVitalStatsCsvReport(String path, ResourceResolver resourceResolver, Csv csv, Writer writer);

    /**
     * Method to get vital stats for report with pagination capability
     * 
     * @param path
     *            the path
     * @param resourceResolver
     *            the resource resolver
     * @param start
     *            the start index
     * @param hitsPerPage
     *            the hits per page
     * @param results
     *            report results array
     * @throws RepositoryException 
     */
    void getVitalStatsReportWithPagination(String path, ResourceResolver resourceResolver, int start, int hitsPerPage,
            List<Object> results) throws RepositoryException;
    
}
