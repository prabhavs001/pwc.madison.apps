package com.pwc.madison.core.models.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.HeroTileItem;
import com.pwc.madison.core.models.HeroTileModel;
import com.pwc.madison.core.models.Item;
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
 * Sling Model for Hero Tile Component
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = HeroTileModel.class)
public class HeroTileModelImpl implements HeroTileModel {

    /**
     * Default Logger
     */
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @ValueMapValue
    @Default(values = "false")
    @Optional
    private String isPersonalisationDisabled;

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

    @OSGiService
    private ContentFilterService contentFilterService;
    
    @OSGiService
    private transient XSSAPI xssapi;

    @Inject
    private transient UserPreferencesProviderService userPreferencesProviderService;

    @ChildResource
    @Optional
    private Resource fixedList;

    private List<HeroTileItem> filteredList;

    private List<HeroTileItem> listOfPagePaths;

    private List<HeroTileItem> fallBackItems;
    
    private String dateFormat;

    private static final String FIXED_LIST = "fixedList";
    private static final String ARTICLE_PAGE_PATH = "articlePagePath";
    private static final String FALLBACK_FOR_HERO_TILE = "fallbackForHeroTile";
    private static final String TRUE_TEXT = "true";
    private static final String CTA_LABEL = "cta";
    private static final String FIXEDLIST_IMAGE_PATH = "imagePath";
    private static final String RENDITION_STYLE = "stylerendition";
    private static final String STANDARD_RENDITION_STYLE = "standard";
    private static final Integer LOWER_LIMIT = 1;
    private static final Integer UPPER_LIMIT = 5;
    
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
            String requestPageTerritoryCode = StringUtils.EMPTY;
			if(pagePath.startsWith(MadisonConstants.PWC_MADISON_XF_BASEPATH))
				requestPageTerritoryCode = MadisonUtil.getTerritoryCodeForXFPath(pagePath);
			else
				requestPageTerritoryCode = MadisonUtil.getTerritoryCodeForPath(pagePath);
            dateFormat = MadisonUtil.fetchDateFormat(requestPageTerritoryCode, countryTerritoryMapperService, MadisonConstants.HOMEPAGE_COMPONENTS_DATE_FORMAT);
            if (Objects.nonNull(fixedList)) {
                List<HeroTileItem> heroTileItems = getListOfPagePaths(fixedList.getChildren(), request.getResourceResolver());
                if (Boolean.parseBoolean(isPersonalisationDisabled)) {
                    filteredList = new ArrayList<HeroTileItem>();
                    filteredList = addFallBackItems(filteredList);
                    if (filteredList.size() > UPPER_LIMIT)
                        filteredList = filteredList.subList(0, UPPER_LIMIT);
                } else
                    getPersonalisedList(heroTileItems);
            }
        } catch (Exception err) {
            LOGGER.info("Exception in init" + err.toString());
        }
    }

    private void getPersonalisedList(List<HeroTileItem> heroTileItems) throws Exception{
        final Cookie madisonCookie = UserRegUtil.getUserRegMadisonCookie(request);
        Comparator<Item> com = new ComparatorImpl();
        if (Objects.nonNull(madisonCookie)) {
            UserProfile userProfile = UserInformationUtil.getUserProfile(request, cryptoSupport, true, userRegRestService,
                    countryTerritoryMapperService, response, userPreferencesProviderService, true, false, xssapi);
            filteredList = contentFilterService.fetchFilteredContent(request, userProfile, heroTileItems, HeroTileItem.class, dateFormat);
            if (filteredList.size() < LOWER_LIMIT) {
                LOGGER.debug("HeroTileModelImpl ::" + "init() ::" + "List size is less than 1 ,adding fallbacks");
                filteredList = addFallBackItems(filteredList);
            } else
                LOGGER.debug("HeroTileModelImpl ::" + "init() ::" + "List size is correct, dont add fallbacks");
            Collections.sort(filteredList, com);
            if (filteredList.size() > UPPER_LIMIT)
                filteredList = filteredList.subList(0, UPPER_LIMIT);
        } else {
            filteredList = contentFilterService.fetchFilteredContent(request, null, heroTileItems, HeroTileItem.class, dateFormat);
            if (filteredList.size() < LOWER_LIMIT) {
                LOGGER.debug("HeroTileModelImpl ::" + "init() ::" + "List size is less than 1 adding fallbacks");
                filteredList = addFallBackItems(filteredList);
            } else
                LOGGER.debug("HeroTileModelImpl ::" + "init() ::" + "List size is correct, dont add fallbacks");
            Collections.sort(filteredList, com);
            if (filteredList.size() > 1)
                filteredList = filteredList.subList(0, UPPER_LIMIT);
        }
    }

    /**
     * Method to add fallback items if filtered list items are less than LOWER_LIMIT
     *
     * @param filteredList {@link List<HeroTileItem>}
     * @return {@link HeroTileItem}
     */
    private List<HeroTileItem> addFallBackItems(List<HeroTileItem> filteredList) {
        try {
            for (HeroTileItem fallbackItem : fallBackItems) {
                HeroTileItem heroFallbackItem = contentFilterService.fetchFallbackItems(fallbackItem, HeroTileItem.class, dateFormat);
                if (heroFallbackItem != null)
                    filteredList.add(heroFallbackItem);
            }
        } catch (Exception err) {
            LOGGER.error("HeroTileModelImpl" + "addFallBackItems() :: Error ::" + err.toString());
        }
        return filteredList;
    }

    /**
     * Method to get the list of page paths from multifield and
     * identifying items which are fallbacks
     *
     * @param iterableResources {@link Iterable<Resource>}
     * @return {@link HeroTileItem}
     */
    private List<HeroTileItem> getListOfPagePaths(Iterable<Resource> iterableResources, ResourceResolver resourceResolver) {
        LOGGER.debug("HeroTileModelImpl :: getListOfPagePaths() ::" + "Getting list of page path from nodes");
        listOfPagePaths = new ArrayList<HeroTileItem>();
        fallBackItems = new ArrayList<HeroTileItem>();
        try {
            for (Resource childResource : iterableResources) {
                ValueMap properties = childResource.getValueMap();
                HeroTileItem item = new HeroTileItem();
                String renditionStyle = properties.get(RENDITION_STYLE, String.class);

                if (StringUtils.isBlank(renditionStyle))
                    item.setRenditionStyle(STANDARD_RENDITION_STYLE);
                else
                    item.setRenditionStyle(renditionStyle);
                item.setArticlePagePath(properties.get(ARTICLE_PAGE_PATH, String.class));
                item.setPath(item.getArticlePagePath());
                item.setCtaLabel(properties.get(CTA_LABEL, String.class));
                item.setImagePath(properties.get(FIXEDLIST_IMAGE_PATH, String.class));
                LOGGER.debug("HeroTileModelImpl :: getListOfPagePaths() :: Page Paths in fixed list::" + item.getArticlePagePath());
                
                HomePageUtil.setAccessType(resourceResolver, item);
                		
                listOfPagePaths.add(item);
                Boolean isFallBack = Boolean.parseBoolean(properties.get(FALLBACK_FOR_HERO_TILE, MadisonConstants.FALSE_TEXT));
                if (isFallBack) {
                    LOGGER.debug("HeroTileModelImpl :: getListOfPagePaths() :: Fallbacks in fixedList ::" + item.getArticlePagePath());
                    fallBackItems.add(item);
                }
            }
            return listOfPagePaths;
        } catch (Exception err) {
            LOGGER.info(err.toString());
        }
        return listOfPagePaths;
    }
    
    /**
     * Comparator Implementation Used for sorting most popular
     * items according to number of views.
     */
    public class ComparatorImpl implements Comparator<Item> {
        @Override
        public int compare(Item item1, Item item2) {
            try {
                return formatter.parse(item2.getUnformattedPublicationDate()).compareTo(formatter.parse(item1.getUnformattedPublicationDate()));
            } catch (Exception err) {
                LOGGER.info(err.toString());
            }
            return -1;
        }
    }

    @Override
    public List<HeroTileItem> getFilteredList() {
        return filteredList;
    }

    @Override
    public String getComponentName() {
        return MadisonConstants.HERO_COMPONENT_NAME;
    }

}
