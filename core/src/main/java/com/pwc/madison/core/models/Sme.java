package com.pwc.madison.core.models;

/**
 * SME Model to get SME List Information.
 *
 */
public interface Sme {
    
    /**
     * Returns {@link SmeList} after adapting SME list resource.
     * @return {@link SmeList}
     */
    public SmeList getSmeList();
    
    /**
     * Returns the encoded email subject
     *
     * @return encodedSubjectText
     */
    String getEncodedSubjectText();

}
