package com.pwc.madison.core.models.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.Cookie;

import com.pwc.madison.core.constants.MadisonConstants;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.pwc.madison.core.models.InsightsHomePage;
import com.pwc.madison.core.models.InsightsHomePageTile;
import com.pwc.madison.core.models.Item;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.services.InsightsHomePageService;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.userreg.utils.UserRegUtil;
import com.pwc.madison.core.util.HomePageUtil;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Sling Model for Insights Home Page Component
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = InsightsHomePage.class)
public class InsightsHomePageImpl implements InsightsHomePage {

	/** Default Logger*/
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	@Inject
	SlingHttpServletRequest request;

	@Inject
	SlingHttpServletResponse response;

	@Inject
	private CryptoSupport cryptoSupport;

	@Inject
	private UserRegRestService userRegRestService;

	@Inject
	private CountryTerritoryMapperService countryTerritoryMapperService;

	@Inject
	InsightsHomePageService insightsHomePageService;

	@Inject
	private transient UserPreferencesProviderService userPreferencesProviderService;
	
    @Inject
    private transient XSSAPI xssapi;

	private List<InsightsHomePageTile> filteredList;

	private List<InsightsHomePageTile> listOfPagePaths;

	private List<InsightsHomePageTile> fallBackItems;
	
	private String dateFormat;
	
	private static final String FIXED_LIST = "fixedList";
	private static final String ARTICLE_PAGE_PATH = "articlePagePath";
	private static final String FALLBACK_FOR_HERO_TILE = "fallbackForInsights";
	private static final String TRUE_TEXT = "true";
	private static final Integer LOWER_LIMIT = 1;
	private static final Integer UPPER_LIMIT = 5;
	private static final String IMAGE_PATH = "imagePath";

	private SimpleDateFormat formatter = new SimpleDateFormat(MadisonConstants.HOMEPAGE_COMPONENTS_DATE_FORMAT);

	/**
	 * init Method of Model.
	 */
	@PostConstruct
	protected void init() {
		try {
			String pagePath = request.getRequestURI();
			Resource requestPageResource = request.getResourceResolver().resolve(pagePath);
			pagePath = requestPageResource != null ? requestPageResource.getPath() : pagePath;
			String requestPageTerritoryCode = MadisonUtil.getTerritoryCodeForPath(pagePath);
			dateFormat = MadisonUtil.fetchDateFormat(requestPageTerritoryCode, countryTerritoryMapperService, MadisonConstants.HOMEPAGE_COMPONENTS_DATE_FORMAT);
			Resource pageResource = request.getResource();
			Resource fixedListResources = pageResource.getChild(FIXED_LIST);
			if(fixedListResources != null) {
				List<InsightsHomePageTile> insightsItems = getListOfPagePaths(fixedListResources.getChildren(), request.getResourceResolver());
				final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
				Comparator<Item> com = new ComparatorImpl();
				if (null != madisonCookie) {
					UserProfile userProfile = UserInformationUtil.getUserProfile(request, cryptoSupport, true, userRegRestService,
							countryTerritoryMapperService, response,userPreferencesProviderService, true, false, xssapi);
					filteredList = insightsHomePageService.fetchFilteredContent(request,userProfile, insightsItems, dateFormat);
					if(filteredList.size() < LOWER_LIMIT) {
						LOGGER.debug("InsightsHomePageImpl ::" + "init() ::" + "List size is less than 1 ,adding fallbacks");
						filteredList = addFallBackItems(filteredList,userProfile);
					}else {
						LOGGER.debug("InsightsHomePageImpl ::" + "init() ::" + "List size is correct, dont add fallbacks");
					}
					Collections.sort(filteredList, com);
					if(filteredList.size() > UPPER_LIMIT) {
						filteredList = filteredList.subList(0, UPPER_LIMIT);	
					}
				} else {
					UserProfile userProfile = null;
					filteredList = insightsHomePageService.fetchFilteredContent(request,userProfile, insightsItems, dateFormat);
					if(filteredList.size() < LOWER_LIMIT) {
						LOGGER.debug("InsightsHomePageImpl ::" + "init() ::" + "List size is less than 1 adding fallbacks");
						filteredList = addFallBackItems(filteredList,userProfile);
					}else {
						LOGGER.debug("InsightsHomePageImpl ::" + "init() ::" + "List size is correct, dont add fallbacks");
					}
					Collections.sort(filteredList, com);
					if(filteredList.size() > UPPER_LIMIT) {
						filteredList = filteredList.subList(0, UPPER_LIMIT);	
					}
				}
			}
		}catch(Exception err) {
			LOGGER.info("Exception in init" + err.toString());
		}
	}

