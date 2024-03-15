package com.pwc.madison.core.models;

public interface EmailShare {

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
     * Return the encoded body text
     *
     * @return encodedBodyText
     */
    public String getEncodedBodyText();
}
