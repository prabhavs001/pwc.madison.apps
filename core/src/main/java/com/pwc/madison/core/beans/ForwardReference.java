package com.pwc.madison.core.beans;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ForwardReference {

    private String path;

    @SerializedName("forwardrefs")
    @Expose
    private List<String> forwardRefs;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getForwardRefs() {
        return forwardRefs;
    }

    public void setForwardRefs(List<String> forwardRefs) {
        this.forwardRefs = forwardRefs;
    }

}
