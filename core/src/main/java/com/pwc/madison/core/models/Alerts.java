package com.pwc.madison.core.models;

public class Alerts extends Item {

    private String selectedTag;
    private String dismissedPagesValue;
    private Integer cookieExpiryHours;
    private String ctaLink;
    private Boolean isInternalUrl;
	
	public Boolean getIsInternalUrl() {
		return isInternalUrl;
	}

	public void setIsInternalUrl(Boolean isInternalUrl) {
		this.isInternalUrl = isInternalUrl;
	}

	public String getCtaLink() {
        return ctaLink;
    }

    public void setCtaLink(final String ctaLink) {
        this.ctaLink = ctaLink;
    }

    public Integer getCookieExpiryHours() {
        return cookieExpiryHours;
    }

    public void setCookieExpiryHours(final Integer cookieExpiryHours) {
        this.cookieExpiryHours = cookieExpiryHours;
    }

    public String getDismissedPagesValue() {
        return dismissedPagesValue;
    }

    public void setDismissedPagesValue(final String dismissedPagesValue) {
        this.dismissedPagesValue = dismissedPagesValue;
    }

    public String getSelectedTag() {
        return selectedTag;
    }

    public void setSelectedTag(final String selectedTag) {
        this.selectedTag = selectedTag;
    }

    @Override
	public String toString() {
		return "Alerts [selectedTag=" + selectedTag + ", dismissedPagesValue=" + dismissedPagesValue
				+ ", cookieExpiryHours=" + cookieExpiryHours + ", ctaLink=" + ctaLink + ", isInternalUrl="
				+ isInternalUrl + "]";
	}

}
