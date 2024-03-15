package com.pwc.madison.core.models;

/**
 * Overview Component's Model.
 * This Component would be used for displaying the title of the topic that the page is for and to provide topic's information.
 * The title color depends upon whether the content type is internal or external.
 */
public interface OverviewComponent {

    /**
     * @return Overview's title
     */
    String getOverviewTitle();

    /**
     * @return Overview's description
     */
    String getOverviewDescription();

}

