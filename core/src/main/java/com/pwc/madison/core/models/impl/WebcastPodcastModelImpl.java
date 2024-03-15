package com.pwc.madison.core.models.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.granite.crypto.CryptoSupport;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.Item;
import com.pwc.madison.core.models.WebcastPodcastModel;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.services.WebcastPodcastService;
import com.pwc.madison.core.userreg.models.User;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.util.HomePageUtil;
import com.pwc.madison.core.util.MadisonUtil;

@Model(
        adaptables = {SlingHttpServletRequest.class},
        adapters = {WebcastPodcastModel.class},
        resourceType = {WebcastPodcastModelImpl.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)

@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class WebcastPodcastModelImpl implements WebcastPodcastModel {
    protected static final String RESOURCE_TYPE = "pwc-madison/components/content/home/webcast-&-podcast";
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    private List<Item> podcastItemList = new ArrayList<Item>();
	private List<Item> listOfPagePaths;
	private List<Item> fallBackItems;
    private String dateFormat;
    private static final String CONTENT_PATH = "contentPath";
    private static final String IMAGE_PATH = "imagePath";
    private static final String FIXEDLIST_FALLBACK_PROP = "fallbackForWebcasts";
    private static final Integer LOWER_LIMIT = 2;

    @Inject
    private SlingHttpServletResponse response;
    @Self
    private SlingHttpServletRequest request;
    @ChildResource
    @Optional
    private Resource sourceItems;
    @ScriptVariable
    private ResourceResolver resolver;
    @Inject
    private WebcastPodcastService webcastPodcastService;
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

    @PostConstruct
    protected void init() {

        if (Objects.nonNull(this.sourceItems)) {
            try {
                String pagePath = request.getRequestURI();
                Resource requestPageResource = request.getResourceResolver().resolve(pagePath);
                pagePath = requestPageResource != null ? requestPageResource.getPath() : pagePath;
                String requestPageTerritoryCode = MadisonUtil.getTerritoryCodeForPath(pagePath);
                dateFormat = MadisonUtil.fetchDateFormat(requestPageTerritoryCode, countryTerritoryMapperService, MadisonConstants.HOMEPAGE_COMPONENTS_DATE_FORMAT);
                List<Item> featuredContentItems = getListOfPagePaths(this.sourceItems.getChildren(), this.resolver);
            	podcastItemList = this.webcastPodcastService.fetchContent(request, this.getUserProfile(), this.resolver, featuredContentItems, dateFormat);
            	
            	if(podcastItemList.size() < LOWER_LIMIT) {
            		podcastItemList = addFallBackItems(podcastItemList, this.resolver);
            	}
            	
            } catch (final Exception e) {
                this.LOG.error("Exception in WebcastPodcastModelImpl :: init Method :: Exception is {} ", e);
            }
        }
    }

	public List<Item> getListOfPagePaths(Iterable<Resource> children, ResourceResolver resourceResolver) {
    	listOfPagePaths = new ArrayList<Item>();
		fallBackItems = new ArrayList<Item>();
		try {
			for (Resource childResource : children) {
				Item item = new Item();
				ValueMap properties = childResource.getValueMap();
				item.setArticlePagePath(properties.get(CONTENT_PATH, String.class));
				HomePageUtil.setAccessType(resourceResolver, item);
				
				item.setPath(item.getArticlePagePath());
				item.setImagePath(properties.get(IMAGE_PATH, String.class));
				listOfPagePaths.add(item);
				String isFallBack = properties.get(FIXEDLIST_FALLBACK_PROP, String.class);
				if(isFallBack != null && isFallBack.equalsIgnoreCase(MadisonConstants.TRUE_TEXT)) {
					fallBackItems.add(item);
				}
			}
		}catch(Exception e) {
			LOG.error("WebcastPodcastModelImpl :: getListOfPagePaths() :: Exception in method getListOfPagePaths {} ",e);
		}
		return listOfPagePaths;
    }

	private User getUserProfile() {
        final User user = UserInformationUtil.getUser(this.request, false, this.userRegRestService,
                this.cryptoSupport, this.response, true, this.countryTerritoryMapperService,
                this.userPreferencesProviderService, false, false, xssapi);
        return user;
    }
    
    private List<Item> addFallBackItems(List<Item> podcastItemList, ResourceResolver resolver) {
    	try {
			for (Item fallbackItem : fallBackItems) {
				Item podcastItem = webcastPodcastService.fetchFallbackItems(fallbackItem, resolver, dateFormat);
				if(podcastItem != null) {
					if(!podcastItemList.stream().anyMatch(item -> item.getArticlePagePath().equalsIgnoreCase(podcastItem.getArticlePagePath()))) {
						podcastItemList.add(podcastItem);
					}
				}
			}
		}catch(Exception e) {
			LOG.error("WebcastPodcastModelImpl :: addFallbackItems() :: Exception in method addFallBackItems {} ",e);
		}
		return podcastItemList;
	}

    @Override
    public List<Item> getPodcastList() {
        return podcastItemList;
    }

	@Override
	public String getComponentName() {
		return MadisonConstants.WEBCASTS_AND_PODCASTS_COMPONENT_NAME;
	}
}
