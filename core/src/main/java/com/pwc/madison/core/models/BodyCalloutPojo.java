package com.pwc.madison.core.models;

import com.pwc.madison.core.models.impl.CalloutDetailedContent;
import com.pwc.madison.core.models.impl.CalloutModel;

/**
 * @author Vijay
 */
public class BodyCalloutPojo {

    private String thumbnail;
    private String publishedDate;
    private String revisedDate;
    private String title;
    private String description;
    private String url;
    private String edNote;
    private String category;
    private String contentId;
    private String format;
    private String scope;
    private String contentFieldValue;
	private String country;
	private String imgAltText;
	private String calloutAccessLevel;
	private CalloutDetailedContent calloutDetailedContent;
	private String privateGroupType;

	public BodyCalloutPojo(CalloutModel calloutModel) {
		this.thumbnail = calloutModel.getImage();
		this.imgAltText = calloutModel.getAltText();
		this.publishedDate = calloutModel.getPublicationDate();
		this.revisedDate = calloutModel.getRevisionDate();
		this.title = calloutModel.getTitle();
		this.description = calloutModel.getDescription();
		this.url = calloutModel.getLink();
		this.edNote = calloutModel.getEdNote();
		this.category = calloutModel.getCategory();
		this.contentId = calloutModel.getContentId();
		this.format = calloutModel.getFormat();
		this.scope = calloutModel.getScope();
		this.calloutDetailedContent = calloutModel.getCalloutDetailedContent();
		this.contentFieldValue = calloutModel.getContentFieldValue();
		this.country = calloutModel.getCountry();
		this.calloutAccessLevel = calloutModel.getCalloutAccessLevel();
		this.privateGroupType = calloutModel.getPrivateGroupType();
	}
	public String getThumbnail() {
		return thumbnail;
	}
	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}
	public String getPublishedDate() {
		return publishedDate;
	}
	public void setPublishedDate(String publishedDate) {
		this.publishedDate = publishedDate;
	}
	public String getRevisedDate() {
		return revisedDate;
	}
	public void setRevisedDate(String revisedDate) {
		this.revisedDate = revisedDate;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getEdNote() {
		return edNote;
	}
	public void setEdNote(String edNote) {
		this.edNote = edNote;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public String getContentId() {
		return contentId;
	}
	public void setContentId(String contentId) {
		this.contentId = contentId;
	}
	public String getFormat() {
		return format;
	}
	public void setFormat(String format) {
		this.format = format;
	}
	public String getScope() {
		return scope;
	}
	public void setScope(String scope) {
		this.scope = scope;
	}
	public CalloutDetailedContent getCalloutDetailedContent() {
		return calloutDetailedContent;
	}
	public void setCalloutDetailedContent(CalloutDetailedContent calloutDetailedContent) {
		this.calloutDetailedContent = calloutDetailedContent;
	}
	public String getContentFieldValue() {
		return contentFieldValue;
	}
	public void setContentFieldValue(String contentFieldValue) {
		this.contentFieldValue = contentFieldValue;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public String getImgAltText() {
		return imgAltText;
	}
	public void setImgAltText(String imgAltText) {
		this.imgAltText = imgAltText;
	}
	public String getCalloutAccessLevel() {
		return calloutAccessLevel;
	}
	public void setCalloutAccessLevel(String calloutAccessLevel) {
		this.calloutAccessLevel = calloutAccessLevel;
	}
	public String getPrivateGroupType() {
		return privateGroupType;
	}
	public void setPrivateGroupType(String privateGroupType) {
		this.privateGroupType = privateGroupType;
	}
}
