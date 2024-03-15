package com.pwc.madison.core.models;

/**
 * @author sushkuma
 *
 */
public interface FootNoteDitaModel {

    /**
     * @return footnote text
     */
    public String getText();

    /**
     * @return foonote callout value
     */
    public String getCallout();

    /**
     * @return foonote id value
     */
    public String getId();
    
    /**
     * @return footnote show flag
     */
    public Boolean getShow();

}
