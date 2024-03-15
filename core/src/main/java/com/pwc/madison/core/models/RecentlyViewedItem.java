package com.pwc.madison.core.models;

public class RecentlyViewedItem extends Item {
	
    private String pagePath;
    private String contentTitle;
    private String newsSource;
    private String sourceLink;
    private Boolean isInternalUrl;
    private String itemViewedDate;
    
	public String getPagePath() {
		return pagePath;
	}
	
	public void setPagePath(String pagePath) {
		this.pagePath = pagePath;
	}
	
	public String getContentTitle() {
		return contentTitle;
	}

	public void setContentTitle(String contentTitle) {
		this.contentTitle = contentTitle;
	}

	public String getNewsSource() {
		return newsSource;
	}
	
	public void setNewsSource(String newsSource) {
		this.newsSource = newsSource;
	}
	
	public String getSourceLink() {
		return sourceLink;
	}
	
	public void setSourceLink(String sourceLink) {
		this.sourceLink = sourceLink;
	}
	
	public Boolean getIsInternalUrl() {
		return isInternalUrl;
	}

	public void setIsInternalUrl(Boolean isInternalUrl) {
		this.isInternalUrl = isInternalUrl;
	}
		
	public String getItemViewedDate() {
		return itemViewedDate;
	}

	public void setItemViewedDate(String itemViewedDate) {
		this.itemViewedDate = itemViewedDate;
	}

	@Override
	public String toString() {
		return "RecentlyViewedItem [pagePath=" + pagePath + ", contentTitle=" + contentTitle + ", contentId="
				+ getContentId() + ", newsSource=" + newsSource + ", sourceLink=" + sourceLink + ", publicationDate="
				+ getPublicationDate() + ", isInternalUrl=" + isInternalUrl + ", itemViewedDate=" + itemViewedDate + "]";
	}
	
}
