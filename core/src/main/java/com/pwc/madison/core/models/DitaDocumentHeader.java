package com.pwc.madison.core.models;

public interface DitaDocumentHeader {

    /**
     * @return TOC Page title
     */
    public String getTocTitle();

    /**
     * @return Share or Mail icon Visibility status
     */
    public Boolean isShareViaEmailOnly();
    
    /**
     * @return Disable PDF Download
     */
    public Boolean disablePDFDownload();

    /**
     * @return Hide SearchWithInDocument Icon
     */
    public Boolean hideSearchWithInDocument();

    /**
     * @return Show Static TOC
     */
    public Boolean showStaticToc();

    /**
     * @return runMode of the instance
     */
    public String getRunMode();

}
