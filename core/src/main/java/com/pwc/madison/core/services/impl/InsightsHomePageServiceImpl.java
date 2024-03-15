package com.pwc.madison.core.services.impl;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import com.google.gson.Gson;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.InsightsHomePageTile;
import com.pwc.madison.core.services.ContentFilterService;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.services.InsightsHomePageService;
import com.pwc.madison.core.services.MadisonDomainsService;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.HomePageUtil;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Insights Home Page Tile Service Implementation
 *
 */
@Component(service = {InsightsHomePageService.class}, immediate = true,
property = {
		"service.description=" + "Insights Home Page Tile Service Implementation"})
public class InsightsHomePageServiceImpl implements InsightsHomePageService {

	/** Default Logger*/
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	@Reference
	ResourceResolverFactory resourceResolverFactory;

	ResourceResolver resourceResolver;

	@Reference
	private ContentFilterService contentFilterService;

	String domain = StringUtils.EMPTY;

	@Reference
	private transient MadisonDomainsService madisonDomainsService;

	@Activate
	protected void activated() {
		LOGGER.info("InsightsHomePageServiceImpl Activated!");
	}


	/**
	 * Returns the list of Type InsightsHomePageTile 
	 *
	 * @param userProfile
	 *  		  {@link UserProfile}
	 * @param insightsItems
	 *            {@link List<InsightsHomePageTile>}
	 * @return {@link List<InsightsHomePageTile>} of given {@link Class<InsightsHomePageTile>}
	 * 
	 */
	@Override
	public List<InsightsHomePageTile> fetchFilteredContent(SlingHttpServletRequest request, UserProfile userProfile, List<InsightsHomePageTile> insightsItems, String dateFormat) {
		List<InsightsHomePageTile> insightsItemsList = null;
		try {
			Gson gson = new Gson();
			boolean isItAFallBackItem = false;
			insightsItemsList = new LinkedList<InsightsHomePageTile>();	
			resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory, 
					MadisonConstants.MADISON_READ_SUB_SERVICE);
			for(InsightsHomePageTile items : insightsItems) {
				Resource pageResource = resourceResolver.getResource(items.getArticlePagePath());
				if(pageResource != null && pageResource instanceof Resource) {
					JSONObject json = new JSONObject(items);
					InsightsHomePageTile singleItem = gson.fromJson(json.toString(), InsightsHomePageTile.class);
					singleItem.setIsFallbackItem("false");
					Page pageItem = pageResource.adaptTo(Page.class);
					ValueMap pageProperties = pageItem.getProperties();
					boolean isValidContent = true;
					LOGGER.debug("InsightsHomePageServiceImpl :: fetchFilteredContent() :: Checking if this path ::" + items.getArticlePagePath() + " is valid or not");
					if(MadisonUtil.isPublishMode(request))
					isValidContent = contentFilterService.isValidContent(pageProperties, userProfile,isItAFallBackItem);
					
					LOGGER.debug("fetchFilteredContent () :: isValidContent" + isValidContent);
					if(isValidContent) {
						Resource ghostPropertiesResource = pageResource.getChild(MadisonConstants.GHOST_MODULE_PATH);
						InsightsHomePageTile filteredItem = new InsightsHomePageTile();
			            
						filteredItem.setPath(pageResource.getPath());
						if(ghostPropertiesResource != null && ghostPropertiesResource instanceof Resource) {
							filteredItem = addFilteredItem(filteredItem, pageProperties, singleItem, ghostPropertiesResource, dateFormat);
						}else {
							ghostPropertiesResource = null;
							filteredItem = addFilteredItem(filteredItem, pageProperties, singleItem, ghostPropertiesResource, dateFormat);
						}
						insightsItemsList.add(filteredItem);
					}
				}
			}
		}catch(Exception err) {
			LOGGER.info("Error in fetchFilteredContent()" + err.toString());
		}finally {
			if(resourceResolver.isLive())
				resourceResolver.close();
		}
		return insightsItemsList;
	}



	/**
	 * Method Used for fetchingFallbackItems
	 */

	@Override
	public InsightsHomePageTile fetchFallbackItems(InsightsHomePageTile insightsTileItem, UserProfile userProfile, String dateFormat) {
		LOGGER.debug("InsightsHomePageServiceImpl :: fetchFallbackItems() :: Fetching fallbackItems");
		InsightsHomePageTile insightsItem = null;
		try {
			Gson gson = new Gson();
			resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory, 
					MadisonConstants.MADISON_READ_SUB_SERVICE);
			Resource pageResource = resourceResolver.getResource(insightsTileItem.getArticlePagePath());
			if(pageResource != null && pageResource instanceof Resource) {
				JSONObject json = new JSONObject(insightsTileItem);
				InsightsHomePageTile singleItem = gson.fromJson(json.toString(), InsightsHomePageTile.class);
				Page pageItem = pageResource.adaptTo(Page.class);
				ValueMap pageProperties = pageItem.getProperties();
				singleItem.setIsFallbackItem("true");
				InsightsHomePageTile filteredItem = new InsightsHomePageTile();
				filteredItem.setPath(pageResource.getPath());
				Resource ghostPropertiesResource = pageResource.getChild(MadisonConstants.GHOST_MODULE_PATH);
				if(ghostPropertiesResource != null && ghostPropertiesResource instanceof Resource) {
					insightsItem = addFilteredItem(filteredItem, pageProperties, singleItem, ghostPropertiesResource, dateFormat);
					LOGGER.debug("InsightsHomePageServiceImpl :: fetchFallbackItems() :: FallbackItemPath is  :: " + insightsItem.getArticlePagePath());
				}else {
					ghostPropertiesResource = null;
					insightsItem = addFilteredItem(filteredItem, pageProperties, singleItem, ghostPropertiesResource, dateFormat);
				}
			}
		}catch(Exception err) {
			LOGGER.info(err.toString());
		}finally {
			if(resourceResolver.isLive())
				resourceResolver.close();
		}
		return insightsItem;
	}

	/**
	 * Method Used for adding the item to list
	 */

	private InsightsHomePageTile addFilteredItem(InsightsHomePageTile filteredItem, ValueMap pageProperties, InsightsHomePageTile singleItem,Resource ghostPropertiesResource, String dateFormat) {
		try {
			domain = madisonDomainsService.getDefaultDomain();

			ValueMap resourceValueMap = null;
			if(ghostPropertiesResource != null) {
				LOGGER.debug("InsightsHomePageServiceImpl () :: addFilteredItem() :: Its A Ghost Item");	
				resourceValueMap = ghostPropertiesResource.getValueMap();
				filteredItem.setGhostTemplateItem(true);
			}else {
				filteredItem.setGhostTemplateItem(false);
			}
			
			Resource pageResource = resourceResolver.getResource(singleItem.getArticlePagePath());
			Page page = pageResource.adaptTo(Page.class);
			final boolean hidePublicationDate = DITAUtils.isHidePublicationDate(page.getContentResource()).equals(MadisonConstants.YES) ? true : false;
			
			String contentId = pageProperties.get(DITAConstants.META_CONTENT_ID, StringUtils.EMPTY);
			String standardSetter = pageProperties.get(DITAConstants.META_STANDARD_SETTERS, StringUtils.EMPTY);
			String contentType = pageProperties.get(DITAConstants.META_CONTENT_TYPE, StringUtils.EMPTY);
			String contentField = getContentFieldValue(contentId, standardSetter, InsightsHomePageTile.getPwcSourceValue(), contentType);
			
			String publicationDate = resourceValueMap != null ? resourceValueMap.get(MadisonConstants.GHOST_PUBLICATION_DATE, String.class) : pageProperties.get(MadisonConstants.PWC_PUBLICATION_DATE, String.class);
			String revisedDate = pageProperties.get(MadisonConstants.PWC_REVISED_DATE, String.class);
			filteredItem.setTopicText(resourceValueMap != null ? resourceValueMap.get(MadisonConstants.TOPIC_TEXT,String.class) : (pageProperties.get(MadisonConstants.PWC_PAGE_TITLE,String.class).isEmpty()? pageProperties.get(JcrConstants.JCR_TITLE,String.class):
				pageProperties.get(MadisonConstants.PWC_PAGE_TITLE,String.class)));			
			filteredItem.setTopicTitle(resourceValueMap != null ? (contentField.isEmpty() ? resourceValueMap.get(MadisonConstants.TOPIC_LABEL,String.class) : contentField) : contentField);
			filteredItem.setCountry(MadisonUtil.getTerritoryCodeFromPagePath(singleItem.getArticlePagePath()).toUpperCase());
			filteredItem.setAbstractDescription(resourceValueMap != null ? resourceValueMap.get(MadisonConstants.ABSTRACT_TEXT, String.class) : pageProperties.get(JcrConstants.JCR_DESCRIPTION,String.class));
			filteredItem.setArticlePagePath(singleItem.getArticlePagePath());
			filteredItem.setLinkUrl(resourceValueMap != null ? resourceValueMap.get(MadisonConstants.LINK_URL,String.class) : singleItem.getArticlePagePath());
            filteredItem.setPath(singleItem.getArticlePagePath());
            filteredItem.setAccessType(singleItem.getAccessType());
            filteredItem.setLicenseTypes(singleItem.getLicenseTypes());
			filteredItem.setLinkLabel(resourceValueMap != null ? resourceValueMap.get(MadisonConstants.LINK_LABEL,String.class)  : "");
			filteredItem.setIsFallbackItem(singleItem.getIsFallbackItem());
			filteredItem.setUrlInternal(HomePageUtil.isInternalUrl(filteredItem.getLinkUrl(), domain));
			filteredItem.setUnformattedPublicationDate(DITAUtils.formatDate(publicationDate, MadisonConstants.HOMEPAGE_COMPONENTS_DATE_FORMAT));
			filteredItem.setHidePublicationDate(hidePublicationDate);
			if(!filteredItem.isHidePublicationDate())
				filteredItem.setPublicationDate(DITAUtils.formatDate(publicationDate, dateFormat));
			filteredItem.setRevisedDate(DITAUtils.formatDate(revisedDate, dateFormat));
			if(null != singleItem.getImagePath()) {
				filteredItem.setImagePath(singleItem.getImagePath());
            }

		}catch(Exception err) {
			LOGGER.info("Error in addFilteredItem()" + err.toString());
		}
		return filteredItem;
	}
	
	/** 
	 * Method used to take decision for contentField Value
	 * 
	 * @return contentFieldValue {@link String}
	 * */
	public String getContentFieldValue(String contentId, String standardSetter, String PwcAuthored, String contentType) {
		String contentFieldValue = StringUtils.EMPTY;
		if(contentId.isEmpty()) {
			if(standardSetter.equalsIgnoreCase(PwcAuthored))
				contentFieldValue = contentType;
			else
				contentFieldValue = standardSetter;
		}else
			contentFieldValue = contentId;
		return contentFieldValue;
	}
}