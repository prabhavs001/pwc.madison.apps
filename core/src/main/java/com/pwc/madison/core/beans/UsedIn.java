
package com.pwc.madison.core.beans;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class UsedIn {

    @SerializedName("path")
    @Expose
    private String path;
    @SerializedName("linkStatus")
    @Expose
    private Boolean linkStatus;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Boolean getLinkStatus() {
        return linkStatus;
    }

    public void setLinkStatus(Boolean linkStatus) {
        this.linkStatus = linkStatus;
    }

}
