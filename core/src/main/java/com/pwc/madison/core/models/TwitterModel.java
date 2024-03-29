package com.pwc.madison.core.models;

public interface TwitterModel {

    /**
     * Return the title of the page
     *
     * @return pageTitle
     */
    public String getTitle();

    /**
     * Return the description of the page
     *
     * @return shortDesc
     */
    public String getPageUrl();

    /**
     * Return share Feature enabled on HomePage
     * 
     * @return true/false
     */
    public boolean isTwitterShareEnabled();

}
