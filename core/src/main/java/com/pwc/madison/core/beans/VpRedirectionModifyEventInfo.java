package com.pwc.madison.core.beans;

/**
 * 
 * POJO representing the information for each Url modification Event for VP to VP redirection.
 * 
 */
public class VpRedirectionModifyEventInfo {

    /**
     * Old URL of the page which was preset before event.
     */
    private String oldUrl;

    /**
     * New URL of the page which old URL changed to after event.
     */
    private String newUrl;

    /**
     * FM GUID unique to dita topic of the page.
     */
    private String fmguid;

    public String getOldUrl() {
        return oldUrl;
    }

    public void setOldUrl(String oldUrl) {
        this.oldUrl = oldUrl;
    }

    public String getNewUrl() {
        return newUrl;
    }

    public void setNewUrl(String newUrl) {
        this.newUrl = newUrl;
    }

    public String getFmguid() {
        return fmguid;
    }

    public void setFmguid(String fmguid) {
        this.fmguid = fmguid;
    }

    @Override
    public String toString() {
        return "VpRedirectionModifyEventInfo [oldUrl=" + oldUrl + ", newUrl=" + newUrl + ", fmguid=" + fmguid + "]";
    }

    public VpRedirectionModifyEventInfo(String oldUrl, String newUrl, String fmguid) {
        super();
        this.oldUrl = oldUrl;
        this.newUrl = newUrl;
        this.fmguid = fmguid;
    }

}
