package com.pwc.madison.core.models.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.pwc.madison.core.constants.MadisonConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.granite.crypto.CryptoSupport;
import com.pwc.madison.core.models.NewsListItem;
import com.pwc.madison.core.models.NewsListModel;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.services.NewsListService;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Sling Model for populating news list content.
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = NewsListModel.class)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class NewsListModelImpl implements NewsListModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewsListModelImpl.class);

    @Inject
    SlingHttpServletRequest request;

    @Inject
    SlingHttpServletResponse response;

    @Inject
    private transient UserRegRestService userRegRestService;

    @Inject
    private transient CryptoSupport cryptoSupport;

    @Inject
    private transient CountryTerritoryMapperService countryTerritoryMapperService;

    @Inject
    private transient UserPreferencesProviderService userPreferencesProviderService;

    @OSGiService
    private XSSAPI xssAPI;

    @ChildResource(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Resource dynamicItems;

    private List<String> dynamicItemsList;

    @ChildResource(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Resource fallbackItems;

    private List<String> fallbackItemsList;

    @OSGiService
    private NewsListService newsListService;

    private List<NewsListItem> newsItems;
    
    private String dateFormat;

    final private static String DYNAMIC_SUBNODE_NAME = "newsPagePath";
    final private static String FALLBACK_SUBNODE_NAME = "fallbackItemPath";

    /**
     * Init Method of Model.
     */
    @PostConstruct
    protected void init() {
        dynamicItemsList = new ArrayList<String>();
        fallbackItemsList = new ArrayList<String>();
        if (dynamicItems != null) {
            for (Resource resource : dynamicItems.getChildren()) {
                String newsItemPath = resource.getValueMap().get(DYNAMIC_SUBNODE_NAME, String.class);
                if (Objects.nonNull(newsItemPath)) {
                    dynamicItemsList.add(newsItemPath);
                }
            }
        }

        if (fallbackItems != null) {
            for (Resource resource : fallbackItems.getChildren()) {
                String fallbackItemPath = resource.getValueMap().get(FALLBACK_SUBNODE_NAME, String.class);
                if (Objects.nonNull(fallbackItemPath)) {
                    fallbackItemsList.add(fallbackItemPath);
                }
            }
        }
        
        try {
        	String pagePath = request.getRequestURI();
    		Resource requestPageResource = request.getResourceResolver().resolve(pagePath);
    		pagePath = requestPageResource != null ? requestPageResource.getPath() : pagePath;
    		String requestPageTerritoryCode = MadisonUtil.getTerritoryCodeForPath(pagePath);
    		dateFormat = MadisonUtil.fetchDateFormat(requestPageTerritoryCode, countryTerritoryMapperService, MadisonConstants.HOMEPAGE_COMPONENTS_DATE_FORMAT);
            UserProfile user = UserInformationUtil.getUserProfile(request, cryptoSupport, true, userRegRestService,
                    countryTerritoryMapperService, response, userPreferencesProviderService, true, false, xssAPI);
            if (null != user) {
                LOGGER.debug("User Profile: " + xssAPI.encodeForHTML(user.toString()));
                newsItems = newsListService.getFilteredList(dynamicItemsList, fallbackItemsList, user, dateFormat);
            } else {
                LOGGER.debug("User is Anonymous");
                newsItems = newsListService.getFilteredList(dynamicItemsList, fallbackItemsList, null, dateFormat);
            }
        } catch (Exception e) {
            LOGGER.error("Error in init() : NewsListModelImpl : {} ", e);
        }
    }

    @Override
    public List<NewsListItem> getNewsItems() {
        return newsItems;
    }

    @Override
    public String getComponentName() {
        return MadisonConstants.NEWS_COMPONENT_NAME;
    }
}
