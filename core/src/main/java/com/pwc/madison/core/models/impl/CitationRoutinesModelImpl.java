package com.pwc.madison.core.models.impl;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.Session;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.CitationPattern;
import com.pwc.madison.core.models.CitationRoutinesModel;
import com.pwc.madison.core.services.CitationMapperService;
import com.pwc.madison.core.util.MadisonUtil;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model implementation to list Citation Routines (Groups)
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = CitationRoutinesModel.class)
public class CitationRoutinesModelImpl implements CitationRoutinesModel {

	private final Logger LOG = LoggerFactory.getLogger(this.getClass());

	@OSGiService
	ResourceResolverFactory resourceResolverFactory;

	@OSGiService
	CitationMapperService citationMapperService;
	
	@OSGiService
    private XSSAPI xssAPI;

	@Inject
	private SlingHttpServletRequest request;

	@Inject
	private QueryBuilder queryBuilder;

	private Map<String, String> citationRoutines = null;

	private Boolean disableAutolinkButton = false;

	private Session session;

	private static final String PAYLOAD_QUERY_PARAMTER = "payload";

	private static final String DITA_EXTENSION = ".dita";

	private static final String DITA_REGEX = "*.dita";

	@PostConstruct
	protected void init() {
		final ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
				MadisonConstants.SYNDICATION_SERVICE_USER);
		final String requestedPage = request.getParameter(PAYLOAD_QUERY_PARAMTER);
		LOG.debug("requestedPage: {}", xssAPI.encodeForHTML(requestedPage));
		LOG.debug("Base Node Path: {}", citationMapperService.getCitationPatternNodesBasePath());
		LOG.debug("Max Number of Dita Topics: {}", citationMapperService.getMaxNumberOfDitaTopics());
		final String P_LIMIT = citationMapperService.getMaxNumberOfDitaTopics();

		if (resourceResolver != null && requestedPage!=null && !requestedPage.endsWith(DITA_EXTENSION)) {
			session = resourceResolver.adaptTo(Session.class);
			SearchResult searchResult = null;
			if (session != null) {
				final Map<String, String> params = new HashMap<String, String>();
				params.put("path", requestedPage);
				params.put("type", com.day.cq.dam.api.DamConstants.NT_DAM_ASSET);
				params.put("nodename", DITA_REGEX);
				params.put("p.limit", P_LIMIT);
				LOG.debug("Query executed :: {}", xssAPI.encodeForHTML(params.toString()));
				if (!params.isEmpty()) {
						searchResult = queryBuilder.createQuery(PredicateGroup.create(params), session).getResult();
				}
			}
			if (null != searchResult) {
				int totalDitaPages = searchResult.getHits().size();
				LOG.debug("totalDitaPages: {}", totalDitaPages);
				if (Integer.parseInt(P_LIMIT) == totalDitaPages) {
					disableAutolinkButton = true;
				}
			} else {
				LOG.debug("Query resulted in null");
			}
		}

		LOG.debug("Disable Autolink Button: {}", disableAutolinkButton);
		citationRoutines = new HashMap<String, String>();
		if (null != resourceResolver) {
			final Resource citationBasePathResource = resourceResolver
					.getResource(citationMapperService.getCitationPatternNodesBasePath());
			if (null != citationBasePathResource) {
				for (final Resource citationResource : citationBasePathResource.getChildren()) {
					final CitationPattern citationPattern = citationResource.adaptTo(CitationPattern.class);
					citationRoutines.put(citationPattern.getRoutineName(), citationPattern.getRoutineId());
				}
			}
			resourceResolver.close();
			LOG.debug("Citation Routines: {}", citationRoutines.toString());
		}

	}

	@Override
	public Map<String, String> getCitationRoutines() {
		return citationRoutines;
	}

	@Override
	public Boolean getDisableAutolinkButton() {
		return disableAutolinkButton;
	}
}
