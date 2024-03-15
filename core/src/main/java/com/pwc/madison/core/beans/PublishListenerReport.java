
package com.pwc.madison.core.beans;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PublishListenerReport {

    @SerializedName("totalCount")
    @Expose
    private Integer totalCount;
    @SerializedName("numMissingElements")
    @Expose
    private Integer numMissingElements;
    @SerializedName("numInDraft")
    @Expose
    private Integer numInDraft;
    @SerializedName("numInReview")
    @Expose
    private Integer numInReview;
    @SerializedName("numReviewed")
    @Expose
    private Integer numReviewed;
    @SerializedName("topics")
    @Expose
    private List<Topic> topics = null;

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getNumMissingElements() {
        return numMissingElements;
    }

    public void setNumMissingElements(Integer numMissingElements) {
        this.numMissingElements = numMissingElements;
    }

    public Integer getNumInDraft() {
        return numInDraft;
    }

    public void setNumInDraft(Integer numInDraft) {
        this.numInDraft = numInDraft;
    }

    public Integer getNumInReview() {
        return numInReview;
    }

    public void setNumInReview(Integer numInReview) {
        this.numInReview = numInReview;
    }

    public Integer getNumReviewed() {
        return numReviewed;
    }

    public void setNumReviewed(Integer numReviewed) {
        this.numReviewed = numReviewed;
    }

    public List<Topic> getTopics() {
        return topics;
    }

    public void setTopics(List<Topic> topics) {
        this.topics = topics;
    }

}
