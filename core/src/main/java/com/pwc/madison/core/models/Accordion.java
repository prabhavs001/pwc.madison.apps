package com.pwc.madison.core.models;

/**
 * 
 * Model representing accordion container.
 *
 */
public interface Accordion {

    /**
     * Returns false if all related link sections and sme section links are not present otherwise true.
     * 
     * @return {@link Boolean}
     */
    public boolean getShow();

}
