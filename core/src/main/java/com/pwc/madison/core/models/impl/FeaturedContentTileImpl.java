package com.pwc.madison.core.models.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ExporterConstants;
import com.pwc.madison.core.models.FeaturedContentTile;
import com.pwc.madison.core.models.FeaturedContentTileContent;

@Model(adaptables = { SlingHttpServletRequest.class }, adapters = { FeaturedContentTile.class }, resourceType = {
		FeaturedContentTileImpl.RESOURCE_TYPE }, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class FeaturedContentTileImpl implements FeaturedContentTile {

	protected static final String RESOURCE_TYPE = "pwc-madison/components/content/featured-content-tile";

	private static final Logger LOGGER = LoggerFactory.getLogger(FeaturedContentTileImpl.class);

	@ValueMapValue
	@Default(values = StringUtils.EMPTY)
	private String title;

	@ChildResource(injectionStrategy = InjectionStrategy.OPTIONAL)
	private Resource configureTileContent;

	private List<FeaturedContentTileContent> tileContents = new ArrayList<>();

	@PostConstruct
	protected void init() {
		if (configureTileContent != null) {
			for (Resource value : configureTileContent.getChildren()) {
				FeaturedContentTileContent featuredContentTileContent = value.adaptTo(FeaturedContentTileContent.class);
				tileContents.add(featuredContentTileContent);
			}
		} else {
			LOGGER.debug("Generate configureTileContent null");
		}
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public List<FeaturedContentTileContent> getTileContent() {
		return tileContents;
	}
}