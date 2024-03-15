
package com.pwc.madison.core.models;

import java.util.Date;

import javax.annotation.PostConstruct;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.day.cq.commons.jcr.JcrConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Featured Content Component composite multifield which represents the tile.
 */
@Model(adaptables = { Resource.class }, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class FeaturedContentTileContent {

	@ValueMapValue
	private String contentType;

	@ValueMapValue
	private String contentURL;

	@ValueMapValue
	private String openInNewWindow;

	@Self
	private Resource resource;

	private ValueMap contentPageValueMap;

	private String territoryCode;
	
	private String formattedDate;

	@PostConstruct
	protected void init() {

		ResourceResolver resourceResolver;
		Resource tileContentResource;
		Date date;

		resourceResolver = resource.getResourceResolver();
		tileContentResource = resourceResolver.getResource(contentURL + MadisonConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
		if(tileContentResource != null) {
		 contentPageValueMap = tileContentResource.adaptTo(ValueMap.class);
		 date = contentPageValueMap.get(MadisonConstants.PWC_REVISED_DATE, Date.class) != null ? contentPageValueMap.get(MadisonConstants.PWC_REVISED_DATE, Date.class) : contentPageValueMap.get(MadisonConstants.PWC_PUBLICATION_DATE, Date.class);
		 formattedDate = MadisonUtil.getDate(date, MadisonConstants.COMPONENTS_DATE_FORMAT);
		 territoryCode = MadisonUtil.getTerritoryCodeFromPagePath(contentURL).toUpperCase();
		}
		contentURL += MadisonUtil.isLinkInternal(contentURL) ? MadisonConstants.HTML_EXTN : "";
	}

	public String getContentType() {
		return contentType;
	}

	public String getContentURL() {
		return contentURL;
	}

	public String getOpenInNewWindow() {
		return openInNewWindow;
	}

	public String getTerritoryCode() {
		return territoryCode;
	}
	
	public String formattedDate() {
		return formattedDate;
	}

	public ValueMap getContentPageValueMap() {
		return contentPageValueMap;
	}
}