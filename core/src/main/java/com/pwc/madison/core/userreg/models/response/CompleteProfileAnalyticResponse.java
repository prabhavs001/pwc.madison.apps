package com.pwc.madison.core.userreg.models.response;

import java.util.List;

/**
 * 
 * Model represents response data to be used for analytics on complete profile success
 *
 */
public class CompleteProfileAnalyticResponse {
	
	private List<String> industryTags;
	private List<String> titleTags;
	private String functionalRoleTitle;
	private String industryTitle;

	public List<String> getIndustryTags() {
		return industryTags;
	}

	public void setIndustryTags(List<String> industryTags) {
		this.industryTags = industryTags;
	}

	public List<String> getTitleTags() {
		return titleTags;
	}

	public void setTitleTags(List<String> titleTags) {
		this.titleTags = titleTags;
	}

	public String getFunctionalRoleTitle() {
		return functionalRoleTitle;
	}

	public void setFunctionalRoleTitle(String functionalRoleTitle) {
		this.functionalRoleTitle = functionalRoleTitle;
	}

	public String getIndustryTitle() {
		return industryTitle;
	}

	public void setIndustryTitle(String industryTitle) {
		this.industryTitle = industryTitle;
	}
}
