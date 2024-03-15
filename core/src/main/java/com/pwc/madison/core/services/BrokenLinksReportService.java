package com.pwc.madison.core.services;

import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;

import com.day.text.csv.Csv;
import com.pwc.madison.core.beans.BrokenLinksReportRow;
import com.pwc.madison.core.beans.PublishListenerReport;
import com.pwc.madison.core.beans.Xref;

/**
 * The Interface BrokenLinksReportService is an OSGi Service which gets the Broken Links
 * report as a list as well as csv.
 * 
 */
public interface BrokenLinksReportService {

	/**
     * Gets the BrokenLinks Report report.
     *
     * @param path             the path
     * @param resourceResolver the resource resolver
     * @return the BrokenLinks report
     */
    public Map<String, List<Xref>> populateTopicsMapFromFolder(String path, ResourceResolver resourceResolver);
    
    /**
     * Gets the BrokenLinks Report report.
     *
     * @param path             the path
     * @param resourceResolver the resource resolver
     * @return the BrokenLinks report
     */
    public Map<String, List<Xref>> populateTopicsMapFromTopics(String path, ResourceResolver resourceResolver);

	/**
	 * Gets the BrokenLinks csv report.
	 *
	 * @param path             the path
	 * @param resourceResolver the resource resolver
	 * @param csv              the csv
	 * @param writer           the writer
	 * @return the BrokenLinks csv report
	 */
	public Csv getBrokenLinksCsvReport(List<BrokenLinksReportRow> brokenLinksReportRows, Csv csv, Writer writer);
	
	/**
	 * Get OOTB report
	 * @param resourceResolver
	 * @param topicsMap
	 * @return
	 */
	public PublishListenerReport getPublishListenerReport(ResourceResolver resourceResolver,Map<String, List<Xref>> topicsMap,String cookieValue);
	
	/**
	 * Get broken links
	 * @param type
	 * @param path
	 * @param resourceResolver
	 * @return
	 */
	public List<BrokenLinksReportRow> getBrokenLinks(ResourceResolver resourceResolver,Map<String, List<Xref>> topicsMap,String cookieValue);
}
