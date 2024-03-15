package com.pwc.madison.core.services.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;

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
import com.pwc.madison.core.authorization.enums.AudienceType;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.Item;
import com.pwc.madison.core.services.ContentFilterService;
import com.pwc.madison.core.services.MadisonDomainsService;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.HomePageUtil;
import com.pwc.madison.core.util.MadisonUtil;


/**
 * Content Filter Service Implementation
 */
@Component(service = {ContentFilterService.class}, immediate = true,
        property = {
                "service.description=" + "Content Filter Service Implementation"})
public class ContentFilterServiceImpl implements ContentFilterService {


    public static final String JAVA_LANG_STRING = "java.lang.String";
    /**
     * Default Logger
     */
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    ResourceResolver resourceResolver;

    @Reference
    private transient MadisonDomainsService madisonDomainsService;

    private static final String ITEMTYPE_FEATURED = "FeaturedContentItem";
    private static final String ITEMTYPE_MOSTPOPULAR = "MostPopularItem";
    private static final String ITEMTYPE_HEROTILE = "HeroTileItem";
    private static final String EXT_HTML = ".html";
    private static final String[] EMPTY_ARRAY = new String[0];


    String domain = StringUtils.EMPTY;

    @Activate
    protected void activated() {
        LOGGER.info("ContentFilterServiceImpl Activated!");
    }


