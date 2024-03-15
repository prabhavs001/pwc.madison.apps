package com.pwc.madison.core.models.impl;

import java.text.ParseException;
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

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.FeaturedContentItem;
import com.pwc.madison.core.models.FeaturedContentModel;
import com.pwc.madison.core.services.ContentFilterService;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.userreg.utils.UserRegUtil;
import com.pwc.madison.core.util.HomePageUtil;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Sling Model for Featured Content Tile Component
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = FeaturedContentModel.class)
public class FeaturedContentModelImpl implements FeaturedContentModel {

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
	private transient UserPreferencesProviderService userPreferencesProviderService;

	@OSGiService
	private ContentFilterService contentFilterService;
	
    @OSGiService
    private transient XSSAPI xssapi;

	private List<FeaturedContentItem> listOfPagePaths;

	private List<FeaturedContentItem> fallBackItems;
    
    private String dateFormat;

	private Boolean isTarget;

	private static Integer LOWER_LIMIT = 1;
	private static Integer UPPER_LIMIT = 12;
	private static final String FIXEDLIST = "fixedList";
	private static final String FIXEDLIST_ARTICLE_PROP = "articlePagePath";
	private static final String FIXEDLIST_RENDITION_STYLE = "renditionStyle";
	private static final String FIXEDLIST_IMAGE_PATH = "imagePath";
	private static final String FIXEDLIST_FALLBACK_PROP = "fallbackForFeatured";
	private static final String TARGET_PROP_NAME = "targetActive";

	private List<FeaturedContentItem> filteredList = new ArrayList<FeaturedContentItem>();
	
	private SimpleDateFormat formatter = new SimpleDateFormat(MadisonConstants.HOMEPAGE_COMPONENTS_DATE_FORMAT);

	/**
	 * init Method of Model.
	 */
	@PostConstruct
	protected void init() {
		try {
			boolean sortList = (boolean) Objects.requireNonNullElse(request.getAttribute("sortList"),true);
			LOWER_LIMIT = (Integer) Objects.requireNonNullElse(request.getAttribute("lowerLimit"),LOWER_LIMIT);
			UPPER_LIMIT = (Integer) Objects.requireNonNullElse(request.getAttribute("upperLimit"),UPPER_LIMIT);

			String pagePath = request.getRequestURI();
			Resource requestPageResource = request.getResourceResolver().resolve(pagePath);
			pagePath = requestPageResource != null ? requestPageResource.getPath() : pagePath;
			String requestPageTerritoryCode = StringUtils.EMPTY;
			if(pagePath.startsWith(MadisonConstants.PWC_MADISON_XF_BASEPATH))
				requestPageTerritoryCode = MadisonUtil.getTerritoryCodeForXFPath(pagePath);
			else
				requestPageTerritoryCode = MadisonUtil.getTerritoryCodeForPath(pagePath);
			dateFormat = MadisonUtil.fetchDateFormat(requestPageTerritoryCode, countryTerritoryMapperService, MadisonConstants.HOMEPAGE_COMPONENTS_DATE_FORMAT);
			Resource pageResource = request.getResource();
			if(pageResource != null && pageResource instanceof Resource) {
				isTarget = Boolean.parseBoolean(pageResource.getValueMap().get(TARGET_PROP_NAME,MadisonConstants.FALSE_TEXT));

				Resource fixedListResource = pageResource.getChild(FIXEDLIST);
				if(fixedListResource != null && fixedListResource instanceof Resource) {
					List<FeaturedContentItem> featuredContentItems = getListOfPagePaths(fixedListResource.getChildren(), request.getResourceResolver());
					LOGGER.debug("value of isTarget is :: " + isTarget);
					if(isTarget) {
						filteredList = addFallBackItems(filteredList);
						if(filteredList.size() > UPPER_LIMIT) {
							filteredList = filteredList.subList(0, UPPER_LIMIT);
						}
					}else {
						final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
						if(madisonCookie != null) {
							UserProfile userProfile = UserInformationUtil.getUserProfile(request, cryptoSupport, true, userRegRestService,
									countryTerritoryMapperService, response,userPreferencesProviderService, true, false, xssapi);
							filteredList = contentFilterService.fetchFilteredContent(request, userProfile, featuredContentItems, FeaturedContentItem.class, dateFormat);
						} else {
							UserProfile userProfile = null;
							filteredList = contentFilterService.fetchFilteredContent(request, userProfile, featuredContentItems, FeaturedContentItem.class, dateFormat);
						}

						if(filteredList.size() < LOWER_LIMIT) {
							filteredList = addFallBackItems(filteredList);

							if(filteredList.size() >= LOWER_LIMIT)
								filteredList = filteredList.subList(0, LOWER_LIMIT);
						}

						if(filteredList.size() > UPPER_LIMIT)
							filteredList = filteredList.subList(0, UPPER_LIMIT);

					}
					if(sortList) {
						filteredList = getSortedList(filteredList);
					}
				}
			}


		}catch(Exception e) {
			LOGGER.error("Exception in init method :: FeaturedcontentModelimpl:: {}",e);
		}
	}

