package com.pwc.madison.core.services.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.xss.XSSAPI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import com.google.gson.Gson;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.RecentlyViewedItem;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.services.MadisonDomainsService;
import com.pwc.madison.core.services.RecentlyViewedService;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.userreg.models.request.RecentViewRequest;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.SecurityUtils;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.HomePageUtil;
import com.pwc.madison.core.util.MadisonUtil;
import com.sun.jersey.api.client.ClientResponse;

/**
 * Recently Viewed Service Implementation containing methods add, get & remove 
 * which provides page links from Spring Boot Services
 *
 */
@Component(
		immediate = true,
		service = { RecentlyViewedService.class },		
		property = { Constants.SERVICE_DESCRIPTION + "= Recently Viewed Service Implementation" })
public class RecentlyViewedServiceImpl implements RecentlyViewedService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RecentlyViewedServiceImpl.class);

	private static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss";
	private static final String ITEM_VIEWED_DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss";
	private static final String PWC_CONTENT_TYPE_VAL = "ContentType_News_FW";
	private static final String STATUS = "status";
	private static final String ONE = "1";
	private static final String INSERT_TO_TEMP_TABLE_SUBJECT = "InsertRecentlyViewedItemInTempTable";
	private static final String DELETE_RECENTLY_VIEWED_ITEMS_SUBJECT = "RemoveRecentlyViewedItemFromTempTable";

	private String domain = StringUtils.EMPTY;

	@Reference
    private transient MadisonDomainsService madisonDomainsService;

	@Reference
	private transient UserRegRestService userRegRestService;

	@Reference
	private transient ResourceResolverFactory resourceResolverFactory;

	@Reference
	private transient CountryTerritoryMapperService countryTerritoryMapperService;

	@Reference
	private transient CryptoSupport cryptoSupport;
	
	@Reference
	private XSSAPI xssAPI;
	
	private SimpleDateFormat formatter = null;
	
	@Activate
	protected void activate() {
		LOGGER.debug("RecentlyViewedServiceImpl Activated!");
	}

	@Override
	public boolean addRecentlyViewedItem(final UserProfile user, final String itemPath, final String token) {
		boolean responseFlag = false;
		formatter = new SimpleDateFormat(DATE_FORMAT);
		final RecentViewRequest recentViewRequest = new RecentViewRequest();
		try {
			if(StringUtils.isNotBlank(token) && StringUtils.isNotBlank(itemPath)) {				
				Long id = (long) user.getId();
				recentViewRequest.setUserId(id);
				recentViewRequest.setItemPath(itemPath);
				recentViewRequest.setTerritory(MadisonUtil.getTerritoryCodeFromPagePath(itemPath));
				recentViewRequest.setLocale(MadisonUtil.getLanguageCodeForPath(itemPath));
				recentViewRequest.setItemViewedDate(formatter.format(new Date().getTime()));
				final ClientResponse clientResponse = userRegRestService.addRecentlyViewed(recentViewRequest, token);
				String responseEntity = clientResponse.getEntity(String.class);
				LOGGER.debug("Response : addRecentlyViewedItem() : " + responseEntity);
				if (clientResponse.getStatus() == SlingHttpServletResponse.SC_OK) {
					responseFlag = true;
				}
			} else {
				LOGGER.debug("Token: {} or Content Path might be null: {}", xssAPI.encodeForHTML(token), xssAPI.encodeForHTML(itemPath));
			}
		} catch (final Exception e) {
			LOGGER.error("Error in addRecentlyViewedItem() method of RecentlyViewedServiceImpl: {} ", e);
		}
		return responseFlag;
	}

	@Override
	public List<RecentlyViewedItem> getRecentlyViewedItem(final SlingHttpServletRequest request, final UserProfile user, final String token, ResourceResolver resourceResolverForRecentlyViewed) {
		List<RecentlyViewedItem> recentItems = new ArrayList<>();
		List<RecentViewRequest> responseList = new ArrayList<>();
		Gson gson = new Gson();
		try {
			if (StringUtils.isNotBlank(token)) {
				final ClientResponse clientResponse = userRegRestService.getRecentlyViewed(token);
				final String responseEntity = clientResponse.getEntity(String.class);
				LOGGER.debug("Response : getRecentlyViewiedItem() : " + responseEntity);
				if(clientResponse.getStatus() == SlingHttpServletResponse.SC_OK) {
					String pagePath = request.getRequestURI();
					String dateFormat = MadisonConstants.HOMEPAGE_COMPONENTS_DATE_FORMAT;
					Resource requestPageResource = request.getResourceResolver().resolve(pagePath);
					pagePath = requestPageResource != null ? requestPageResource.getPath() : pagePath;
					String requestPageTerritoryCode = MadisonUtil.getTerritoryCodeForPath(pagePath);
					if(null != requestPageTerritoryCode)
						dateFormat = MadisonUtil.fetchDateFormat(requestPageTerritoryCode, countryTerritoryMapperService, MadisonConstants.HOMEPAGE_COMPONENTS_DATE_FORMAT);
					JSONObject json = new JSONObject(responseEntity);
					JSONArray data = json.has(MadisonConstants.DATA_OBJ) ? json.getJSONArray(MadisonConstants.DATA_OBJ) : null;
					if(data != null) {
						for(int index = 0; index<data.length(); index++) {
							String eachItem = data.getJSONObject(index).toString();
							RecentViewRequest responseObj = gson.fromJson(eachItem, RecentViewRequest.class);
							responseList.add(responseObj);
						}
						if(!responseList.isEmpty()) {
							for(RecentViewRequest recentItem : responseList) {
								Resource pageRes = resourceResolverForRecentlyViewed.getResource(recentItem.getItemPath());								
								if(pageRes != null && pageRes instanceof Resource) {
									Page page = pageRes.adaptTo(Page.class);
									if(page != null && page instanceof Page) {
										ValueMap properties = page.getProperties();
										final boolean hidePublicationDate = DITAUtils.isHidePublicationDate(page.getContentResource()).equals(MadisonConstants.YES) ? true : false;
										RecentlyViewedItem eachItem = new RecentlyViewedItem();
										eachItem.setPagePath(MadisonUtil.getUrlForPageResource(page.getPath()));
										eachItem.setArticlePagePath(page.getPath());
										HomePageUtil.setAccessType(resourceResolverForRecentlyViewed, eachItem);
										
										eachItem.setContentTitle(properties.get(JcrConstants.JCR_TITLE, String.class));
										eachItem.setContentId(properties.get(DITAConstants.META_CONTENT_ID, String.class));
										eachItem.setHidePublicationDate(hidePublicationDate);
										if (!eachItem.isHidePublicationDate()) {
											final String publicationDate = properties.get(DITAConstants.META_PUBLICATION_DATE, String.class);
											if (publicationDate != null) {
												eachItem.setPublicationDate(DITAUtils.formatDate(publicationDate, dateFormat));
											}
										}
										if(properties.get(DITAConstants.META_CONTENT_TYPE, StringUtils.EMPTY).equalsIgnoreCase(PWC_CONTENT_TYPE_VAL)) {
											eachItem = populateMetaProperties(pageRes, eachItem, resourceResolverForRecentlyViewed);
										}
										Date parsedDate = new SimpleDateFormat(ITEM_VIEWED_DATE_FORMAT).parse(recentItem.getItemViewedDate());
										String itemViewedDate = new SimpleDateFormat(MadisonConstants.HOMEPAGE_COMPONENTS_DATE_FORMAT).format(parsedDate);
										eachItem.setItemViewedDate(itemViewedDate);
										recentItems.add(eachItem);
									}
								}
							}
						}
					}
				} else {
					LOGGER.debug(
							"RecentlyViewedService getRecentlyViewedItem() : Getting error status {} with response {}",
							clientResponse.getStatus(), responseEntity);
				}
			}
		} catch (final Exception e) {
			LOGGER.error("Error in getRecentlyViewedItem() method of RecentlyViewedService: {} ", e);
		} 
		return recentItems;
	}
	
	/**
	 * This method is used to insert recently published items into a temp table in database
	 * The method uses custom authorization and generates a JWT-Token using a private key and
	 * token gets decrypted on the ums-services application
	 * 
	 */	
	@Override
	public Boolean insertRecentlyViewedItemInTempTable(final List<String> contentPathList) {
		final RecentViewRequest recentViewRequest = new RecentViewRequest();
		Boolean removeStatus = false;
		try {
            String token = SecurityUtils.encrypt(INSERT_TO_TEMP_TABLE_SUBJECT, userRegRestService.getfdKey());
			if(contentPathList.size()!= 0 && contentPathList!= null) {
				recentViewRequest.setListOfPaths(contentPathList);
				final ClientResponse clientResponse= userRegRestService.insertRecentlyViewedItemInTempTable(recentViewRequest,token);
				final String responseEntity = clientResponse.getEntity(String.class);
				if(clientResponse.getStatus() == SlingHttpServletResponse.SC_OK) {
					removeStatus = true;
				} else {
					LOGGER.debug(
							"RecentlyViewedService removeRecentlyViewedItem() : Getting error status {} with response {}",
							clientResponse.getStatus(), responseEntity);
				}
			}
		} catch(Exception e) {
			LOGGER.error("Error in removeRecentlyViewedItem() method of RecentlyViewedService: {} ", e);
		}
		return removeStatus;
	}
	
	/**
	 * This method is used to remove recently published items from database
	 * The method uses custom authorization and generates a JWT-Token using a private key and
	 * token gets decrypted on the ums-services application
	 * 
	 */	
	@Override
	public Boolean removeRecentlyViewedItemFromTempTable() {
		Boolean removeStatus = false;
		try {
			final RecentViewRequest recentViewRequest = new RecentViewRequest();
			String jwtToken = SecurityUtils.encrypt(DELETE_RECENTLY_VIEWED_ITEMS_SUBJECT, userRegRestService.getfdKey());
			final ClientResponse clientResponse= userRegRestService.removeRecentlyViewedItemFromTempTable(recentViewRequest,jwtToken);
			final String responseEntity = clientResponse.getEntity(String.class);
			if(clientResponse.getStatus() == SlingHttpServletResponse.SC_OK) {
				JSONObject json = new JSONObject(responseEntity).getJSONObject(MadisonConstants.DATA_OBJ);
				if(json.has(STATUS)) {
					if(json.getString(STATUS).equalsIgnoreCase(ONE)) removeStatus = true;
					LOGGER.debug("Items Removed From Temp table and production table");
				}
			}
		} catch(Exception e) {
			LOGGER.error("Error in removeRecentlyViewedItemFromTempTable() method of RecentlyViewedService: {} ", e);
		}
		return removeStatus;
	}

	/**
	 * This method is used to populate News Source and link from the Node Hierarchy
	 * Structure.
	 * 
	 * @param resource
	 * 			{@link Resource}
	 * 
	 * @param recentItem
	 * 			{@link RecentlyViewedItem}
	 * 
	 * @return
	 * 		{@link RecentlyViewedItem}
	 */
	private RecentlyViewedItem populateMetaProperties(final Resource resource, final RecentlyViewedItem recentItem, ResourceResolver resourceResolver) {
        domain = madisonDomainsService.getDefaultDomain();
		try {
			String sourceLink = StringUtils.EMPTY;
			String newsSource = StringUtils.EMPTY;
			resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory, MadisonConstants.MADISON_GENERIC_SUB_SERVICE);

			final Resource pRes = resourceResolver.getResource(resource.getPath() + MadisonConstants.PWC_P_PATH);
			if (Objects.nonNull(pRes)) {
				final ValueMap pValueMap = pRes.getValueMap();
				if (pValueMap.containsKey(MadisonConstants.FLATTENED_PROPERTY)) {
					final String[] element = MadisonUtil.fetchElementsFromHTML(pValueMap, MadisonConstants.FLATTENED_RECENTLY_VIEWED);
					sourceLink = element[0];
					newsSource = element[1];
				} else {
					final Resource xrefRes = resourceResolver.getResource(resource.getPath() + MadisonConstants.PWC_XREF_PATH);
					if (Objects.nonNull(xrefRes)) {
						final ValueMap xrefProps = xrefRes.getValueMap();
						if (xrefProps.containsKey(MadisonConstants.FLATTENED_PROPERTY)) {
							final String[] element = MadisonUtil.fetchElementsFromHTML(xrefProps, MadisonConstants.FLATTENED_RECENTLY_VIEWED);
							sourceLink = element[0];
							newsSource = element[1];
						} else {
							sourceLink = xrefProps.get(MadisonConstants.PWC_XREF_LINK_POPERTY, String.class);
							final Resource linkTextRes = resourceResolver.getResource(resource.getPath() + MadisonConstants.PWC_XREF_INFO_TEXT_PATH);
							if (Objects.nonNull(linkTextRes)) {
								final ValueMap linkTextValueMap = linkTextRes.getValueMap();
								if (linkTextValueMap.containsKey(MadisonConstants.PWC_XREF_INFO_TEXT_PROPERTY))
									newsSource = linkTextValueMap.get(MadisonConstants.PWC_XREF_INFO_TEXT_PROPERTY, String.class);
							}
						}
					}
				}
			}
			recentItem.setNewsSource(newsSource);
			recentItem.setSourceLink(sourceLink);
			recentItem.setIsInternalUrl(HomePageUtil.isInternalUrl(sourceLink, domain));
		} catch (final Exception e) {
			LOGGER.error("RecentlyViewedService :: populateMetaProperties () :: Error :: {} ", e);
		}
		return recentItem;
	}
}
