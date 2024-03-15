package com.pwc.madison.core.services;

import java.util.List;
import java.util.Map;

import com.pwc.madison.core.models.CitationPattern;
import com.pwc.madison.core.models.CitationPatternResultModel;
import com.pwc.madison.core.models.CitationTextFileRefrenceModel;

/*
 * Service that will find anchor ID from DITA topic metadata
 */
public interface CitationTextToLinkMapperService {

	public Map<String, String> CollectCitationTextToLinkMap(Map<String, CitationTextFileRefrenceModel> citationMap,
			String searchScope, CitationPattern citationPattern,
			List<CitationPatternResultModel> citationPatternResults);

}
