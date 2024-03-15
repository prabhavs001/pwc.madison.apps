package com.pwc.madison.core.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.models.CitationPattern;
import com.pwc.madison.core.models.CitationPatternResultModel;
import com.pwc.madison.core.models.CitationTextFileRefrenceModel;
import com.pwc.madison.core.services.CitationLinkingService;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.SyndicationUtil;
import com.pwc.madison.core.util.XMLFormatter;

/**
 * Service implementation that will replace <autolink> elements to its
 * corresponding <pwc-xref> element
 */
@Component(service = { CitationLinkingService.class }, immediate = true)
public class CitationLinkingServiceImpl implements CitationLinkingService {

	@Reference
	private ResourceResolverFactory resolverFactory;
	
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;
	
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	private static final String CITTEXTPATTERN = "<autolink>%s</autolink>";
	private static final String LINKTEXTPATTERN = "<pwc-xref href=\"%s\" scope=\"peer\" format=\"dita\">%s</pwc-xref>";
	private static final String DAM_ASSETSTATE = "dam:assetState";
	private static final String DAM_ASSETSTATE_PROCESSED = "processed";
	private static final String JCR_CONTENT = "jcr:content";
	private static final String FORWARD_SLASH = "/";

	@Override
	public void LinkCitationText(final Map<String, CitationTextFileRefrenceModel> citationPatternMap,
			final Map<String, String> citationLinkMap, final CitationPattern citationPattern,
			List<CitationPatternResultModel> citationPatternResults) {

		ResourceResolver resolver = null;
		CitationPatternResultModel citationPatternResultModel;

		try {
			resolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());

			if (resolver != null) {
				for (final Map.Entry<String, CitationTextFileRefrenceModel> entry : citationPatternMap.entrySet()) {
					final Set<String> citationFileList = entry.getValue().getFilePaths();
					final String citationText = entry.getKey();
					final boolean isCitationLinkExist = citationLinkMap.containsKey(citationText);
					if (isCitationLinkExist) {
						for (final String filePath : citationFileList) {
							LOGGER.debug("Updating Citation Text:: {} in the Asset Path:: {}", citationText, filePath);
							final Asset asset = null != resolver.getResource(filePath)
									? resolver.getResource(filePath).adaptTo(Asset.class)
									: null;
							if(asset != null) {
								final InputStream inputStream = asset.getRendition(DamConstants.ORIGINAL_FILE).getStream();
								final StringWriter writer = new StringWriter();
								IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
								final String originalAssetContent = writer.toString();
								final String linkPath = SyndicationUtil
										.getRelativePath(citationLinkMap.get(citationText), filePath);
								LOGGER.debug("Link Path: {}", linkPath);
								String modifiedAssetContent = originalAssetContent.replace(String.format(CITTEXTPATTERN, citationText),
										String.format(LINKTEXTPATTERN, linkPath, citationText));
								citationPatternResultModel = new CitationPatternResultModel();
								if(originalAssetContent.equals(modifiedAssetContent)) {
									citationPatternResultModel.setStatus(DITAConstants.FAILURE_STATUS);
									citationPatternResultModel.setFailureReason(String.format(DITAConstants.FAILURE_DITA_REPLACE_FAILED, citationText));
								} else {
									citationPatternResultModel.setStatus(DITAConstants.SUCCESS_STATUS);
									ValueMap properties = resolver.getResource(filePath + FORWARD_SLASH + JCR_CONTENT).adaptTo(ValueMap.class);
									String asssetState = properties.get(DAM_ASSETSTATE, (String) null);
									final XMLFormatter formatter = new XMLFormatter();
									modifiedAssetContent = formatter.format(modifiedAssetContent);
									int count = 1;
									while (!asssetState.equals(DAM_ASSETSTATE_PROCESSED) && count<=5) {
										TimeUnit.MILLISECONDS.sleep(250);
										++count;
										resolver.refresh();
										properties = resolver.getResource(filePath  + FORWARD_SLASH + JCR_CONTENT).adaptTo(ValueMap.class);
										asssetState = properties.get(DAM_ASSETSTATE, (String) null);
									}
									asset.addRendition(DamConstants.ORIGINAL_FILE, IOUtils.toInputStream(modifiedAssetContent, StandardCharsets.UTF_8), asset.getMimeType());

								}
								citationPatternResultModel.setPatternName(citationPattern.getName());
								citationPatternResultModel.setSourcePath(filePath);
								citationPatternResultModel.setTargetPath(citationLinkMap.get(citationText));
								citationPatternResults.add(citationPatternResultModel);
								
							}
						}
					}
				}
				resolver.commit();
				resolver.refresh();
			}

		} catch (IOException e) {
			LOGGER.error("IOException occurred in LinkCitationText: {}", e);
		} catch (InterruptedException e) {
			LOGGER.error("InterruptedException occurred in LinkCitationText: {}", e);
		} finally {
			if (null != resolver && resolver.isLive()) {
				resolver.close();
			}
		}

	}

}
