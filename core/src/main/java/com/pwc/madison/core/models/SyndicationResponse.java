package com.pwc.madison.core.models;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SyndicationResponse {

    @SerializedName("destinationPath")
    @Expose
    private String destinationPath;
    @SerializedName("successCount")
    @Expose
    private Integer successCount;
    @SerializedName("errorFiles")
    @Expose
    private List<String> errorFiles = new ArrayList<>();
    @SerializedName("errorMsg")
    @Expose
    private String errorMsg;

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(final String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(final String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(final Integer successCount) {
        this.successCount = successCount;
    }

    public List<String> getErrorFiles() {
        return errorFiles;
    }

    public void setErrorFiles(final List<String> errorFiles) {
        this.errorFiles = errorFiles;
    }

}
