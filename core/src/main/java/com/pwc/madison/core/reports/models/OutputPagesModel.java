/**
 *
 */
package com.pwc.madison.core.reports.models;

public interface OutputPagesModel {

    /**
     * Method to get number of output pages for a ditamap.
     *
     * @return Number of Pages
     */
    Object getNumberOfPages();

    /**
     * Method to get the number of topics for a ditamap.
     * 
     * @return topics count
     */
    Object getTopicsCount();

}
