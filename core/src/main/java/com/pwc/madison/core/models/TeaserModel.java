package com.pwc.madison.core.models;

/**
 * Model for intheloop Teaser Component
 */
public interface TeaserModel {
    /**
     * fetch Current Page publication date
     */
    String getPagePublicationDate();

    /**
     * fetch Current Page revision Date
     */
    String getPageRevisionDate();

    String getContentType();

    Boolean isInternal();
    String[] getPrivateGroups();
}
