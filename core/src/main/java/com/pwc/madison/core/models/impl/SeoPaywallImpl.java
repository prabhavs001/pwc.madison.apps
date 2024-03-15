package com.pwc.madison.core.models.impl;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;

import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.authorization.enums.AccessLevel;
import com.pwc.madison.core.authorization.models.ContentAuthorization;
import com.pwc.madison.core.models.SeoPaywall;
import com.pwc.madison.core.services.MadisonDomainsService;
import com.pwc.madison.core.util.MadisonUtil;

@Model(adaptables = SlingHttpServletRequest.class, adapters = SeoPaywall.class)
public class SeoPaywallImpl implements SeoPaywall {

	@ScriptVariable
	private Page currentPage;
	
	@OSGiService
    private MadisonDomainsService domainService;

	private Boolean isContentAccessible;
	
	private String pageUrl;

	/**
	 * Init Method of Model.
	 */
	@PostConstruct
	protected void init() {
		final ContentAuthorization contentAuthorization = MadisonUtil.getPageContentAuthorization(currentPage);
		String accessLevel = contentAuthorization.getAccessLevel();
		if(accessLevel != null) {
			isContentAccessible = !accessLevel.equals(AccessLevel.PREMIUM.getValue()) && !accessLevel.equals(AccessLevel.LICENSED.getValue());
			pageUrl = domainService.getPublishedPageUrl(currentPage.getPath(), true);
		}	
	}

	@Override
	public Boolean getContentAccessible() {
		return isContentAccessible;
	}

	@Override
	public String getPublishedPageUrl() {
		return pageUrl;
	}
}