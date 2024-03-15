package com.pwc.madison.core.models;

import java.util.List;

import com.pwc.madison.core.constants.DITAConstants;

/**
 * Most Popular Item POJO that provides getters 
 * and setters for the properties of the MostPopularList.
 */
public class Item {

	private String articlePagePath;
	private String abstractDesc;
	private List<String> summaryText; 
	private String topicTitle;
	private String topicText;
	private String image;
	private String linkText;
	private String tileBackground;	
	private String noOfViews;
	private String Date;
	private boolean hidePublicationDate;
	private String publicationDate;
	private String unformattedPublicationDate;
	private String revisedDate;
	private String contentId;
	private String contentType;
	private String country;
	private boolean isUrlInternal;
	private String standardSetterType;
	private String isFallbackItem;
	private String ctaLabel;
	private String contentFieldValue;
	private String renditionStyle;
	private String imagePath;
	private String path;
	private boolean openInNewWindow;
	private String accessType;
	private String licenseTypes;
	
	public String getAccessType() {
		return accessType;
	}

	public void setAccessType(String accessType) {
		this.accessType = accessType;
	}

	public String getLicenseTypes() {
		return licenseTypes;
	}

	public void setLicenseTypes(String licenseTypes) {
		this.licenseTypes = licenseTypes;
	}

	public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getSummaryText() {
		return summaryText;
	}

	public void setSummaryText(List<String> summaryText) {
		this.summaryText = summaryText;
	}
	
	public String getAbstractDesc() {
		return abstractDesc;
	}

	public void setAbstractDesc(String abstractDesc) {
		this.abstractDesc = abstractDesc;
	}
	
	public String getContentFieldValue() {
		return contentFieldValue;
	}

	public void setContentFieldValue(String contentFieldValue) {
		this.contentFieldValue = contentFieldValue;
	}

	public static String getPwcSourceValue() {
		return DITAConstants.PWC_SOURCE_VALUE;
	}

	public String getIsFallbackItem() {
		return isFallbackItem;
	}

	public void setIsFallbackItem(String isFallbackItem) {
		this.isFallbackItem = isFallbackItem;
	}

	public String getCtaLabel() {
		return ctaLabel;
	}

	public void setCtaLabel(String ctaLabel) {
		this.ctaLabel = ctaLabel;
	}

	public String getStandardSetterType() {
		return standardSetterType;
	}

	public void setStandardSetterType(String standardSetterType) {
		this.standardSetterType = standardSetterType;
	}

	public boolean isUrlInternal() {
		return isUrlInternal;
	}

	public void setUrlInternal(boolean isUrlInternal) {
		this.isUrlInternal = isUrlInternal;
	}

	public String getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(String publicationDate) {
		this.publicationDate = publicationDate;
	}

	public String getUnformattedPublicationDate() {
		return unformattedPublicationDate;
	}

	public void setUnformattedPublicationDate(String unformattedPublicationDate) {
		this.unformattedPublicationDate = unformattedPublicationDate;
	}

	public String getRevisedDate() {
		return revisedDate;
	}

	public void setRevisedDate(String revisedDate) {
		this.revisedDate = revisedDate;
	}
	
	public String getContentId() {
		return contentId;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getDate() {
		return Date;
	}

	public void setDate(String date) {
		Date = date;
	}

	public boolean isHidePublicationDate() {
		return hidePublicationDate;
	}

	public void setHidePublicationDate(boolean hidePublicationDate) {
		this.hidePublicationDate = hidePublicationDate;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getTopicTitle() {
		return topicTitle;
	}

	public void setTopicTitle(String topicTitle) {
		this.topicTitle = topicTitle;
	}

	public String getTopicText() {
		return topicText;
	}

	public void setTopicText(String topicText) {
		this.topicText = topicText;
	}


	public String getLinkText() {
		return linkText;
	}

	public void setLinkText(String linkText) {
		this.linkText = linkText;
	}

	public String getTileBackground() {
		return tileBackground;
	}

	public void setTileBackground(String tileBackground) {
		this.tileBackground = tileBackground;
	}

	public String getArticlePagePath() {
		return articlePagePath;
	}

	public void setArticlePagePath(String articlePagePath) {
		this.articlePagePath = articlePagePath;
	}

	public String getNoOfViews() {
		return noOfViews;
	}

	public void setNoOfViews(String noOfViews) {
		this.noOfViews = noOfViews;
	}

	public String getRenditionStyle() {
		return renditionStyle;
	}

	public void setRenditionStyle(String renditionStyle) {
		this.renditionStyle = renditionStyle;
	}

	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	public boolean getOpenInNewWindow() {
		return openInNewWindow;
	}

	public void setOpenInNewWindow(boolean openInNewWindow) {
		this.openInNewWindow = openInNewWindow;
	}

    @Override
    public String toString() {
        return "Item [articlePagePath=" + articlePagePath + ", abstractDesc=" + abstractDesc + ", summaryText="
                + summaryText + ", topicTitle=" + topicTitle + ", topicText=" + topicText + ", image=" + image
                + ", linkText=" + linkText + ", tileBackground=" + tileBackground + ", noOfViews=" + noOfViews
                + ", Date=" + Date + ", publicationDate=" + publicationDate + ", revisedDate=" + revisedDate
                + ", contentId=" + contentId + ", contentType=" + contentType + ", country=" + country
                + ", isUrlInternal=" + isUrlInternal + ", standardSetterType=" + standardSetterType
                + ", isFallbackItem=" + isFallbackItem + ", ctaLabel=" + ctaLabel + ", contentFieldValue="
                + contentFieldValue + ", renditionStyle=" + renditionStyle + ", imagePath=" + imagePath + ", path="
                + path + "]";
    }

}
