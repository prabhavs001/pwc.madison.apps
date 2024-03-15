package com.pwc.madison.core.services;

import java.util.List;

import com.pwc.madison.core.models.CitationPattern;

/**
 * Service to create a citation pattern map in memory and fetch citation patterns from routine names
 */
public interface CitationMapperService {

	/**
	 * Returns citation pattern nodes base path
	 * 
	 * @return CitationPattern
	 */
	List<CitationPattern> getCitationPatternsByRoutineNames(List<String> routineNames);

	/**
	 * Returns citation pattern nodes base path
	 * 
	 * @return String
	 */
	String getCitationPatternNodesBasePath();

	/**
	 * Returns max number of dita topics allowed
	 * 
	 * @return String
	 */
	String getMaxNumberOfDitaTopics();

}
