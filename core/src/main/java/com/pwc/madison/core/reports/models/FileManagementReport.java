package com.pwc.madison.core.reports.models;

import com.pwc.madison.core.beans.FileManagementReportRow;

/**
 * Interface created for File Management Report
 * 
 * @author sevenkat
 *
 */
public interface FileManagementReport {

    /**
     * Method to return Display text of Selected Option
     * 
     * @return Display value
     */
    public FileManagementReportRow getEachRow();
}
