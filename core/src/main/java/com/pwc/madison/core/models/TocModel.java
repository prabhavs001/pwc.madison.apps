package com.pwc.madison.core.models;

public interface TocModel {

    /**
     * Returns the base page path, It is used to fetch the TOC list by the TOCServlet.
     *
     * @return
     */
    public String getBasePath();

    /**
     * Returns the flag to determine whether TOC to be displayed or not
     */
    public Boolean showToc();
    
    public boolean isChapterToc();

	String getChapterTocBasePath();

}
