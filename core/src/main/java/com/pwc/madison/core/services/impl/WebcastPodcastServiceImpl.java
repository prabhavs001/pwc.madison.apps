package com.pwc.madison.core.services.impl;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.pwc.madison.core.authorization.enums.AudienceType;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.Item;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.services.WebcastPodcastService;
import com.pwc.madison.core.userreg.models.User;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Service containing business logic for
 * WebcastPodcast Homepage Component
 * PwC Viewpoint
 */
@Component(
        service = {WebcastPodcastService.class},
        immediate = true,
        property = {"service.description=" + "WebcastPodcast component service implementation"})
public class WebcastPodcastServiceImpl implements WebcastPodcastService {
    private static final String OBJECT_REFERENCE_PATH = "/jcr:content/root/maincontainer/readerrow/bodycontainer/docreader/contentbody/topicbody/pwc-topic/pwc-body/bodydiv/object";
    private static final String POSTER_IMAGE_PATH = "/jcr:content/renditions/poster.png";
    private static final String FILE_REFERENCE = "fileReference";
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    public Boolean isAuthorized(final Resource authoredRes, final User user, final ResourceResolver resolver) {
        final ValueMap props = this.getPagePropertiesFromAuthoredNode(authoredRes, resolver);
        String pageAudienceType = StringUtils.EMPTY;
        pageAudienceType = props.get(DITAConstants.META_AUDIENCE, StringUtils.EMPTY);
        if (Objects.nonNull(user.getUserProfile()) && user.getUserProfile().getIsInternalUser()) {
            return this.checkForInternalUserOrPrivateGroup(user.getUserProfile(), props);
        } else
            return this.checkForAnonymousOrExternal(pageAudienceType);
    }

    @Override
    public List<Item> fetchContent(final SlingHttpServletRequest request, final User user, final ResourceResolver resourceResolver, final List<Item> itemList, final String dateFormat) {
    	
		List<Item> filteredList = new LinkedList<Item>();
		for (Item item : itemList) {
			Resource pageResource = resourceResolver.getResource(item.getArticlePagePath());
			if (pageResource != null && pageResource instanceof Resource) {
				String pagePath = item.getArticlePagePath();				
				LOG.debug("fetchContent: Setting Item object for content Path:: {}", pagePath);
				ValueMap pageProps = this.getPagePropertiesFromAuthoredNode(pageResource, resourceResolver);
				boolean isValidContent = true;
				if (MadisonUtil.isPublishMode(request)) {
					isValidContent = isAuthorized(pageResource, user, resourceResolver);
				}
				if (isValidContent) {
					if (Objects.nonNull(pageProps)) {
						Page page = pageResource.adaptTo(Page.class);
						if (page != null && page instanceof Page) {
							final boolean hidePublicationDate = DITAUtils.isHidePublicationDate(page.getContentResource()).equals(MadisonConstants.YES) ? true : false;
							item.setHidePublicationDate(hidePublicationDate);
						}
						String contentTitle = this.getContentTitle(pageProps);
						String imagePath = this.getThumbnailImage(item.getImagePath(), item.getArticlePagePath(), resourceResolver);
						if(!item.isHidePublicationDate()) {
							String publicationDate = pageProps.get(MadisonConstants.PWC_PUBLICATION_DATE, String.class);
							item.setPublicationDate(DITAUtils.formatDate(publicationDate, dateFormat));
						}
						item.setTopicTitle(Objects.nonNull(contentTitle) ? contentTitle : StringUtils.EMPTY);
						item.setArticlePagePath(pagePath.concat(MadisonConstants.HTML_EXTN));
						
						item.setContentFieldValue(MadisonUtil.getcontentFieldValue(pageProps));
						item.setCountry(MadisonUtil.getTerritoryCodeFromPagePath(item.getArticlePagePath()).toUpperCase());
						item.setImagePath(imagePath);
						item.setPath(pagePath);
						filteredList.add(item);
					}
				}

			}

		}
		return filteredList;
    }
    
	@Override
	public Item fetchFallbackItems(final Item item, final ResourceResolver resourceResolver, final String dateFormat) {

		Item filteredList = null;
		Resource pageResource = resourceResolver.getResource(item.getArticlePagePath());
		if (pageResource != null && pageResource instanceof Resource) {
			filteredList = new Item();
			String pagePath = item.getArticlePagePath();
			LOG.debug("fetchFallbackItems: Setting Item object for content Path:: {}", pagePath);
			ValueMap pageProps = this.getPagePropertiesFromAuthoredNode(pageResource, resourceResolver);
			if (Objects.nonNull(pageProps)) {
				String contentTitle = this.getContentTitle(pageProps);
				String imagePath = this.getThumbnailImage(item.getImagePath(), item.getArticlePagePath(), resourceResolver);
				String publicationDate = pageProps.get(MadisonConstants.PWC_PUBLICATION_DATE, String.class);
				filteredList.setTopicTitle(Objects.nonNull(contentTitle) ? contentTitle : StringUtils.EMPTY);
				filteredList.setArticlePagePath(pagePath.concat(MadisonConstants.HTML_EXTN));
				filteredList.setAccessType(item.getAccessType());
				filteredList.setLicenseTypes(item.getLicenseTypes());
				filteredList.setContentFieldValue(MadisonUtil.getcontentFieldValue(pageProps));
				filteredList.setCountry(MadisonUtil.getTerritoryCodeFromPagePath(item.getArticlePagePath()).toUpperCase());
				filteredList.setImagePath(imagePath);
				filteredList.setPath(pagePath);
				filteredList.setPublicationDate(DITAUtils.formatDate(publicationDate, dateFormat));
			}

		}
		return filteredList;
	}
    