	private List<FeaturedContentItem> getListOfPagePaths(Iterable<Resource> iterableResources, ResourceResolver resourceResolver){
		listOfPagePaths = new ArrayList<FeaturedContentItem>();
		fallBackItems = new ArrayList<FeaturedContentItem>();

		try {
			for (Resource childResource : iterableResources) {
				FeaturedContentItem item = new FeaturedContentItem();
				ValueMap properties = childResource.getValueMap();
				item.setArticlePagePath(properties.get(FIXEDLIST_ARTICLE_PROP, String.class));
				item.setPath(item.getArticlePagePath());
				item.setRenditionStyle(properties.get(FIXEDLIST_RENDITION_STYLE, String.class));
				item.setImagePath(properties.get(FIXEDLIST_IMAGE_PATH, String.class));
				HomePageUtil.setAccessType(resourceResolver, item);
				listOfPagePaths.add(item);
				String isFallBack = properties.get(FIXEDLIST_FALLBACK_PROP, String.class);
				if(isFallBack != null && isFallBack.equalsIgnoreCase(MadisonConstants.TRUE_TEXT)) {
					fallBackItems.add(item);
				}
			}
		}catch(Exception e) {
			LOGGER.error("FeaturedContentModelImpl :: getListOfPagePaths :: Exception in method ",e);
		}
		return listOfPagePaths;
	}

	private List<FeaturedContentItem> getSortedList(final List<FeaturedContentItem> filteredList) {
		Collections.sort(filteredList, new Comparator<FeaturedContentItem>() {
			@Override
			public int compare(FeaturedContentItem item1, FeaturedContentItem item2) {
				Date date1 = null, date2 = null;
				try {
					if(StringUtils.isBlank(item1.getUnformattedPublicationDate())){
						return 1;
					} else {
						date1 = formatter.parse(item1.getUnformattedPublicationDate());
					}
				} catch (ParseException e) {
					LOGGER.error("getSortedList -> Error in parsing date", e);
				}
				try {
					if(StringUtils.isBlank(item2.getUnformattedPublicationDate())){
						return -1;
					} else {
						date2 = formatter.parse(item2.getUnformattedPublicationDate());
					}
				} catch (ParseException e) {
					LOGGER.error("getSortedList -> Error in parsing date", e);
				}

				if(Objects.isNull(date2)){
					return -1;
				}
				if (Objects.nonNull(date1)) {
					return date2.compareTo(date1);
				}
				return 1;
			}
		});
		return filteredList;
	}

	private List<FeaturedContentItem> addFallBackItems(List<FeaturedContentItem> filteredList){
		try {
			for (FeaturedContentItem fallbackItem : fallBackItems) {
				FeaturedContentItem featuredContentItem = contentFilterService.fetchFallbackItems(fallbackItem, FeaturedContentItem.class, dateFormat);
				if(featuredContentItem != null && !isTarget) {
					if(!filteredList.stream().anyMatch(item -> item.getArticlePagePath().equalsIgnoreCase(featuredContentItem.getArticlePagePath()))) {
						filteredList.add(featuredContentItem);
					}
				}else if(featuredContentItem != null && isTarget) {
					filteredList.add(featuredContentItem);
				}
			}
		}catch(Exception e) {
			LOGGER.error("FeaturedContentModelImpl :: addFallbackItems() :: Exception {}",e);
		}
		return filteredList;
	}

	@Override
	public List<FeaturedContentItem> getFilteredList() {
		return filteredList;
	}

	@Override
	public String getComponentName() {
		return MadisonConstants.FEATURED_COMPONENT_NAME;
	}
}