	/**
	 * Method to add fallbackitems if filteredlist items are less than 1
	 * 
	 * 
	 */
	private List<InsightsHomePageTile> addFallBackItems(List<InsightsHomePageTile> filteredList,UserProfile userProfile){
		try {
			int count = LOWER_LIMIT - filteredList.size();
			LOGGER.debug("InsightsHomePageImpl" + "addFallBackItems() ::" + "These items will be from fallback ::" + count);
			LOGGER.debug("Fallback item size" + fallBackItems.size());
			for (InsightsHomePageTile fallbackItem : fallBackItems) {
				InsightsHomePageTile insightsFallBackItem = insightsHomePageService.fetchFallbackItems(fallbackItem, userProfile, dateFormat);
				if(insightsFallBackItem != null) {
					filteredList.add(insightsFallBackItem);
				}
			}
		}catch(Exception err) {
			LOGGER.error("InsightsHomePageImpl" + "addFallBackItems() :: Error ::"  + err.toString());
		}
		return filteredList;
	}

	/**
	 * Method to get the list of page paths from multifield and
	 * identifying items which are fallbacks 
	 * 
	 */
	private List<InsightsHomePageTile> getListOfPagePaths(Iterable<Resource> iterableResources, ResourceResolver resourceResolver) {
		LOGGER.debug("InsightsHomePageImpl :: getListOfPagePaths() ::" + "Getting list of page path from nodes");
		listOfPagePaths = new ArrayList<InsightsHomePageTile>();
		fallBackItems = new ArrayList<InsightsHomePageTile>();
		try{
			for (Resource childResource : iterableResources) 
			{
				ValueMap properties = childResource.getValueMap();
				InsightsHomePageTile item = new InsightsHomePageTile();
				item.setArticlePagePath(properties.get(ARTICLE_PAGE_PATH, String.class));
				if(properties.containsKey(IMAGE_PATH) && null != properties.get(IMAGE_PATH, String.class)) {
                	item.setImagePath(properties.get(IMAGE_PATH, String.class));
                }
				HomePageUtil.setAccessType(resourceResolver, item);
					String isFallBack = properties.get(FALLBACK_FOR_HERO_TILE, String.class);
					if(isFallBack != null && isFallBack.equalsIgnoreCase(TRUE_TEXT)) {
						LOGGER.debug("InsightsHomePageImpl :: getListOfPagePaths() :: Fallbacks in fixedList ::" + item.getArticlePagePath());
						fallBackItems.add(item);
					}else {
						LOGGER.debug("InsightsHomePageImpl :: getListOfPagePaths() :: Page Paths in fixed list::" + item.getArticlePagePath());
						listOfPagePaths.add(item);	
					}

			}
			return listOfPagePaths;
		}catch(Exception err) {
			LOGGER.info(err.toString());
		}
		return listOfPagePaths;
	}

	@Override
	public List<InsightsHomePageTile> getFilteredList() {
		return filteredList;
	}
	
	@Override
	public String getComponentName(){
		return MadisonConstants.INSIGHTS_COMPONENT_NAME;
	}
	/**
	 * Comparator Implementation Used for sorting insights home page 
	 * items according to number of views.
	 */
	public class ComparatorImpl implements Comparator<Item>{
		@Override
		public int compare(Item item1, Item item2) {
			try {
				return formatter.parse(item2.getUnformattedPublicationDate()).compareTo(formatter.parse(item1.getUnformattedPublicationDate()));
			}catch(Exception err) {
				LOGGER.info(err.toString());
			}
			return -1;
		}
	}

}
