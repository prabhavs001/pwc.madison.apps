package com.pwc.madison.core.userreg.models.request;

import java.util.List;

/**
 * 
 * Model to represent data to send to UserReg Rest delete favorite list request API.
 *
 */
public class DeleteFavoriteListRequest extends UserRegRequest {

    public DeleteFavoriteListRequest(List<String> pagePaths, boolean updatePagesData, String currentPageTerritoryCode) {
        super();
        this.pagePaths = pagePaths;
        this.updatePagesData = updatePagesData;
        this.currentPageTerritoryCode = currentPageTerritoryCode;
    }

    private List<String> pagePaths;


    private boolean updatePagesData;
    
    private String currentPageTerritoryCode;

    public List<String> getPagePaths() {
        return pagePaths;
    }

    public void setPagePath(List<String> pagePaths) {
        this.pagePaths = pagePaths;
    }

    public Boolean getUpdatePagesData() {
        return updatePagesData;
    }

    public void setUpdatesPageData(Boolean updatePagesData) {
        this.updatePagesData = updatePagesData;
    }
    
    public String getCurrentPageTerritoryCode() {
        return currentPageTerritoryCode;
    }

    public void setCurrentPageTerritoryCode(String currentPageTerritoryCode) {
        this.currentPageTerritoryCode = currentPageTerritoryCode;
    }
    
}
