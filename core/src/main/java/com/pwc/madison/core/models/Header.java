/*
 * Model class for populating the authorable header component fields.
 */
package com.pwc.madison.core.models;

public interface Header {

    String LOGO_URL_PROPERTY_NAME = "logoURL";
    String SITE_NAME_PROPERTY_NAME = "siteName";
    String SEARCH_URL_PROPERTY_NAME = "searchURL";
    String SEARCH_MAX_SUGGESTION_PROPERTY_NAME = "maxsuggestion";
    String SEARCH_MIN_CHAR_SUGGESTION = "mincharsuggestion";
    String SEARCH_MAX_SEARCH_GUIDANCE = "maxsuggestedGuidance";
    String SUGGESTED_GUIDANCE_TIMEOUT = "suggestedGuidanceTimeout";
    String NOTIFICATION_MESSAGE = "notificationMessage";
    String SEARCH_MAX_SEARCH_GUIDANCE_SLIDES = "maxsuggestedGuidanceSlides";

    /***
     * @return logo url.
     */
    public String getLogoURL();

    /***
     * @return sitename text.
     */
    public String getSiteName();

    /***
     * @return search url.
     */
    public String getSearchURL();

    /***
     * @return help link.
     */
    public String getHelpLink();

    /***
     * @return query String.
     */
    public String getQueryStr();

    /***
     * @return territory code of the current page
     */
    public String getPageTerritoryCode();

    /***
     * @return locale of the current page
     */
    public String getPageLocale();

    public String getAudience();

    public String getCc();

    public String getPage();

    /**
     * @return searchMaxSuggestions
     */
    public String getSearchMaxSuggestions();

    /**
     * @return minCharSuggestion
     */
    public String getMinCharSuggestions();

    /**
     * @return maxsuggestedGuidance
     */
    public String getSearchMaxSuggestedGuidance();

    String getPwcSourceValue();

    /**
     * @return suggestedGuidanceTimeout
     */
    public String getSuggestedGuidanceTimeout();
    
    /**
     * Returns the analytics component's name.
     * 
     * @return {@link String}
     */
    public String getComponentName();
    
    /**
     * Returns notification message.
     * 
     * @return {@link String}
     */
    public String getNotificationMessage();
    
    /**
     * @return maxsuggestedGuidanceSlides
     */
    public String getSearchMaxSuggestedGuidanceSlides();
}
