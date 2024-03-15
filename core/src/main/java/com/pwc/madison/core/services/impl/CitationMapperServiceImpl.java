package com.pwc.madison.core.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.CitationPattern;
import com.pwc.madison.core.services.CitationMapperService;
import com.pwc.madison.core.services.impl.CitationMapperServiceImpl.CitationPatternConfiguration;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Service implementation to create a citation pattern map in memory and fetch citation patterns from routine names
 */
@Component(service = { CitationMapperService.class, EventHandler.class }, immediate = true, property = {
		EventConstants.EVENT_TOPIC + "=org/apache/sling/api/resource/Resource/*",
		EventConstants.EVENT_FILTER + "=(path=/conf/pwc-madison/settings/citation/*)" })
@Designate(ocd = CitationPatternConfiguration.class)
public class CitationMapperServiceImpl implements CitationMapperService, EventHandler {

	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	@Reference
	ResourceResolverFactory resourceResolverFactory;

	@Reference
	private QueryBuilder queryBuilder;
	
	@Reference
    private XSSAPI xssAPI;

	private String citationBasePath;

	private String maxNumberOfDitaTopics;

	private Map<String, CitationPattern> citationPatternMap;

	private Session session;

	private static final String ROUTINE_NAME_PROPERTY = "routineName";
	private static final String NT_UNSTRUCTURED_VALUE = "nt:unstructured";
	private static final String PAGE_LIMIT = "-1";
	private static final String SYMB_AT = "@";

	@Activate
	@Modified
	protected void Activate(final CitationPatternConfiguration citationPatternConfiguration) {
		LOGGER.debug("CitationMapperService : Entered Activate/Modify");
		citationBasePath = citationPatternConfiguration.citationBasePath();
		maxNumberOfDitaTopics = citationPatternConfiguration.maxNumberOfDitaTopics();
		LOGGER.debug("CitationMapperService Activate() Madison Citation Pattern Base Path : {}", citationBasePath);
		createCitationPatternMap();
	}

	@Override
	public void handleEvent(final Event event) {
		createCitationPatternMap();
	}

	private void createCitationPatternMap() {
		final ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
				MadisonConstants.SYNDICATION_SERVICE_USER);
		if (null != resourceResolver) {
			final Resource citationBasePathResource = resourceResolver.getResource(citationBasePath);
			if (null != citationBasePathResource) {
				citationPatternMap = new HashMap<>();
				for (final Resource citationResource : citationBasePathResource.getChildren()) {
					if (citationResource != null && citationResource.hasChildren()) {
						for (final Resource childrenCitationResource : citationResource.getChildren()) {
							final CitationPattern citationPattern = childrenCitationResource.adaptTo(CitationPattern.class);
							citationPatternMap.put(citationPattern.getId(), citationPattern);
						}
					}
				}
			}
			resourceResolver.close();
		}
		LOGGER.debug("CitationMapperServiceImpl createCitationPatternMap() : Citation ID to Config Map {}", citationPatternMap);
		LOGGER.debug("Total config nodes found are: {}", citationPatternMap.size());
	}

	@Override
	public List<CitationPattern> getCitationPatternsByRoutineNames(final List<String> routineNames) {
		final ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
				MadisonConstants.SYNDICATION_SERVICE_USER);
		session = resourceResolver.adaptTo(Session.class);
		SearchResult searchResult = null;
		List<CitationPattern> citationPatternList = null;
		if (session != null && !routineNames.isEmpty()) {
			final Map<String, String> params = new HashMap<String, String>();
			params.put("path", citationBasePath);
			params.put("type", NT_UNSTRUCTURED_VALUE);
			params.put("group.p.or", MadisonConstants.TRUE_TEXT);
			int count = 0;
			for (final String routineName : routineNames) {
				if (routineName != null && !StringUtils.isBlank(routineName)) {
					int index = ++count;
					String group = "group." + index + "_group.";
					params.put(group.concat("property"), ROUTINE_NAME_PROPERTY);
					params.put(group.concat("property.value"), routineName);
				}
			}
			params.put("orderby", SYMB_AT + ROUTINE_NAME_PROPERTY);
			params.put("p.limit", PAGE_LIMIT);
			LOGGER.debug("Query executed: {}", xssAPI.encodeForHTML(params.toString()));
			if (!params.isEmpty()) {
				searchResult = queryBuilder.createQuery(PredicateGroup.create(params), session).getResult();
			}
		}
		if (null != searchResult) {
			citationPatternList = new ArrayList<CitationPattern>();
			try {
				for (final Hit hit : searchResult.getHits()) {
					final Resource resource = resourceResolver.getResource(hit.getPath());
					if(resource != null && resource.hasChildren()) {
						for (final Resource patternResource : resource.getChildren()) {
							LOGGER.debug("Paths: {}", patternResource.getPath());
							final CitationPattern citationPattern = patternResource.adaptTo(CitationPattern.class);
							if (citationPatternMap.get(citationPattern.getId()) != null)
								citationPatternList.add(citationPatternMap.get(citationPattern.getId()));
						}
					}
				}
			} catch (RepositoryException e) {
				LOGGER.error("RepositoryException occured in getCitationPatternsByRoutineIds: {}", e);
			}
		} else {
			LOGGER.debug("Query resulted in null");
		}
		LOGGER.debug("Citation Pattern List: {}", citationPatternList);
		return citationPatternList;
	}

	@ObjectClassDefinition(name = "PwC Viewpoint Citation Configuration")
	public @interface CitationPatternConfiguration {

		@AttributeDefinition(name = "Citation Pattern Config Path", description = "Content path under which Citation Pattern reference data is stored")
		String citationBasePath() default "/conf/pwc-madison/settings/citation";

		@AttributeDefinition(name = "Maximum Number of Dita Topics", description = "Maximum Number of Dita Topics on which Autolinker tool will execute")
		String maxNumberOfDitaTopics() default "500";

	}

	@Override
	public String getCitationPatternNodesBasePath() {
		return citationBasePath;
	}

	@Override
	public String getMaxNumberOfDitaTopics() {
		return maxNumberOfDitaTopics;
	}

}