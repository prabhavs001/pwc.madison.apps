package com.pwc.madison.core.userreg.models.response;

import java.util.Map;

/**
 * 
 * Model to represent each UserReg rest API response. Every UserReg rest API response model must extend this class.
 *
 */
public class FavoriteListResponse {

    private Data data;

    public FavoriteListResponse(Data data) {
        super();
        this.data = data;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public class Data {

        private Map<String, FavoriteFolderResponse> favoriteFolders;

        private String status;

        public Map<String, FavoriteFolderResponse> getFavoriteFolders() {
            return favoriteFolders;
        }

        public void setFavoriteFolders(Map<String, FavoriteFolderResponse> favoriteFolders, String status) {
            this.favoriteFolders = favoriteFolders;
            this.status = status;
        }

        public Data(Map<String, FavoriteFolderResponse> favoriteFolders) {
            super();
            this.favoriteFolders = favoriteFolders;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

    }

}
