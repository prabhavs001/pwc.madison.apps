package com.pwc.madison.core.services.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.NameConstants;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.models.CitationPattern;
import com.pwc.madison.core.models.CitationPatternResultModel;
import com.pwc.madison.core.models.CitationTextFileRefrenceModel;
import com.pwc.madison.core.services.CitationTextToLinkMapperService;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Service implementation that will find anchor ID from DITA topic metadata
 */
@Component(service = { CitationTextToLinkMapperService.class }, immediate = true)
public class CitationTextToLinkMapperServiceImpl implements CitationTextToLinkMapperService {

	@Reference
	private ResourceResolverFactory resolverFactory;
	
	@Reference
	private XSSAPI xssAPI;

    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
		
	private static final String ANCHOR_HASH = "#";

	@Override
	public Map<String, String> CollectCitationTextToLinkMap(
			final Map<String, CitationTextFileRefrenceModel> citationMap, final String searchScope,
			final CitationPattern citationPattern, List<CitationPatternResultModel> citationPatternResults) {

		if (null == citationMap || null == citationPattern || citationMap.isEmpty() || StringUtils.isBlank(searchScope)) {
			return null;
		}

		ResourceResolver resolver = null;
		final Map<String, String> citationLinkMap = new HashMap<>();
		try {

			resolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());

			if (null == resolver) {
				LOGGER.error(" ResourceResolver null in CollectCitationTextToLinkMap for user {}",
						madisonSystemUserNameProviderService.getFmditaServiceUsername());
				return null;
			}

			for (final Map.Entry<String, CitationTextFileRefrenceModel> entry : citationMap.entrySet()) {
				final String citID = entry.getValue().getCitationId();
				final String query = "SELECT * FROM [dam:Asset] AS s WHERE ISDESCENDANTNODE([" + searchScope
						+ "]) and CONTAINS(s.[jcr:content/fmditaIds], '" + citID + "')";

				LOGGER.debug("Finding Citation id for text {} via query {}", xssAPI.encodeForHTML(citID), xssAPI.encodeForHTML(query));

				final Iterator<Resource> results = resolver.findResources(query, javax.jcr.query.Query.JCR_SQL2);
				if (!results.hasNext()) {
					LOGGER.debug("Result is null");
					for (final String filePath : entry.getValue().getFilePaths()) {
						CitationPatternResultModel citationPatternResultModel = new CitationPatternResultModel();
						citationPatternResultModel.setPatternName(citationPattern.getName());
						citationPatternResultModel.setSourcePath(filePath);
						citationPatternResultModel.setStatus(DITAConstants.FAILURE_STATUS);
						citationPatternResultModel.setFailureReason(String.format(DITAConstants.FAILURE_ANCHOR_ID_NOT_FOUND, citID, entry.getValue().getCitationText()));
						citationPatternResults.add(citationPatternResultModel);
					}
				} else {

					while (results.hasNext()) {
						final Node assestNode = results.next().adaptTo(Node.class);
						final Node jcrAssestNode = assestNode.getNode(NameConstants.NN_CONTENT);
						final String fmditaIDS = jcrAssestNode.getProperty(DITAConstants.FMDITA_IDS).getString();
						List<String> fmIDs = new ArrayList<>();
						if (fmditaIDS.contains(DITAConstants.USER_SEPARATOR)) {
							fmIDs = Arrays.asList(fmditaIDS.split(DITAConstants.USER_SEPARATOR));
						} else {
							fmIDs.add(fmditaIDS);
						}

						final String topicId = fmIDs.get(0);
						for (final String fmID : fmIDs) {
							if (citID.equalsIgnoreCase(fmID)) {

								final String linkPath = assestNode.getPath() + ANCHOR_HASH + topicId
										+ DITAConstants.FORWARD_SLASH + fmID;
								LOGGER.debug("LinkPath: {}", linkPath);
								if (citationLinkMap.containsKey(entry.getKey())) {
									LOGGER.debug("Multiple Asset with same anchor id is present assetpath {}",
											assestNode.getPath());
									break;
								} else {
									citationLinkMap.put(entry.getKey(), linkPath);
									break;
								}
							}
						}
					}
					if (citationLinkMap != null && citationLinkMap.isEmpty()) {
						for (final String filePath : entry.getValue().getFilePaths()) {
							CitationPatternResultModel citationPatternResultModel = new CitationPatternResultModel();
							citationPatternResultModel.setPatternName(citationPattern.getName());
							citationPatternResultModel.setSourcePath(filePath);
							citationPatternResultModel.setStatus(DITAConstants.FAILURE_STATUS);
							citationPatternResultModel.setFailureReason(String.format(DITAConstants.FAILURE_ANCHOR_ID_NOT_FOUND, citID, entry.getValue().getCitationText()));
							citationPatternResults.add(citationPatternResultModel);
						}
					}
				}
			}
		} catch (final RepositoryException e) {
			LOGGER.error("RepositoryException occured in CollectCitationTextToLinkMap {}", e);
			return null;
		} finally {
			if (null != resolver && resolver.isLive()) {
				resolver.close();
			}
		}

		return citationLinkMap;
	}

}