    /**
     * Method to get ValueMap for authored content
     *
     * @param resource {@link Resource}
     * @param resolver {@link ResourceResolver}
     * @return ValueMap
     */
    private ValueMap getPagePropertiesFromAuthoredNode(final Resource resource, final ResourceResolver resolver) {
		final PageManager pm = resolver.adaptTo(PageManager.class);
		final Page page = pm.getContainingPage(resource);
		if (Objects.nonNull(page)) {
			return page.getProperties();
		}
		return null;
    }

    /**
     * Method to validate content for Internal User or Internal User
     * with private group
     *
     * @param userProfile UserProfile
     * @param pageProps   ValueMap
     * @return Boolean
     */
    private Boolean checkForInternalUserOrPrivateGroup(final UserProfile userProfile, final ValueMap pageProps) {
        this.LOG.debug("checkForInternalUserOrPrivateGroup :: User is internal");
        final List<String> userPrivateGroupsList = userProfile.getContentAccessInfo().getPrivateGroups();
        final String pageAudienceType = pageProps.get(DITAConstants.META_AUDIENCE, StringUtils.EMPTY);
        if (StringUtils.isNotBlank(pageAudienceType)) {
            if (pageAudienceType.equalsIgnoreCase(AudienceType.INTERNAL_ONLY.getValue()) ||
                    pageAudienceType.equalsIgnoreCase(AudienceType.INTERNAL_AND_EXTERNAL.getValue())) {
                return true;
            }
            if (Objects.nonNull(userPrivateGroupsList) && !userPrivateGroupsList.isEmpty()) {
                if (this.hasValidPrivateGroup(pageProps, userPrivateGroupsList))
                    return true;
            }
        }
        return false;
    }

    /**
     * Method to validate content for External User or Anonymous User
     *
     * @param audienceType String
     * @return Boolean
     */
    private Boolean checkForAnonymousOrExternal(final String audienceType) {
        this.LOG.debug("checkForInternalUserOrPrivateGroup :: User is external or Anonymous");
        return (audienceType.isEmpty() ||
                audienceType.equalsIgnoreCase(AudienceType.INTERNAL_AND_EXTERNAL.getValue()) ||
                audienceType.equalsIgnoreCase(AudienceType.EXTERNAL_ONLY.getValue()));
    }

    /**
     * Method to check if content has valid private group as per user's private groups
     *
     * @param pageProps            {@link ValueMap}
     * @param userPrivateGroupList {@link List}
     *                             return Boolean
     */
    private Boolean hasValidPrivateGroup(final ValueMap pageProps, final List<String> userPrivateGroupList) {
        final List<String> pagePrivateGroupList = this.getListOfPrivateGroupsfromProps(pageProps);
        if (!pagePrivateGroupList.isEmpty()) {
            for (final String privateGroupListItem : pagePrivateGroupList) {
                if (userPrivateGroupList.contains(privateGroupListItem))
                    return true;
            }
        }
        return false;
    }

    /**
     * Method to retrieve privateGroup list from ValueMap
     *
     * @param props {@link ValueMap}
     *              return privateGroupList {@link List}
     */
    private List<String> getListOfPrivateGroupsfromProps(final ValueMap props) {
        final List<String> privateGroupList = new ArrayList<String>();
        if (props.containsKey(DITAConstants.META_PRIVATE_GROUP)) {
            final String[] contentPrivateGroupList = props.get(DITAConstants.META_PRIVATE_GROUP, String[].class);
            privateGroupList.addAll(Arrays.asList(contentPrivateGroupList));
        }
        return privateGroupList;
    }


    /**
     * Method to get the title after setting the value
     *
     * @param pageProps @{@link ValueMap}
     * @return contentTitle {@link String}
     */
    private String getContentTitle(final ValueMap pageProps) {
        return !pageProps.get(MadisonConstants.PWC_PAGE_TITLE, StringUtils.EMPTY).isEmpty() ?
                pageProps.get(MadisonConstants.PWC_PAGE_TITLE, StringUtils.EMPTY) :
                pageProps.get(JcrConstants.JCR_TITLE, StringUtils.EMPTY);
    }

    /**
     * Method to get Thumbnail image either after reading the node passed by author
     * or by returning poster thumbnail image from inside the content
     *
     * @param pageProps
     * @param resourceProps
     * @return
     */
    private String getThumbnailImage(final String imagePath, final String articlePagePath, final ResourceResolver resolver) {
        if (StringUtils.isNotBlank(imagePath)) {
            return imagePath;
        } else {
            return this.getPosterImage(articlePagePath, resolver);
        }
    }

    /**
     * Method to get <b>Poster.png</b> when no image is passed by the author.
     * This method traverses the Dita content and returns poster.png rendition
     * for the same
     *
     * @param pagePath
     * @return posterImage {@link String}
     */
    private String getPosterImage(@Nonnull final String pagePath, final ResourceResolver resolver) {
        final Resource objectRes = resolver.getResource(pagePath.concat(OBJECT_REFERENCE_PATH));
            final ValueMap objectProps = objectRes.getValueMap();
            if (Objects.nonNull(objectProps) && objectProps.containsKey(FILE_REFERENCE)) {
                if (Objects.nonNull(objectProps.get(FILE_REFERENCE, String.class)))
                    return objectProps.get(FILE_REFERENCE, String.class).concat(POSTER_IMAGE_PATH);
            }
        return StringUtils.EMPTY;
    }
}
