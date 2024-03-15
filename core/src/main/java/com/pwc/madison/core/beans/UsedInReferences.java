package com.pwc.madison.core.beans;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class UsedInReferences {
	
	@SerializedName("path")
    @Expose
    private String path;
    
    @SerializedName("title")
    @Expose
    private String title;
    
    @SerializedName("contentId")
    @Expose
    private String contentId;
    
    @SerializedName("publicationDate")
    @Expose
    private String publicationDate;
	
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

	public String getContentId() {
		return contentId;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	public String getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(String publicationDate) {
		this.publicationDate = publicationDate;
	}

}