    /**
     * Returns the object of Type T fetched from {@link Class<T>}}
     *
     * @param request     {@link SlingHttpServletRequest}
     * @param userProfile {@link UserProfile}
     * @param itemsList   {@link List<T extends Item>}
     * @param type        {@link Class<T>}
     *
     * @return {@link List<T>} of given {@link Class<T>}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Item> List<T> fetchFilteredContent(SlingHttpServletRequest request, UserProfile userProfile, List<T> itemsList, Class<T> type, String dateFormat) {
        List<T> filteredList = null;
        try {
            boolean isItAFallBackItem = false;
            Gson gson = new Gson();
            resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                    MadisonConstants.MADISON_READ_SUB_SERVICE);
            filteredList = new LinkedList<T>();
            for (T item : itemsList) {
                Resource pageResource = resourceResolver.getResource(item.getArticlePagePath());
                if (pageResource != null && pageResource instanceof Resource) {
                    JSONObject json = new JSONObject(item);
                    T singleItem = gson.fromJson(json.toString(), type);
                    Page pageItem = pageResource.adaptTo(Page.class);
                    ValueMap pageProperties = pageItem.getProperties();
                    boolean isValidContent = true;
                    LOGGER.debug("ContentFilterServiceImpl :: fetchFilteredContent() :: Checking if this path ::" + item.getArticlePagePath() + " is valid or not");
                    if (MadisonUtil.isPublishMode(request)) {
                        isValidContent = isValidContent(pageProperties, userProfile, isItAFallBackItem);
                    }
                    if (isValidContent) {
                        T filteredItem = (T) HomePageUtil.getInstance(type);
                        if (type.getSimpleName().equalsIgnoreCase(ITEMTYPE_MOSTPOPULAR)) {
                            filteredItem = addFilteredItem(filteredItem, pageProperties, singleItem, dateFormat);
                            filteredList.add(filteredItem);
                        } else if (type.getSimpleName().equalsIgnoreCase(ITEMTYPE_FEATURED)) {
                            Resource ghostPropertiesResource = pageResource.getChild(MadisonConstants.GHOST_MODULE_PATH);
                            if (ghostPropertiesResource != null && ghostPropertiesResource instanceof Resource) {
                                LOGGER.debug("Its a Ghost Path");
                                filteredItem = addFeaturedFilteredItem(filteredItem, pageProperties, singleItem, ghostPropertiesResource, dateFormat);
                            } else {
                                ghostPropertiesResource = null;
                                filteredItem = addFeaturedFilteredItem(filteredItem, pageProperties, singleItem, ghostPropertiesResource, dateFormat);
                            }
                            filteredList.add(filteredItem);
                        } else if (type.getSimpleName().equalsIgnoreCase(ITEMTYPE_HEROTILE)) {
                            filteredItem = addFilteredHeroItem(filteredItem, pageProperties, singleItem, dateFormat);
                            filteredList.add(filteredItem);
                        }
                    }
                }
            }
            return filteredList;
        } catch (Exception e) {
            LOGGER.error("Error in fetchFilteredContent() method {} ", e);
        } finally {
            if (null != resourceResolver && resourceResolver.isLive())
                resourceResolver.close();
        }
        return filteredList;
    }


	/**
     * Method used for filtering the pages
     */
    @Override
    public boolean isValidContent(ValueMap pageProperties, UserProfile userProfile, boolean isItAFallBackItem) {
        try {
            String audienceType = pageProperties.get(DITAConstants.META_AUDIENCE, StringUtils.EMPTY);
            if (userProfile == null) {
                LOGGER.debug("ContentFilterServiceImpl :: isValidContent() :: Anonymous user, fetching content for him without any prefernces");
                if (audienceType.isEmpty() || audienceType.equalsIgnoreCase(AudienceType.INTERNAL_AND_EXTERNAL.getValue()) || audienceType.equalsIgnoreCase(AudienceType.EXTERNAL_ONLY.getValue())) {
                    LOGGER.debug("ContentFilterServiceImpl :: isValidContent() :: It is a valid content");
                    return true;
                }
            } else {
                String userType = AudienceType.EXTERNAL_ONLY.getValue();
                LOGGER.debug("ContentFilterServiceImpl :: isValidContent() :: LOGGED IN user, fetching content for him with preferences");
                boolean isUserPreferenceMatched = matchingUserPreferences(pageProperties, userProfile);
                boolean doUserHaveThePrivateGroup = false;
                if (userProfile.getIsInternalUser()) {
                    userType = AudienceType.INTERNAL_ONLY.getValue();
                    List<String> userPrivateGroupsList = userProfile.getContentAccessInfo().getPrivateGroups();
                    if (userPrivateGroupsList != null && userPrivateGroupsList.size() != 0) {
                        Object obj = pageProperties.get(DITAConstants.META_PRIVATE_GROUP);
                        if (obj != null) {
                            if (obj.getClass().getName().equalsIgnoreCase(JAVA_LANG_STRING)) {
                                String privateGroup = pageProperties.get(DITAConstants.META_PRIVATE_GROUP, StringUtils.EMPTY);
                                if (userPrivateGroupsList.contains(privateGroup)) {
                                    LOGGER.debug("ContentFilterServiceImpl :: isValidContent :: Group on page" + privateGroup);
                                    doUserHaveThePrivateGroup = true;
                                }
                            } else {
                                String[] privateGroupList = (String[]) pageProperties.get(DITAConstants.META_PRIVATE_GROUP);
                                if (privateGroupList.length != 0) {
                                    for (String privateGroupListItem : privateGroupList) {
                                        if (userPrivateGroupsList.contains(privateGroupListItem)) {
                                            LOGGER.debug("ContentFilterServiceImpl :: isValidContent :: Group on page MATCHED WITH USER" + privateGroupListItem);
                                            doUserHaveThePrivateGroup = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (isItAFallBackItem) {
                    LOGGER.debug("Page is valid content :: and a fallbackcontent" + pageProperties.get("jcr:title"));
                    return true;
                } else {
                    if ((audienceType.isEmpty() || userType.equalsIgnoreCase(audienceType) || audienceType.equalsIgnoreCase(AudienceType.INTERNAL_AND_EXTERNAL.getValue()) || doUserHaveThePrivateGroup) && isUserPreferenceMatched) {
                        LOGGER.debug("Page is valid content ::" + pageProperties.get("jcr:title"));
                        return true;
                    }
                }
            }
        } catch (Exception err) {
            LOGGER.error("Error in isValidContent method {} ", err);
        }
        return false;
    }


    /**
     * Method Used for fetchingFallbackItems
     */

    @Override
    public <T extends Item> T fetchFallbackItems(T item, Class<T> type, String dateFormat) {
        LOGGER.debug("ContentFilterServiceImpl :: fetchFallbackItems() :: Fetching fallbackItems");
        T filteredItem = null;
        try {
            Gson gson = new Gson();
            resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                    MadisonConstants.MADISON_READ_SUB_SERVICE);
            Resource pageResource = resourceResolver.getResource(item.getArticlePagePath());
            if (pageResource != null && pageResource instanceof Resource) {
                filteredItem = type.newInstance();
                JSONObject json = new JSONObject(item);
                T singleItem = gson.fromJson(json.toString(), type);
                Page pageItem = pageResource.adaptTo(Page.class);
                ValueMap pageProperties = pageItem.getProperties();
                if (type.getSimpleName().equalsIgnoreCase(ITEMTYPE_MOSTPOPULAR)) {
                    filteredItem = addFilteredItem(filteredItem, pageProperties, singleItem, dateFormat);
                    LOGGER.debug("ContentFilterServiceImpl :: fetchFallbackItems() :: FallbackItemPath is  :: " + item.getArticlePagePath());
                } else if (type.getSimpleName().equalsIgnoreCase(ITEMTYPE_HEROTILE)) {
                    filteredItem = addFilteredHeroItem(filteredItem, pageProperties, singleItem, dateFormat);
                    LOGGER.debug("ContentFilterServiceImpl :: fetchFallbackItems() :: FallbackItemPath is  :: " + item.getArticlePagePath());
                } else if (type.getSimpleName().equalsIgnoreCase(ITEMTYPE_FEATURED)) {
                    LOGGER.debug("Fallback item Type :: Featured");
                    Resource ghostPropertiesResource = pageResource.getChild(MadisonConstants.GHOST_MODULE_PATH);
                    if (ghostPropertiesResource != null && ghostPropertiesResource instanceof Resource) {
                        filteredItem = addFeaturedFilteredItem(filteredItem, pageProperties, singleItem, ghostPropertiesResource, dateFormat);
                        LOGGER.debug("ContentFilterServiceImpl :: fetchFallbackItems() :: Ghost :: FallbackItemPath is  :: " + filteredItem.getArticlePagePath());
                    } else {
                        ghostPropertiesResource = null;
                        filteredItem = addFeaturedFilteredItem(filteredItem, pageProperties, singleItem, ghostPropertiesResource, dateFormat);
                        LOGGER.debug("ContentFilterServiceImpl :: fetchFallbackItems() :: FallbackItemPath is  :: " + filteredItem.getArticlePagePath());
                    }
                } else {
                    LOGGER.debug("Handle Acccordingly");
                }
            }
            return filteredItem;
        } catch (Exception e) {
            LOGGER.error("Exception in : ContentFilterServiceImpl :: fetchFallbackItems() Method :: Exception is {}", e);
        } finally {
            if (null != resourceResolver && resourceResolver.isLive())
                resourceResolver.close();
        }
        return filteredItem;
    }

    /*
     *Method for adding filtered hero tile item
     *
     */
    @Override
    public <T extends Item> T addFilteredHeroItem(T filteredItem, ValueMap pageProperties, T singleItem, String dateFormat) {
        try {
            String pageTitle = !pageProperties.get(MadisonConstants.PWC_PAGE_TITLE, StringUtils.EMPTY).isEmpty() ? pageProperties.get(MadisonConstants.PWC_PAGE_TITLE, StringUtils.EMPTY) : pageProperties.get(JcrConstants.JCR_TITLE, StringUtils.EMPTY);
            String contentId = pageProperties.get(DITAConstants.META_CONTENT_ID, StringUtils.EMPTY);
            String standardSetter = pageProperties.get(DITAConstants.META_STANDARD_SETTERS, StringUtils.EMPTY);
            String contentType = pageProperties.get(DITAConstants.META_CONTENT_TYPE, StringUtils.EMPTY);
            
            Resource pageResource = resourceResolver.getResource(singleItem.getArticlePagePath());
            Page page = pageResource.adaptTo(Page.class);
            final boolean hidePublicationDate = DITAUtils.isHidePublicationDate(page.getContentResource()).equals(MadisonConstants.YES) ? true : false;

            domain = madisonDomainsService.getDefaultDomain();
            String publicationDate = pageProperties.get(MadisonConstants.PWC_PUBLICATION_DATE, String.class);
            String revisedDate = pageProperties.get(MadisonConstants.PWC_REVISED_DATE, String.class);
            filteredItem.setTopicText(pageProperties.get(JcrConstants.JCR_DESCRIPTION, StringUtils.EMPTY));
            filteredItem.setTopicTitle(pageTitle);

            filteredItem.setContentId(contentId);
            filteredItem.setContentType(contentType);
            filteredItem.setStandardSetterType(standardSetter);
            filteredItem.setContentFieldValue(getContentFieldValue(contentId, standardSetter, Item.getPwcSourceValue(), contentType));
            filteredItem.setCountry(MadisonUtil.getTerritoryCodeFromPagePath(singleItem.getArticlePagePath()).toUpperCase());
            filteredItem.setArticlePagePath(singleItem.getArticlePagePath() + EXT_HTML);
            filteredItem.setPath(singleItem.getPath());
            filteredItem.setAccessType(singleItem.getAccessType());
            filteredItem.setLicenseTypes(singleItem.getLicenseTypes());
            filteredItem.setRenditionStyle(singleItem.getRenditionStyle());
            filteredItem.setImagePath(singleItem.getImagePath());
            filteredItem.setCtaLabel(singleItem.getCtaLabel());
            filteredItem.setUrlInternal(HomePageUtil.isInternalUrl(singleItem.getArticlePagePath(), domain));
            filteredItem.setUnformattedPublicationDate(DITAUtils.formatDate(publicationDate, MadisonConstants.HOMEPAGE_COMPONENTS_DATE_FORMAT));
            filteredItem.setHidePublicationDate(hidePublicationDate);
            if(!filteredItem.isHidePublicationDate())
            	filteredItem.setPublicationDate(DITAUtils.formatDate(publicationDate, dateFormat));
            filteredItem.setRevisedDate(DITAUtils.formatDate(revisedDate, dateFormat));
            if(null != singleItem.getImagePath()) {
				filteredItem.setImagePath(singleItem.getImagePath());
            }
        } catch (Exception e) {
            LOGGER.error("HeroTileServiceImpl :: addFilteredHeroItem() ::" + e);
        }
        return filteredItem;
    }


    /**
     * Method Used for adding the item to list
     */
    @Override
    public <T extends Item> T addFilteredItem(T filteredItem, ValueMap pageProperties, T singleItem, String dateFormat) {
        try {
            String pageTitle = !pageProperties.get(MadisonConstants.PWC_PAGE_TITLE, StringUtils.EMPTY).isEmpty() ? pageProperties.get(MadisonConstants.PWC_PAGE_TITLE, StringUtils.EMPTY) : pageProperties.get(JcrConstants.JCR_TITLE, StringUtils.EMPTY);
            String contentId = pageProperties.get(DITAConstants.META_CONTENT_ID, StringUtils.EMPTY);
            String standardSetter = pageProperties.get(DITAConstants.META_STANDARD_SETTERS, StringUtils.EMPTY);
            String contentType = pageProperties.get(DITAConstants.META_CONTENT_TYPE, StringUtils.EMPTY);
            String abstractDesc = pageProperties.get(JcrConstants.JCR_DESCRIPTION, StringUtils.EMPTY);
            Resource pageResource = resourceResolver.getResource(singleItem.getArticlePagePath());
            Page page = pageResource.adaptTo(Page.class);
            final boolean hidePublicationDate = DITAUtils.isHidePublicationDate(page.getContentResource()).equals(MadisonConstants.YES) ? true : false;
            domain = madisonDomainsService.getDefaultDomain();
            String publicationDate = pageProperties.get(MadisonConstants.PWC_PUBLICATION_DATE, String.class);
            String revisedDate = pageProperties.get(MadisonConstants.PWC_REVISED_DATE, String.class);
            filteredItem.setTopicTitle(pageTitle);
            filteredItem.setAbstractDesc(abstractDesc);
            filteredItem.setNoOfViews(singleItem.getNoOfViews());
            filteredItem.setArticlePagePath(singleItem.getArticlePagePath() + EXT_HTML);
            filteredItem.setPath(singleItem.getPath());
            filteredItem.setAccessType(singleItem.getAccessType());
            filteredItem.setLicenseTypes(singleItem.getLicenseTypes());
            filteredItem.setOpenInNewWindow(singleItem.getOpenInNewWindow());
            filteredItem.setContentId(contentId);
            filteredItem.setContentType(contentType);
            filteredItem.setContentFieldValue(getContentFieldValue(contentId, standardSetter, Item.getPwcSourceValue(), contentType));
            filteredItem.setCountry(MadisonUtil.getTerritoryCodeFromPagePath(singleItem.getArticlePagePath()).toUpperCase());
            filteredItem.setStandardSetterType(standardSetter);
            filteredItem.setUrlInternal(HomePageUtil.isInternalUrl(singleItem.getArticlePagePath(), domain));
            filteredItem.setUnformattedPublicationDate(DITAUtils.formatDate(publicationDate, MadisonConstants.HOMEPAGE_COMPONENTS_DATE_FORMAT));
            filteredItem.setHidePublicationDate(hidePublicationDate);
            if(!filteredItem.isHidePublicationDate())
            	filteredItem.setPublicationDate(DITAUtils.formatDate(publicationDate, dateFormat));
            filteredItem.setRevisedDate(DITAUtils.formatDate(revisedDate, dateFormat));
            if(null != singleItem.getImagePath()) {
				filteredItem.setImagePath(singleItem.getImagePath());
            }
        } catch (Exception err) {
            LOGGER.error("Error in addFilteredItem() method {} ", err);
        }
        return filteredItem;
    }

    /**
     * Method Used for adding the item to featured List
     */
    @Override
    public <T extends Item> T addFeaturedFilteredItem(T filteredItem, ValueMap pageProperties, T singleItem, Resource ghostPropertiesResource, String dateFormat) {
        try {
            ValueMap resourceValueMap = null;
            if (ghostPropertiesResource != null)
                resourceValueMap = ghostPropertiesResource.getValueMap();

            domain = madisonDomainsService.getDefaultDomain();

            String contentId = pageProperties.get(DITAConstants.META_CONTENT_ID, StringUtils.EMPTY);
            String standardSetter = pageProperties.get(DITAConstants.META_STANDARD_SETTERS, StringUtils.EMPTY);
            String contentType = pageProperties.get(DITAConstants.META_CONTENT_TYPE, StringUtils.EMPTY);
            String revisedDate = pageProperties.get(MadisonConstants.PWC_REVISED_DATE, String.class);
            
            Resource pageResource = resourceResolver.getResource(singleItem.getArticlePagePath());
            Page page = pageResource.adaptTo(Page.class);
            final boolean hidePublicationDate = DITAUtils.isHidePublicationDate(page.getContentResource()).equals(MadisonConstants.YES) ? true : false;

            String pageTitle = resourceValueMap != null ? resourceValueMap.get(MadisonConstants.TOPIC_LABEL, String.class)
                    : (pageProperties.get(MadisonConstants.PWC_PAGE_TITLE, StringUtils.EMPTY).isEmpty()
                    ? pageProperties.get(JcrConstants.JCR_TITLE, String.class)
                    : pageProperties.get(MadisonConstants.PWC_PAGE_TITLE, String.class));

            String abstractDesc = resourceValueMap != null ? resourceValueMap.get(MadisonConstants.TOPIC_TEXT, String.class)
                    : pageProperties.get(JcrConstants.JCR_DESCRIPTION, StringUtils.EMPTY);

            String[] featureSummary = resourceValueMap != null ? resourceValueMap.get(MadisonConstants.SUMMARY_TEXT, EMPTY_ARRAY)
                    : pageProperties.get(DITAConstants.META_DISABLE_FEATURE_SUMMARY, EMPTY_ARRAY);

            String publicationDate = resourceValueMap != null
                    ? resourceValueMap.get(MadisonConstants.GHOST_PUBLICATION_DATE, String.class)
                    : pageProperties.get(MadisonConstants.PWC_PUBLICATION_DATE, String.class);

            String articlePagePath = resourceValueMap != null ? resourceValueMap.get(MadisonConstants.LINK_URL, String.class)
                    : singleItem.getArticlePagePath() + EXT_HTML;
            
            String linkText = resourceValueMap != null ? resourceValueMap.get(MadisonConstants.LINK_LABEL, String.class) : "";

            filteredItem.setTopicTitle(pageTitle);
            filteredItem.setSummaryText(Arrays.asList(featureSummary));
            filteredItem.setAbstractDesc(abstractDesc);
            filteredItem.setArticlePagePath(articlePagePath);
            filteredItem.setRenditionStyle(singleItem.getRenditionStyle());
            filteredItem.setImagePath(singleItem.getImagePath());
            filteredItem.setAccessType(singleItem.getAccessType());
            filteredItem.setLicenseTypes(singleItem.getLicenseTypes());
            filteredItem.setLinkText(linkText);
            filteredItem.setPath(singleItem.getArticlePagePath());
            filteredItem.setStandardSetterType(standardSetter);
            filteredItem.setContentFieldValue(getContentFieldValue(contentId, standardSetter, Item.getPwcSourceValue(), contentType));
            filteredItem.setContentId(contentId);
            filteredItem.setContentType(contentType);
            filteredItem.setCountry(MadisonUtil.getTerritoryCodeFromPagePath(singleItem.getArticlePagePath()).toUpperCase());
            filteredItem.setUnformattedPublicationDate(DITAUtils.formatDate(publicationDate, MadisonConstants.HOMEPAGE_COMPONENTS_DATE_FORMAT));
            filteredItem.setHidePublicationDate(hidePublicationDate);
            if(!filteredItem.isHidePublicationDate())
            	filteredItem.setPublicationDate(DITAUtils.formatDate(publicationDate, dateFormat));
            filteredItem.setRevisedDate(DITAUtils.formatDate(revisedDate, dateFormat));
            if(null != singleItem.getImagePath()) {
				filteredItem.setImagePath(singleItem.getImagePath());
            }

        } catch (Exception err) {
            LOGGER.error("Error in addFeaturedFilteredItem() method {} ", err);
        }
        return filteredItem;
    }


    /**
     * Method Used for matching user's preference with page Tag
     */

    public boolean matchingUserPreferences(ValueMap pageProperties, UserProfile userProfile) {
        try {
            List<String> userTags = new ArrayList<String>();
            List<String> userIndustryTags = userProfile.getIndustryTags();
            List<String> userTopicTags = userProfile.getTopicTags();
            List<String> userTitleTags = userProfile.getTitleTags();

            if (!userProfile.getIndustryTags().isEmpty())
                userIndustryTags.forEach(tag -> userTags.add(tag));

            if (!userProfile.getTopicTags().isEmpty())
                userTopicTags.forEach(tag -> userTags.add(tag));

            if (!userProfile.getTitleTags().isEmpty())
                userTitleTags.forEach(tag -> userTags.add(tag));

            String[] pageCqTags = (String[]) pageProperties.get(DITAConstants.META_TAGS);

            if (userTags == null || userTags.isEmpty())
                return true;

            if (pageCqTags != null) {
                for (String CqTag : pageCqTags) {
                    LOGGER.debug("ContentFilterServiceImpl :: matchingUserPreferences() :: industryTag ::" + CqTag);
                    if (userTags.contains(CqTag)) {
                        LOGGER.info("ContentFilterServiceImpl :: matchingUserPreferences() :: Match occured, it is valid content");
                        return true;
                    } else {
                        LOGGER.debug("ContentFilterServiceImpl :: matchingUserPreferences() :: Match didnt occur");
                    }
                }
            }
        } catch (Exception err) {
            LOGGER.error("Error in matchingUserPreferences() method {} ", err);
        }
        return false;
    }

    /**
     * Method used to take decision for contentField Value
     *
     * @return contentFieldValue {@link String}
     */
    public String getContentFieldValue(String contentId, String standardSetter, String PwcAuthored, String contentType) {
        String contentFieldValue = StringUtils.EMPTY;
        if (contentId.isEmpty()) {
            if (standardSetter.equalsIgnoreCase(PwcAuthored))
                contentFieldValue = contentType;
            else
                contentFieldValue = standardSetter;
        } else
            contentFieldValue = contentId;
        return contentFieldValue;
    }
}
