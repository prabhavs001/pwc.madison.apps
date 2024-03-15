package com.pwc.madison.core.services.impl;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.models.CitationPattern;
import com.pwc.madison.core.models.CitationTextFileRefrenceModel;
import com.pwc.madison.core.services.CitationTextToIdConverterService;

/**
 * Service implementation that will convert citation text to its corresponding
 * ID
 */
@Component(service = { CitationTextToIdConverterService.class }, immediate = true)
public class CitationTextToIdConverterServiceImpl implements CitationTextToIdConverterService {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Override
	public Map<String, CitationTextFileRefrenceModel> ConvertCitationTextToId(
			final Map<String, CitationTextFileRefrenceModel> citationPatternMap,
			final CitationPattern citationPattern) {

		if (null == citationPattern || citationPatternMap.isEmpty()) {
			return null;
		}

		final String id = citationPattern.getId();
		
		String citId = null;

		for (final Map.Entry<String, CitationTextFileRefrenceModel> entry : citationPatternMap.entrySet()) {
			final String citationText = entry.getKey();
			final CitationTextFileRefrenceModel citationTextFileRefrenceModel = entry.getValue();
			if(id.endsWith("AG-CS")) {
				citId = citationText.replaceAll("([A-Z]{2,3}) ([0-9]+(.[0-9]+)*)$", "pwc-$1$2").replace(".", "_");
			}
			else if(id.endsWith("AG-AS")) {
				citId = citationText.replaceAll("([A-Z]{2,3}) ([0-9]+(.[0-9]+)*[A-Z])$", "pwc-$1$2").replace(".", "_");
				final String lastCharacterSmallCase = citId.substring(citId.length()-1, citId.length()).toLowerCase();
				final String remainingCharacters = citId.substring(0, citId.length()-1);
				citId = remainingCharacters+lastCharacterSmallCase;
			}
			else if(id.endsWith("AG-App")) {
				citId = citationText.replaceAll("([A-Z]{2,3}) Appendix ([A-Z]{1})$", "pwc-$1_App$2");
			}
			else if(id.equals("RG-AG-AppS")) {
				citId = citationText.replaceAll("RG D.([0-9]+(.[0-9]+)*)$", "pwc-RG_AppD$1").replace(".", "_");
			}
			else if(id.equals("PAG-S")) {
				citId = citationText.replace("PwC Audit ", "pwc-ag").replace(".", "_");
			}
			else if(id.equals("NPAG-S")) {
				citId = citationText.replace("PwC Audit (NP) ", "pwc-npag").replace(".", "_");
			}
			else if(id.equals("IFRS-Para-Numbers")) {
				citId = citationText.replace("IFRS 16", "ifrs 16").replace("para","pr").replaceAll("\\s+\\h*(.*$)","$1").replaceAll("\\s+", "_");
			}
			else if(id.equals("IFRS-Para-Numbers-Subsection")) {
				citId = citationText.replace("IFRS 16", "ifrs 16").replace("para","pr").replaceAll("\\s+\\h*(.*$)","$1").replaceAll("\\s+", "_");
			}
			else if(id.equals("IFRS-BC")) {
				citId = citationText.replace("IFRS 16", "ifrs 16").replace("para","pr").replaceAll("\\s+\\h*(.*$)","$1").replaceAll("\\s+", "_").toLowerCase();
			}
			else if(id.equals("IFRS-BCZ")) {
				citId = citationText.replace("IFRS 16", "ifrs 16").replace("para","pr").replaceAll("\\s+\\h*(.*$)","$1").replaceAll("\\s+", "_").toLowerCase();
			}
			else if(id.equals("IFRS-App-B")) {
				citId = citationText.replace("IFRS 16 App B", "ifrs 16").replace("para","pr").replaceAll("\\s+\\h*(.*$)","$1").replaceAll("\\s+", "_");
			}
			else if(id.equals("IFRS-App-C")) {
				citId = citationText.replace("IFRS 16 App C", "ifrs 16").replace("para","pr").replaceAll("\\s+\\h*(.*$)","$1").replaceAll("\\s+", "_");
			}
			
			log.debug("ConvertCitationTextToId Citation Text: {} and converted ID: {}", citationText, citId);
			
			if (citId != null) {
				citationTextFileRefrenceModel.setCitationId(citId);
				citationPatternMap.put(citationText, citationTextFileRefrenceModel);
			}
		}
		return citationPatternMap;
	}

}
