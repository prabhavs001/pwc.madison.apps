package com.pwc.madison.core.services;

import java.io.Writer;

import com.day.text.csv.Csv;

/**
 * This Service Provides Implementation to get syndication failure details.
 */
public interface SyndicationFailureReportService {

	/**
	 * write syndication Failure report in csv
	 * 
	 * @param sourcePath source folder path
	 * @param csv
	 * @param writer
	 * @return
	 */
	public void writeFailureReport(String sourcePath, Csv csv, Writer writer);
	
	/**write publish details for all the publishing points of source and destination territories under sourcePath folder in csv
	 * 
	 * @param sourcePath
	 * @param csv
	 * @param writer
	 */
	public void writePublishReport(String sourcePath, Csv csv, Writer writer);

}
