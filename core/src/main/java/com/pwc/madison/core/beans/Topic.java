
package com.pwc.madison.core.beans;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Topic {

    @SerializedName("valid")
    @Expose
    private Boolean valid;
    @SerializedName("path")
    @Expose
    private String path;
    @SerializedName("title")
    @Expose
    private String title;
    @SerializedName("author")
    @Expose
    private String author;
    @SerializedName("docstate")
    @Expose
    private String docstate;
    @SerializedName("numMissingImages")
    @Expose
    private Integer numMissingImages;
    @SerializedName("numMissingLinks")
    @Expose
    private Integer numMissingLinks;
    @SerializedName("reviewStatus")
    @Expose
    private Integer reviewStatus;
    @SerializedName("summary")
    @Expose
    private Summary summary;

    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDocstate() {
        return docstate;
    }

    public void setDocstate(String docstate) {
        this.docstate = docstate;
    }

    public Integer getNumMissingImages() {
        return numMissingImages;
    }

    public void setNumMissingImages(Integer numMissingImages) {
        this.numMissingImages = numMissingImages;
    }

    public Integer getNumMissingLinks() {
        return numMissingLinks;
    }

    public void setNumMissingLinks(Integer numMissingLinks) {
        this.numMissingLinks = numMissingLinks;
    }

    public Integer getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(Integer reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

}
