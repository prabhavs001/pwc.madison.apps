package com.pwc.madison.core.beans;

import java.util.List;

public class FileManagementReportRow {

    private List<String> assetPath;

    private List<String> referenceUrls;

    private List<String> pageStatus;

    /**
     * @return the assetPath
     */
    public List<String> getAssetPath() {
        return assetPath;
    }

    /**
     * @param assetPath
     *            the assetPath to set
     */
    public void setAssetPath(List<String> assetPath) {
        this.assetPath = assetPath;
    }

    /**
     * @return the referenceUrls
     */
    public List<String> getReferenceUrls() {
        return referenceUrls;
    }

    /**
     * @param referenceUrls
     *            the referenceUrls to set
     */
    public void setReferenceUrls(List<String> referenceUrls) {
        this.referenceUrls = referenceUrls;
    }

    /**
     * @return the pageStatus
     */
    public List<String> getPageStatus() {
        return pageStatus;
    }

    /**
     * @param pageStatus
     *            the pageStatus to set
     */
    public void setPageStatus(List<String> pageStatus) {
        this.pageStatus = pageStatus;
    }
}
