package com.pwc.madison.core.models;

import com.pwc.madison.core.constants.DITAConstants;

public class InsightsHomePageTile extends Item {

	private String linkLabel;
	private String linkUrl;
	private boolean isGhostTemplateItem;
	private String abstractDescription;

	public String getAbstractDescription() {
		return abstractDescription;
	}

	public void setAbstractDescription(String abstractDescription) {
		this.abstractDescription = abstractDescription;
	}

	public boolean isGhostTemplateItem() {
		return isGhostTemplateItem;
	}

	public void setGhostTemplateItem(boolean isGhostTemplateItem) {
		this.isGhostTemplateItem = isGhostTemplateItem;
	}

	public String getLinkUrl() {
		return linkUrl;
	}

	public void setLinkUrl(String linkUrl) {
		this.linkUrl = linkUrl;
	}

	public String getLinkLabel() {
		return linkLabel;
	}

	public void setLinkLabel(String linkLabel) {
		this.linkLabel = linkLabel;
	}
	
	public static String getPwcSourceValue() {
		return DITAConstants.PWC_SOURCE_VALUE;
	}

}
