package com.pwc.madison.core.beans;

/**
 * 
 * POJO represents the parent path which child pages are to be includes in sitemap XML file.
 */
public class SitemapParent {

    /**
     * {@link String} the parent page path which child pages are to be included in sitemap XML file.
     */
    private String parentPagePath;

    /**
     * {@link Boolean} defines whether the parent page itself is to be included in sitemap XML file or not. Mostly used
     * to handle usecase where ditaroot home pages are not to be included and landing home pages are to be included.
     */
    private boolean includeParentPagePath;

    public String getParentPagePath() {
        return parentPagePath;
    }

    public void setParentPagePath(String parentPagePath) {
        this.parentPagePath = parentPagePath;
    }

    public boolean isIncludeParentPagePath() {
        return includeParentPagePath;
    }

    public void setIncludeParentPagePath(boolean includeParentPagePath) {
        this.includeParentPagePath = includeParentPagePath;
    }

    public SitemapParent(String parentPagePath, boolean includeParentPagePath) {
        super();
        this.parentPagePath = parentPagePath;
        this.includeParentPagePath = includeParentPagePath;
    }

    @Override
    public String toString() {
        return "SitemapParent [parentPagePath=" + parentPagePath + ", includeParentPagePath=" + includeParentPagePath
                + "]";
    }

}
