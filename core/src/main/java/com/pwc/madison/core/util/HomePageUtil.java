package com.pwc.madison.core.util;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.authorization.enums.AccessLevel;
import com.pwc.madison.core.authorization.models.ContentAuthorization;
import com.pwc.madison.core.models.FeaturedContentItem;
import com.pwc.madison.core.models.HeroTileItem;
import com.pwc.madison.core.models.Item;
import com.pwc.madison.core.models.MostPopularItem;

public class HomePageUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(HomePageUtil.class);

	private static final String HTTP = "http";
	private static final String HTML = ".html";
	private static final String DITA = ".dita";
	private static final String HTTPS = "https";
	private static final String MOST_POPULAR_ITEM = "MostPopularItem";
	private static final String HERO_TILE_ITEM = "HeroTileItem";
	private static final String FEATURED_CONTENT_ITEM = "FeaturedContentItem";
	private static final String LICENSE_SEPRATOR=", ";

	/**
	 * This method is used to return an Instance of any Item class
	 * 
	 * @param type type of the Item Class for which you want to get the Object
	 * 
	 * @return {@link Object}
	 */
	public static <T> Object getInstance(Class<T> type) {
		Object obj = null;		
		if(type.getSimpleName().equalsIgnoreCase(MOST_POPULAR_ITEM)) {
			return new MostPopularItem();
		}else if(type.getSimpleName().equalsIgnoreCase(HERO_TILE_ITEM)) {
			return new HeroTileItem();
		}else if(type.getSimpleName().equalsIgnoreCase(FEATURED_CONTENT_ITEM)) {
			return new FeaturedContentItem();
		}else {
			return obj;
		}	
	}

	/**
	 * This method is used to check whether the given source link is internal or external
	 * based on the domain.
	 * 
	 * @param url
	 * 			{@link String}
	 * 
	 * @param domain
	 * 			{@link String}
	 * 
	 * @return 
	 * 			{@link Boolean}
	 */
	public static Boolean isInternalUrl(final String url, final String domain) {
		Boolean isInternalUrl = false;
		try {
			if(StringUtils.isNotBlank(url)) {
				if(url.contains(domain) || !url.startsWith(HTTP)) {
					isInternalUrl = true;
				}
			}
		} catch(Exception e ) {
			LOGGER.error("Error in isInternalUrl() : of HomePageUtil {} ", e);
		}
		return isInternalUrl;
	}

	/**
	 * This Method is used to check if it is having valid html in the end and 
	 * Appends it if not there 
	 * 
	 * @param url
	 * 			{@link String}
	 * 
	 * @return 
	 * 			{@link String}
	 */
	public static String getValidLink(final String url) {
		String link= url;
		try {
			if(StringUtils.isNotBlank(url)) {
				if((!url.startsWith(HTTP) || !url.startsWith(HTTPS))&& !url.contains(HTML) && !url.contains(DITA)) {
					link = link.concat(HTML);
				}
			}
		}catch(Exception e) {
			LOGGER.error("Error in linkChecker() : of HomepageUtil {} ",e);
		}
		return link;
	}

	public static void closeResourceResolver(Session session, ResourceResolver resourceResolver) {
		if(session != null)
			session.logout();
		if(resourceResolver.isLive())
			resourceResolver.close();
	}

	
	public static void setAccessType(ResourceResolver resourceResolver, Item item) {

		Resource requestPageResource = resourceResolver.resolve(item.getArticlePagePath());

		if (requestPageResource != null) {
			Page page = requestPageResource.adaptTo(Page.class);
			if (Objects.nonNull(page)) {
				final ContentAuthorization contentAuthorization = MadisonUtil.getPageContentAuthorization(page);

				item.setAccessType(contentAuthorization.getAccessLevel());

				if (AccessLevel.LICENSED.getValue().equals(contentAuthorization.getAccessLevel())) {
					StringBuilder liceses = new StringBuilder();
					final String[] contentLicenses = contentAuthorization.getLicenses();
					List<String> contentLicensesList = Arrays.asList(contentLicenses);
					for (final String license : contentLicensesList) {
						liceses.append(license + LICENSE_SEPRATOR);
					}
					item.setLicenseTypes(StringUtils.substringBeforeLast(liceses.toString(), LICENSE_SEPRATOR));
				}
			}
		}
	}
}
