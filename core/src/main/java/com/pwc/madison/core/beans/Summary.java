
package com.pwc.madison.core.beans;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Summary {

    @SerializedName("xrefs")
    @Expose
    private List<Xref> xrefs = null;
    @SerializedName("conrefs")
    @Expose
    private List<Xref> conrefs = null;
    @SerializedName("images")
    @Expose
    private List<Xref> images = null;
    @SerializedName("usedIn")
    @Expose
    private List<Xref> usedIn = null;
    @SerializedName("reviews")
    @Expose
    private List<Object> reviews = null;

    public List<Xref> getXrefs() {
        return xrefs;
    }

    public void setXrefs(List<Xref> xrefs) {
        this.xrefs = xrefs;
    }

    public List<Xref> getConrefs() {
        return conrefs;
    }

    public void setConrefs(List<Xref> conrefs) {
        this.conrefs = conrefs;
    }

    public List<Xref> getImages() {
        return images;
    }

    public void setImages(List<Xref> images) {
        this.images = images;
    }

    public List<Xref> getUsedIn() {
        return usedIn;
    }

    public void setUsedIn(List<Xref> usedIn) {
        this.usedIn = usedIn;
    }

    public List<Object> getReviews() {
        return reviews;
    }

    public void setReviews(List<Object> reviews) {
        this.reviews = reviews;
    }

}
