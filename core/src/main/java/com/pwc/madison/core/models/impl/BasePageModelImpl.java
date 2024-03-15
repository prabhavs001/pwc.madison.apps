package com.pwc.madison.core.models.impl;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.commons.Externalizer;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.foundation.Image;
import com.google.common.collect.Lists;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.BasePageModel;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.services.InlineLinksConfigService;
import com.pwc.madison.core.userreg.services.UserRegPagesPathProvidesService;
import com.pwc.madison.core.util.DITALinkUtils;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.*;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.*;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.Session;
import java.util.*;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = BasePageModel.class,
    resourceType = BasePageModelImpl.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class BasePageModelImpl implements BasePageModel {

    private static final String ID = "id";

	private static final Logger LOGGER = LoggerFactory.getLogger(BasePageModelImpl.class);

    protected static final String RESOURCE_TYPE = "pwc-madison/components/structure/page";
    
    private static final String P_LIMIT = "p.limit";
	private static final String P_LIMIT_VALUE = "-1";
	private static final String FMDITA_COMPONENTS_DITA_TOPIC = "fmdita/components/dita/topic";
	private static final String PATH = "path";

    private String pageTitle = StringUtils.EMPTY;
    private String pageUrl = StringUtils.EMPTY;
    private String pageImage = StringUtils.EMPTY;
    private String pageDesc = StringUtils.EMPTY;
    String pagePath = StringUtils.EMPTY;
    String pageLocale = StringUtils.EMPTY;
    String formattedTags = StringUtils.EMPTY;
    String formattedPublicationDateString = null;
    String formattedExpiryDateString = null;
    String formattedRevisedDateString = null;
    String formattedOriginalReleaseDateString = null;
    String pwcDocContext = null;
    String hidePublicationDate = "no";

    private TagManager tagManager = null;

    @ScriptVariable
    private Page currentPage;

    @Self
    private SlingHttpServletRequest request;

    @Inject
    SlingSettingsService slingSettingsService;

    @SlingObject
    private ResourceResolver resourceResolver;

    @OSGiService
    private UserRegPagesPathProvidesService userRegPagesPathProvidesService;

    @OSGiService
    private CountryTerritoryMapperService countryTerritoryMapperService;

    @OSGiService
    private ResourceResolverFactory resolverFactory;
    
    @OSGiService
    private QueryBuilder queryBuilder;
    
    @OSGiService
    private InlineLinksConfigService inlineLinksConfigService;

    @ValueMapValue(name = DITAConstants.META_CONTENT_TYPE)
    private String contentType;

    private String country;

    private String language;

    @ValueMapValue(name = DITAConstants.META_TAGS)
    private String[] tags;

    @ValueMapValue(name = DITAConstants.META_CONTENT_ID)
    private String contentID;

    @ValueMapValue(name = DITAConstants.META_AUDIENCE)
    private String audience;

    @ValueMapValue(name = DITAConstants.META_PRIVATE_GROUP)
    private String[] privateGroup;

    @ValueMapValue(name = DITAConstants.META_ACCESS_LEVEL)
    private String accessLevel;

    @ValueMapValue(name = DITAConstants.META_LICENSE)
    private String[] license;

    @ValueMapValue(name = DITAConstants.META_STANDARD_SETTERS)
    private String standardSetter;

    @ValueMapValue(name = DITAConstants.META_KEYWORDS)
    private String[] keywords;
    
    @ValueMapValue(name = DITAConstants.META_GUIDANCE_TERMS)
    private String[] guidanceTerms;
    
    @ValueMapValue(name = DITAConstants.META_SUGGESTED_GUIDANCE)
    private String suggestedGuidance;
    
    @ValueMapValue(name = DITAConstants.META_SUGGESTED_GUIDANCE_ORDER)
    @Default(values = "0")
    private String suggestedGuidanceOrder;

    @ValueMapValue(name = DITAConstants.META_REVISION_DATE)
    private Date revisedDate;

    @ValueMapValue(name = DITAConstants.META_REVISION_DATE)
    private String revisedDateString;

    @ValueMapValue(name = DITAConstants.META_ORIGINAL_RELEASE_DATE)
    private Date originalReleaseDate;

    @ValueMapValue(name = DITAConstants.META_REVISION_DATE)
    private String originalReleaseDateString;

    @ValueMapValue(name = DITAConstants.META_PUBLICATION_DATE)
    private Date publicationDate;

    @ValueMapValue(name = DITAConstants.META_PUBLICATION_DATE)
    private String publicationDateString;
    
    @ValueMapValue(name = DITAConstants.META_EXPIRY_DATE)
    private Date expiryDate;

    @ValueMapValue(name = DITAConstants.META_EXPIRY_DATE)
    private String expiryDateString;

    @ValueMapValue(name = JcrConstants.JCR_CREATED)
    private Date createdDate;

    @ValueMapValue(name = DITAConstants.META_HIDE_PAGE_FROM_SITE)
    @Default(booleanValues = false)
    private boolean hiddenFromSiteSearch;

    @ValueMapValue(name = DITAConstants.META_CANONICAL_URL)
    @Default(values = StringUtils.EMPTY)
    private String canonicalUrl;

    @ValueMapValue(name = DITAConstants.META_ROBOTS)
    @Default(values = "index")
    private String metaRobots;

    @ValueMapValue(name = DITAConstants.META_SORT_ORDER)
    @Default(values = StringUtils.EMPTY)
    private String sortOrder;

    @ValueMapValue(name = DITAConstants.BASE_PATH)
    @Default(values = StringUtils.EMPTY)
    private String dataLayerBookMapBasePath;
    
    @ValueMapValue(name = "disableCompleteProfile")
    @Default(booleanValues = false)
    private boolean disableCompleteProfile;

    private String dataLayerBookMap;
    private String dataLayerTopic;
    private String dataLayerMap;
    private boolean isHomePage;
    private String disableInlineLinks = "no";
    private String joinedPagePath;
    private String pageId;
    private String joinedLevel;
    private boolean isJoinedPage;

    /**
     * Set publication date to created date, if empty.
     */
    @PostConstruct
    protected void init() {
    	Resource currentPageContentResource = currentPage.getContentResource();
        final ValueMap valueMap = currentPageContentResource.getValueMap();
        String basePath = valueMap.get("basePath", String.class);
        Resource baseContentResource = resourceResolver.getResource(basePath + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
		if (null != baseContentResource && !ResourceUtil.isNonExistingResource(baseContentResource)) {
			ValueMap baseValueMap = baseContentResource.adaptTo(ValueMap.class);
			if ("yes".equals(baseValueMap.get(DITAConstants.META_JOINED_SECTION_TOC, String.class))) {
				joinedLevel = baseValueMap.get(DITAConstants.META_JOINED_SECTION_LEVEL, Integer.class).toString();
				InheritanceValueMap inheritedProperties = new HierarchyNodeInheritanceValueMap(currentPageContentResource);
		    	joinedPagePath = StringUtils.isNotBlank(inheritedProperties.getInherited(MadisonConstants.JOINED_PAGE_URL, String.class)) ? inheritedProperties.getInherited(MadisonConstants.JOINED_PAGE_URL, String.class) : StringUtils.EMPTY;
		    	// Get topic ID
				Resource topicResource = getTopicResource(currentPage.getContentResource().getPath(), resourceResolver);
				if(null != topicResource) {
					ValueMap topicValueMap = topicResource.adaptTo(ValueMap.class);
					if(topicValueMap.containsKey(ID)) {
						pageId = topicValueMap.get(ID, String.class);
					}
				}
			}
		}
        hidePublicationDate = DITAUtils.isHidePublicationDate(currentPageContentResource);
        isHomePage = currentPage.getPath().matches(MadisonConstants.MADISON_HOMEPAGE_HIERARCHY);
        isJoinedPage = currentPage.getPath().contains(DITAConstants.JOINED);
        
        if(currentPage.getTemplate().getName().equals(MadisonConstants.DITA_CONTENT_PAGE_TEMPLATE)){
            pageTitle = StringUtils.isNotBlank(currentPage.getPageTitle()) ? currentPage.getPageTitle()
                : currentPage.getTitle();
        }else{
            pageTitle = StringUtils.isNotBlank(currentPage.getNavigationTitle()) ? currentPage.getNavigationTitle()
                : currentPage.getTitle();
        }

        if (StringUtils.isBlank(pageTitle)) {
            pageTitle = currentPage.getName();
        }

        if (null!= currentPage && StringUtils.isNotBlank(currentPage.getPath())) {
            // Populate Country / Territory code
            country = MadisonUtil.getTerritoryCodeForPath(currentPage.getPath());
            // Populate Territory Language code
            language = MadisonUtil.getLanguageCodeForPath(currentPage.getPath());
        }
        
        pageDesc = currentPage.getDescription();
        final Externalizer externalizer = resourceResolver.adaptTo(Externalizer.class);
        if (null!= currentPage && StringUtils.isNotBlank(currentPage.getPath()) && null != externalizer){
            String runMode = MadisonUtil.getCurrentRunmode(slingSettingsService);
            runMode = StringUtils.isNotBlank(runMode) ? runMode : Externalizer.PUBLISH;
            pageUrl = externalizer.externalLink(resourceResolver, runMode,
                resourceResolver.map(currentPage.getPath())) + MadisonConstants.HTML_EXTN;
            pageUrl = pageUrl + (request.getQueryString() != null ? "?" + request.getQueryString() : StringUtils.EMPTY);
        }else{
            pageUrl = request.getRequestURL().toString()
                + (request.getQueryString() != null ? "?" + request.getQueryString() : StringUtils.EMPTY);
        }
        final Resource resource = currentPage.getContentResource("image");
        if (null != resource && null != externalizer) {
            String runMode = MadisonUtil.getCurrentRunmode(slingSettingsService);
            runMode = StringUtils.isNotBlank(runMode) ? runMode : Externalizer.PUBLISH;
            final Image image = new Image(resource);
            pageImage = externalizer.externalLink(resourceResolver, runMode,
                resourceResolver.map(currentPage.getPath() + ".img.png" + image.getSuffix()));
        }
        if (Objects.isNull(publicationDate)) {
            publicationDate = createdDate;
        }
        formattedPublicationDateString = DITAUtils.formatDate(publicationDateString, MadisonConstants.COMPONENTS_DATE_FORMAT);
        formattedExpiryDateString = DITAUtils.formatDate(expiryDateString, MadisonConstants.COMPONENTS_DATE_FORMAT);
        formattedRevisedDateString = DITAUtils.formatDate(revisedDateString, MadisonConstants.COMPONENTS_DATE_FORMAT);
        formattedOriginalReleaseDateString = DITAUtils.formatDate(originalReleaseDateString, MadisonConstants.COMPONENTS_DATE_FORMAT);

        pagePath = request.getRequestURI();
        if (null != pagePath && pagePath.startsWith(userRegPagesPathProvidesService.getBaseUserregPath())) {
            pageLocale = request.getParameter(MadisonConstants.LOCALE_QUERY_PARAM);
        }
        else if(null != pagePath && pagePath.matches(MadisonConstants.MADISON_PAGE_HIERARCHY)){
            pageLocale = MadisonUtil.getLocaleForPath(pagePath);
        }
        else {
            pageLocale = countryTerritoryMapperService.getDefaultLocale();
        }
        if(null != tags){
            formattedTags = MadisonUtil.formatPWCTags(tags, resourceResolver, pageLocale);
        }
        pwcDocContext = MadisonUtil.getPwcDocContext(currentPage.getPath(), resourceResolver, false);

        //DataLayer DitaMap/Map/Topic logic
        //the DitaMap represents the L1 Page
        //the Map represents the L2 Page
        //the Topic represents the L3 Page

        if(StringUtils.isNotEmpty(dataLayerBookMapBasePath)) {
            dataLayerTopic = currentPage.getTitle();

            //L1 from page properties
            Resource bookMapResource = resourceResolver.getResource(dataLayerBookMapBasePath);

            if (null != bookMapResource) {
                Page bookMapPage = bookMapResource.adaptTo(Page.class);

                dataLayerBookMap = bookMapPage.getTitle();

                //L2 recursion
                Iterator<Page> dataLayerMapIter = bookMapPage.listChildren();

                while (dataLayerMapIter.hasNext()) {
                    Page dataLayerMapPage = dataLayerMapIter.next();

                    if (currentPage.getPath().contains(dataLayerMapPage.getPath())) {
                        dataLayerMap = dataLayerMapPage.getTitle();
                    }
                }
            }
        }
        if(null != inlineLinksConfigService) {
        	disableInlineLinks = inlineLinksConfigService.getDisableInlineLinks();
        }

    }
    
	private Resource getTopicResource(String sourcePagePath, ResourceResolver resolver) {
		final Map<String, Object> predicateMap = new HashMap<>();
		long startTime = System.currentTimeMillis();
        predicateMap.put(P_LIMIT, P_LIMIT_VALUE);
        predicateMap.put(PATH, sourcePagePath);
        predicateMap.put("property", JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY);
        predicateMap.put("property.value", FMDITA_COMPONENTS_DITA_TOPIC);
        
        final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap), resolver.adaptTo(Session.class));
        final SearchResult searchResult = query.getResult();

        final Iterator<Resource> resources = searchResult.getResources();
        List<Resource> resList = Lists.newArrayList(resources);
        resList.sort((Resource res1, Resource res2) -> ((Integer)res1.getPath().split("/").length).compareTo((Integer)res2.getPath().split("/").length));
        LOGGER.debug("Retreiving topic node under {} took {} seconds to complete the process",sourcePagePath, (System.currentTimeMillis() - startTime) / 1000);
		return resList.isEmpty() ? null : resList.get(0);
	}

    @Override
    public String getDataLayerBookMap() {
        return dataLayerBookMap;
    }

    @Override
    public String getDataLayerTopic() {

        return dataLayerTopic;
    }

    @Override
    public String getDataLayerMap() {
        return dataLayerMap;
    }

    @Override
    public String getPageTitle() {
        return pageTitle;
    }

    @Override
    public String getPageUrl() {
        return pageUrl;
    }

    @Override
    public String getPageImage() {
        return pageImage;
    }

    @Override
    public String getPageDesc() {
        return pageDesc;
    }

    @Override
    public String[] getKeywords() {
        return keywords;
    }

    @Override
    public Date getRevisedDate() {
        return revisedDate;
    }

    @Override
    public Date getPublicationDate() {
        return publicationDate;
    }

    @Override
    public Date getOriginalReleaseDate() {
        return originalReleaseDate;
    }

    @Override
    public boolean hiddenFromSiteSearch() {
        return hiddenFromSiteSearch;
    }

    @Override
    public String getMetaRobots() {
        return metaRobots;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getCountry() {
        return country;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public String[] getTags() {
        return tags;
    }

    @Override
    public String getContentID() {
        return contentID;
    }

    @Override
    public String getAudience() {
        return audience;
    }

    @Override
    public String[] getPrivateGroup() {
        return privateGroup;
    }

    @Override
    public String getAccessLevel() {
        return accessLevel;
    }

    @Override
    public String getLicense() {
    	if(ArrayUtils.isNotEmpty(license)) {
    		return String.join(MadisonConstants.COMMA_SEPARATOR, license);
    	}
        return StringUtils.EMPTY;
    }

    @Override
    public String getStandardSetter() {
        return standardSetter;
    }

    @Override
    public Date getCreatedDate() {
        return createdDate;
    }

    @Override
    public boolean isHiddenFromSiteSearch() {
        return hiddenFromSiteSearch;
    }

    @Override
    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    @Override
	public Date getExpiryDate() {
		return expiryDate;
	}

    @Override
    public String getFormattedTags() { return formattedTags; }

    @Override
    public String[] getGuidanceTerms() {
        return guidanceTerms;
    }

    @Override
    public String getSuggestedGuidance() {
        return suggestedGuidance;
    }
    
    public String getSuggestedGuidanceOrder() {
        return suggestedGuidanceOrder;
    }

    public String getFormattedPublicationDateString() { return formattedPublicationDateString; }

    @Override
    public String getFormattedExpiryDateString() { return formattedExpiryDateString; }

    @Override
    public String getFormattedRevisedDateString() { return formattedRevisedDateString; }

    @Override
    public String getFormattedOriginalReleaseDateString() { 
        return formattedOriginalReleaseDateString; }

    @Override
    public String getPwcDocContext() {
        return pwcDocContext;
    }

    @Override
    public boolean isHomePage() {
        return isHomePage;
    }
    
    @Override
    public boolean isJoinedPage() {
        return isJoinedPage;
    }

    @Override
    public String getHidePublicationDate() {
        return hidePublicationDate;
    }
    
    @Override
    public String getDisableInlineLinks() {
        return disableInlineLinks;
    }
    
    @Override
    public String getJoinedPagePath() {
        return joinedPagePath;
    }
    
    @Override
    public String getPageId() {
        return pageId;
    }
    
    @Override
    public String getJoinedLevel() {
        return joinedLevel;
    }
    @Override
    public String getSortOrder(){
        return sortOrder;
    }

    public boolean isDisableCompleteProfile() {
		return disableCompleteProfile;
	}

}
