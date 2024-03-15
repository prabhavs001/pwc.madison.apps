package com.pwc.madison.core.services;

import java.util.Map;

import com.pwc.madison.core.models.CitationPattern;
import com.pwc.madison.core.models.CitationTextFileRefrenceModel;

/**
 * The interface which will convert citation text to its corresponding ID
 */
public interface CitationTextToIdConverterService {

	public Map<String, CitationTextFileRefrenceModel> ConvertCitationTextToId(
			Map<String, CitationTextFileRefrenceModel> citationPatternMap, CitationPattern citationPattern);

}
