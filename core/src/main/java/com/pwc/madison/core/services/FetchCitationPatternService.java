package com.pwc.madison.core.services;

import java.util.Map;

import com.pwc.madison.core.models.CitationPattern;
import com.pwc.madison.core.models.CitationTextFileRefrenceModel;

/**
 * Service to fetch <autolink> elements from dita
 */
public interface FetchCitationPatternService {

	public Map<String, CitationTextFileRefrenceModel> CollectCitationPattern(CitationPattern citationPattern,
			String searchPath);

}
