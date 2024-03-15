package com.pwc.madison.core.userreg.models.response;

import java.util.List;

/**
 * 
 * Model to represent the response data from UserReg add/remove/get favorite list request API.
 *
 */
public class FavoriteFolderResponse {

    private long folderId;

    private String folderName;

    private List<FavoriteList> list;

    public FavoriteFolderResponse(long folderId, String folderName, List<FavoriteList> list) {
        super();
        this.folderId = folderId;
        this.folderName = folderName;
        this.list = list;
    }

    public class FavoriteList {

        private String pagePath;

        private String pageHref;

        private String title;

        private String description;
        
        private String country;

        private String content;
        
        private boolean hidePublicationDate;

        private String publicationDate;

        private String favoritedDate;

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

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
	    this.country = country;
        }

        public String getContent() {
            return content;
        }
        
        public boolean isHidePublicationDate() {
            return hidePublicationDate;
        }
        
        public void setHidePublicationDate(boolean hidePublicationDate) {
            this.hidePublicationDate = hidePublicationDate;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getPublicationDate() {
            return publicationDate;
        }

        public void setPublicationDate(String publicationDate) {
            this.publicationDate = publicationDate;
        }

        public String getFavoritedDate() {
            return favoritedDate;
        }

        public void setFavoritedDate(String favoritedDate) {
            this.favoritedDate = favoritedDate;
        }

        public FavoriteList(String pagePath, String pageHref, String title, String description, String country, String content,
                String publicationDate, String favoritedDate) {
            super();
            this.pagePath = pagePath;
            this.pageHref = pageHref;
            this.title = title;
            this.description = description;
            this.country = country;
            this.content = content;
            this.publicationDate = publicationDate;
            this.favoritedDate = favoritedDate;
        }    

    }

    public long getFolderId() {
        return folderId;
    }

    public void setFolderId(long folderId) {
        this.folderId = folderId;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public List<FavoriteList> getList() {
        return list;
    }

    public void setList(List<FavoriteList> list) {
        this.list = list;
    }

}