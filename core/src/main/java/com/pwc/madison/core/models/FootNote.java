package com.pwc.madison.core.models;

import java.util.Map;

/**
 * @author sushkuma
 *
 */

public interface FootNote {

    /**
     * 
     * 
     * @return footnotes map with callout as key and text as value for all footnotes used in current page
     */
    public Map<String, String> getFootNoteMap();

}
