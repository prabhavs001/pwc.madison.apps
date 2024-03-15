package com.pwc.madison.core.models;

/**
 *  News List Component's Pojo.
 *  This class provides getters and setters for the properties of a News List Item .
 */
public class NewsListItem extends Item {
	
	private String newsPagePath;
	private String category;
	private String fallbackItemPath;
	private String newsTitle;
	private String newsSource;
	private String sourceLink;
	private String fallbackCategory;
	private Boolean isInternalUrl;
	
	public String getNewsPagePath() {
		return newsPagePath;
	}
	
	public void setNewsPagePath(String newsPagePath) {
		this.newsPagePath = newsPagePath;
	}
	
	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getFallbackCategory() {
		return fallbackCategory;
	}

	public void setFallbackCategory(String fallbackCategory) {
		this.fallbackCategory = fallbackCategory;
	}

	public String getFallbackItemPath() {
		return fallbackItemPath;
	}
	
	public void setFallbackItemPath(String fallbackItemPath) {
		this.fallbackItemPath = fallbackItemPath;
	}
	
	public String getNewsTitle() {
		return newsTitle;
	}
	
	public void setNewsTitle(String newsTitle) {
		this.newsTitle = newsTitle;
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

	@Override
	public String toString() {
		return "NewsListItem [newsPagePath=" + newsPagePath + ", category=" + category + ", fallbackItemPath="
				+ fallbackItemPath + ", newsTitle=" + newsTitle + ", newsSource=" + newsSource + ", sourceLink="
				+ sourceLink + ", fallbackCategory=" + fallbackCategory + ", isInternalUrl=" + isInternalUrl + "]";
	}
}
