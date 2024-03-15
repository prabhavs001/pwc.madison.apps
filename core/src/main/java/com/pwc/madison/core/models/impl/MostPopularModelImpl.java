package com.pwc.madison.core.models.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.pwc.madison.core.models.Item;
import com.pwc.madison.core.models.MostPopularItem;
import com.pwc.madison.core.models.MostPopularModel;
import com.pwc.madison.core.services.ContentFilterService;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.userreg.utils.UserRegUtil;
import com.pwc.madison.core.util.HomePageUtil;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;

/**
 * Sling Model for Most Popular Item Component
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = MostPopularModel.class)
public class MostPopularModelImpl implements MostPopularModel {

	/** Default Logger*/
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	@Inject
	SlingHttpServletRequest request;

	@Inject
	SlingHttpServletResponse response;

	@OSGiService
	private ContentFilterService contentFilterService;

	@Inject
	private CryptoSupport cryptoSupport;

	@Inject
	private UserRegRestService userRegRestService;

	@Inject
	private CountryTerritoryMapperService countryTerritoryMapperService;

	@Inject
	private transient UserPreferencesProviderService userPreferencesProviderService;
	
    @Inject
    private transient XSSAPI xssapi;

	@ChildResource(injectionStrategy = InjectionStrategy.OPTIONAL)
	private Resource fixedList;

	private List<MostPopularItem> filteredList;
	private List<MostPopularItem> listOfPagePaths;
	private List<MostPopularItem> fallBackItems;
    
    private String dateFormat;

	private static final String ARTICLE_PAGE_PATH = "articlePagePath";
	private static final String FALLBACK_FOR_MOST_POPULAR = "fallbackForMostPopular";
	private static final String TRUE_TEXT = "true";
	private static final String NO_OF_VIEWS = "noOfViews";
	private static final String OPEN_IN_NEW_WINDOW = "openInNewWindow";
	private static final Integer LIMIT = 4;
	private static final String IMAGE_PATH = "imagePath";
	
	private SimpleDateFormat dateFormatter = new SimpleDateFormat(MadisonConstants.HOMEPAGE_COMPONENTS_DATE_FORMAT);
	
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
			if (Objects.nonNull(fixedList)) {
				List<MostPopularItem> mostPopularItems = getListOfPagePaths(fixedList.getChildren(), request.getResourceResolver());
				final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
				Comparator<Item> com = new ComparatorImpl();
				if (null != madisonCookie) {
					UserProfile userProfile = UserInformationUtil.getUserProfile(request, cryptoSupport, true, userRegRestService,
							countryTerritoryMapperService, response,userPreferencesProviderService, true, false, xssapi);
					filteredList = contentFilterService.fetchFilteredContent(request, userProfile, mostPopularItems, MostPopularItem.class, dateFormat);
					if(filteredList.size() < LIMIT) {
						LOGGER.debug("MostPopularModelImpl ::" + "init() ::" + "List size is less than 4 ,adding fallbacks");
						filteredList = addFallBackItems(filteredList,userProfile);
					}else {
						LOGGER.debug("MostPopularModelImpl ::" + "init() ::" + "List size is correct, dont add fallbacks");
					}
					Collections.sort(filteredList, com);
					if(filteredList.size() > LIMIT) {
						filteredList = filteredList.subList(0, LIMIT);	
					}
				}else {
					UserProfile userProfile = null;
					filteredList = contentFilterService.fetchFilteredContent(request, userProfile, mostPopularItems, MostPopularItem.class, dateFormat);
					if(filteredList.size() < LIMIT) {
						LOGGER.debug("MostPopularModelImpl ::" + "init() ::" + "List size is less than 4 adding fallbacks");
						filteredList = addFallBackItems(filteredList,userProfile);
					}else {
						LOGGER.debug("MostPopularModelImpl ::" + "init() ::" + "List size is correct, dont add fallbacks");
					}
					Collections.sort(filteredList, com);
					if(filteredList.size() > LIMIT) {
						filteredList = filteredList.subList(0, LIMIT);	
					}
				}
			}
		}catch(Exception err) {
			LOGGER.error("Error in init of Most popular model impl" + err.toString());
		}
	}


	@Override
	public List<MostPopularItem> getFilteredList() {
		return filteredList;
	}

	@Override
	public String getComponentName() {
		return MadisonConstants.MOST_POPULAR_COMPONENT_NAME;
	}


	/**
	 * Method to add fallbackitems if filteredlist items are less than 4
	 * 
	 * 
	 */
	private List<MostPopularItem> addFallBackItems(List<MostPopularItem> filteredList,UserProfile userProfile){
		try {
			int count = LIMIT - filteredList.size();
			LOGGER.debug("MostPopularModelImpl" + "addFallBackItems() ::" + "These items will be from fallback ::" + count);
			LOGGER.debug("Fallback item size" + fallBackItems.size());
			for (MostPopularItem fallbackItem : fallBackItems) {
				MostPopularItem mostPopularItem = contentFilterService.fetchFallbackItems(fallbackItem, MostPopularItem.class, dateFormat);
				if(mostPopularItem != null) {
					if(filteredList.stream().anyMatch(item -> item.getArticlePagePath().equalsIgnoreCase(mostPopularItem.getArticlePagePath()))) {
						LOGGER.debug("Items are same in fallback as well in fixed list:" + mostPopularItem.getArticlePagePath());
					}else {
						filteredList.add(mostPopularItem);
					}
				}
			}
			return filteredList;
		}catch(Exception err) {
			LOGGER.error("MostPopularModelImpl" + "addFallBackItems() :: Error ::"  + err.toString());
		}
		return filteredList;
	}


	/**
	 * Method to get the list of page paths from multifield and
	 * identifying items which are fallbacks 
	 * 
	 */
	private List<MostPopularItem> getListOfPagePaths(Iterable<Resource> iterableResources, ResourceResolver resourceResolver) {
		LOGGER.debug("MostPopularModelImpl :: getListOfPagePaths() ::" + "Getting list of page path from nodes");
		listOfPagePaths = new ArrayList<MostPopularItem>();
		fallBackItems = new ArrayList<MostPopularItem>();
		try{
			for (Resource childResource : iterableResources) 
			{
				ValueMap properties = childResource.getValueMap();
				MostPopularItem item = new MostPopularItem();
				item.setArticlePagePath(properties.get(ARTICLE_PAGE_PATH, String.class));
                item.setPath(properties.get(ARTICLE_PAGE_PATH, String.class));
                if(properties.containsKey(IMAGE_PATH) && null != properties.get(IMAGE_PATH, String.class)) {
                	item.setImagePath(properties.get(IMAGE_PATH, String.class));
                }
				item.setNoOfViews(properties.get(NO_OF_VIEWS, String.class));
				item.setOpenInNewWindow(properties.containsKey(OPEN_IN_NEW_WINDOW) ? properties.get(OPEN_IN_NEW_WINDOW, Boolean.class) : Boolean.FALSE);
				LOGGER.debug("MostPopularModelImpl :: getListOfPagePaths() :: Page Paths in fixed list::" + item.getArticlePagePath());
				HomePageUtil.setAccessType(resourceResolver, item);
				listOfPagePaths.add(item);
				String isFallBack = properties.get(FALLBACK_FOR_MOST_POPULAR, String.class);
				if(isFallBack != null && isFallBack.equalsIgnoreCase(TRUE_TEXT)) {
					LOGGER.info("MostPopularModelImpl :: getListOfPagePaths() :: Fallbacks in fixedList ::" + item.getArticlePagePath());
					fallBackItems.add(item);
				}
			}
			return listOfPagePaths;
		}catch(Exception err) {
			LOGGER.info(err.toString());
		}
		return listOfPagePaths;
	}


	/**
	 * Comparator Implementation Used for sorting most popular 
	 * items according to number of views.
	 */
	public class ComparatorImpl implements Comparator<Item>{
		@Override
		public int compare(Item item1, Item item2) {
			int result = 0;
			try {
				int a = Integer.parseInt(item1.getNoOfViews());
				int b = Integer.parseInt(item2.getNoOfViews());
				if(a<b) {
					result = 1;
				} else if(a == b) {
					result = 2;							
				} else {
					result = -1;
				}
				if(result == 2) {
					Date date1 = null;
					Date date2 = null;
					if (item1.getUnformattedPublicationDate() != null && item2.getUnformattedPublicationDate() != null) {
						date1 = dateFormatter.parse(item1.getUnformattedPublicationDate());
						date2 = dateFormatter.parse(item2.getUnformattedPublicationDate());
					}
					return date2.compareTo(date1);
				}
			}catch(Exception err) {
				LOGGER.info("Error in comparator in MostPopularModelImpl" + err.toString());
			}
			return result;
		}
	}

}
