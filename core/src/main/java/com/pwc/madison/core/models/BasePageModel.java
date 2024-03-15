package com.pwc.madison.core.models;

import java.util.Date;

/**
 * Base page component's model. All PWC Madison pages are derived from this component either directly or indirectly.
 */
public interface BasePageModel {

    public String getDataLayerBookMap();

    public String getDataLayerTopic();

    public String getDataLayerMap();

    /**
     * @return pageTitle
     */
    public String getPageTitle();

    /**
     * @return pageUrl
     */
    public String getPageUrl();

    /**
     * @return pageImage
     */
    public String getPageImage();

    /**
     * @return pageDesc
     */
    public String getPageDesc();

    /**
     * @return keywords defined to drive search
     */
    String[] getKeywords();
    
    /**
     * @return guidance terms defined to drive search suggested guidance
     */
    String[] getGuidanceTerms();
    
    /**
     * @return guidance title defined to drive search suggested guidance
     */
    String getSuggestedGuidance();

    /**
     * @return revised date of the page
     */
    Date getRevisedDate();

    /**
     * @return publication date of page
     */
    Date getPublicationDate();
    
    /**
     * @return expiry date of page
     */
    Date getExpiryDate();

    /**
     * @return releaseDate
     */
    Date getOriginalReleaseDate();

    /**
     * @return whether the page is hidden from Site search or not.
     */
    boolean hiddenFromSiteSearch();

    /**
     * @return robots meta tag value to show/hide page from search engine.
     */
    String getMetaRobots();

    /**
     * @return content type of the page
     */
    String getContentType();

    /**
     * @return country of the page
     */
    String getCountry();

    /**
     * @return language of the page
     */
    String getLanguage();

    /**
     * @return tags for the page
     */
    String[] getTags();

    /**
     * @return content ID of the page
     */
    String getContentID();

    /**
     * @return audience of the page for permission modal
     */
    String getAudience();

    /**
     * @return private group of the page for permission modal
     */
    String[] getPrivateGroup();

    /**
     * @return access level of the page for permission modal
     */
    String getAccessLevel();

    /**
     * @return license of the page for permission modal
     */
    String getLicense();

    /**
     * @return standard setter of the page
     */
    String getStandardSetter();

    /**
     * @return created date of the page
     */
    Date getCreatedDate();

    /**
     * @return ishiddenfromsearch
     */
    boolean isHiddenFromSiteSearch();

    /**
     * @return canonical url in string format
     */

    public  String getCanonicalUrl();


    /**
     * @return formatted Tags
     */
    public String getFormattedTags();

    /**
     * @return formatted publication Date in string format
     */
    String getFormattedPublicationDateString();

    /**
     * @return formatted expiry Date in string format
     */
    String getFormattedExpiryDateString();

    /**
     * @return formatted revised Date in string format
     */
    String getFormattedRevisedDateString();

    /**
     * @return formatted original release Date in string format
     */
    String getFormattedOriginalReleaseDateString();

    /**
     * @return PwcDocContext for searching within document
     */
    String getPwcDocContext();

    /**
     * @return {@link Boolean} returns true if page is homepage otherwise false
     */
    boolean isHomePage();

    /**
     * @return hidePublicationDate
     */
    String getHidePublicationDate();
    
    /**
     * @return disableInlineLinks
     */
    String getDisableInlineLinks();

	String getJoinedPagePath();

	String getPageId();

	String getJoinedLevel();

	boolean isJoinedPage();
    String getSortOrder();
}
