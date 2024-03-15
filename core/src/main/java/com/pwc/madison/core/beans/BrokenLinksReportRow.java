package com.pwc.madison.core.beans;

import java.util.List;

/**
 * 
 * The Class BrokenLinksReportRow is a bean for defining the report columns in
 * Broken Links Report Report for Topics.
 * 
 */
public class BrokenLinksReportRow {

	private String topicPath;

	private List<Xref> brokenLinks;
	
	private List<Xref> contentReferences;
	
	private List<Xref> aemPageLinks;


	public List<Xref> getBrokenLinks() {
		return brokenLinks;
	}

	public void setBrokenLinks(List<Xref> brokenLinks) {
		this.brokenLinks = brokenLinks;
	}

    /**
     * @return the topicPath
     */
    public String getTopicPath() {
        return topicPath;
    }

    /**
     * @param topicPath the topicPath to set
     */
    public void setTopicPath(String topicPath) {
        this.topicPath = topicPath;
    }

    /**
     * @return the contentReferences
     */
    public List<Xref> getContentReferences() {
        return contentReferences;
    }

    /**
     * @param contentReferences the contentReferences to set
     */
    public void setContentReferences(List<Xref> contentReferences) {
        this.contentReferences = contentReferences;
    }

    /**
     * @return the aemPageLinks
     */
    public List<Xref> getAemPageLinks() {
        return aemPageLinks;
    }

    /**
     * @param aemPageLinks the aemPageLinks to set
     */
    public void setAemPageLinks(List<Xref> aemPageLinks) {
        this.aemPageLinks = aemPageLinks;
    }

}
