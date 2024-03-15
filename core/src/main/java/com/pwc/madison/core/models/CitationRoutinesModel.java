package com.pwc.madison.core.models;

import java.util.Map;

/**
 * Sling model that lists the Citation Routines (Groups)
 */
public interface CitationRoutinesModel {
	
	/**
     * Returns the Citation Routines.
     *
     * @return Map.
     *
     */
	Map<String, String> getCitationRoutines();
	
	/**
     * Returns boolean value to disable autolink button
     *
     * @return Boolean
     *
     */
	Boolean getDisableAutolinkButton();

}
