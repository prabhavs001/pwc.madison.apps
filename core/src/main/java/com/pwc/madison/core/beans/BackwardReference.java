package com.pwc.madison.core.beans;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class BackwardReference {

    private String path;

    @SerializedName("refs")
    @Expose
    private List<String> backwardRefs;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getBackwardRefs() {
        return backwardRefs;
    }

    public void setBackwardRefs(List<String> backwardRefs) {
        this.backwardRefs = backwardRefs;
    }

}
