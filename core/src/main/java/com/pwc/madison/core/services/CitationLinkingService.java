package com.pwc.madison.core.services;

import java.util.List;
import java.util.Map;

import com.pwc.madison.core.models.CitationPattern;
import com.pwc.madison.core.models.CitationPatternResultModel;
import com.pwc.madison.core.models.CitationTextFileRefrenceModel;

/**
 * Service that will replace <autolink> elements to its corresponding <pwc-xref>
 * element
 */
public interface CitationLinkingService {

	public void LinkCitationText(Map<String, CitationTextFileRefrenceModel> citationMap,
			Map<String, String> citationTextLinkMap, CitationPattern citationPattern,
			List<CitationPatternResultModel> citationPatternResults);

}
