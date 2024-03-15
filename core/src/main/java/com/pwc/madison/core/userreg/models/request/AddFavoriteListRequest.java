package com.pwc.madison.core.userreg.models.request;

/**
 * 
 * Model to represent data to send to UserReg Rest add favorite list request API.
 *
 */
public class AddFavoriteListRequest extends UserRegRequest {

    public AddFavoriteListRequest(String pagePath, String pageHref, boolean updatePagesData) {
        super();
        this.pagePath = pagePath;
        this.pageHref = pageHref;
        this.updatePagesData = updatePagesData;
    }

    private String pagePath;

    private String pageHref;

    private boolean updatePagesData;

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    public String getPageHref() {
        return pageHref;
    }

    public void setPageHref(String pageHref) {
        this.pageHref = pageHref;
    }

    public Boolean getUpdatePagesData() {
        return updatePagesData;
    }

    public void setUpdatesPageData(Boolean updatePagesData) {
        this.updatePagesData = updatePagesData;
    }

}
