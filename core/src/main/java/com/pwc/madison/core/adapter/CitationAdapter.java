package com.pwc.madison.core.adapter;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.models.CitationPattern;

/**
 * Adapter to map the properties of a {@link Resource} to a {@link CitationPattern}.
 */
public class CitationAdapter {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CitationAdapter.class);

	public CitationPattern adaptResourceToCitationPattern(final Resource citationResource) {
		CitationPattern citationPattern = null;
		if (null != citationResource) {
			final ValueMap citationValueMap = citationResource.getValueMap();
			citationPattern = new CitationPattern();
			citationPattern.setId(
					citationValueMap.containsKey("id") ? citationValueMap.get("id", String.class) : StringUtils.EMPTY);
			citationPattern.setRoutineId(
					citationValueMap.containsKey("routineId") ? citationValueMap.get("routineId", String.class)
							: StringUtils.EMPTY);
			citationPattern.setRoutineName(
					citationValueMap.containsKey("routineName") ? citationValueMap.get("routineName", String.class)
							: StringUtils.EMPTY);
			citationPattern.setName(citationValueMap.containsKey("name") ? citationValueMap.get("name", String.class)
					: StringUtils.EMPTY);
			citationPattern.setScope(citationValueMap.containsKey("scope") ? citationValueMap.get("scope", String.class)
					: StringUtils.EMPTY);
			citationPattern
			.setRegex(citationValueMap.containsKey("regex") ? citationValueMap.get("regex", String.class)
					: StringUtils.EMPTY);

			LOGGER.debug(
					"CitationAdapter adaptResourceToCitationPattern() : Adapting resource at path {} to CitationPattern: {}",
					citationResource.getPath(), citationPattern.toString());
		}
		return citationPattern;
	}

}
