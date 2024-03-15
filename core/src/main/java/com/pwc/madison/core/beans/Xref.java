
package com.pwc.madison.core.beans;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Xref {

    @SerializedName("path")
    @Expose
    private String path;
    @SerializedName("linkStatus")
    @Expose
    private Boolean linkStatus;
    private String scope;

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

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
    

}
