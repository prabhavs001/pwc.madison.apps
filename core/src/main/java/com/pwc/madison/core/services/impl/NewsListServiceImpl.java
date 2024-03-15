package com.pwc.madison.core.services.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.authorization.enums.AudienceType;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.NewsListItem;
import com.pwc.madison.core.services.MadisonDomainsService;
import com.pwc.madison.core.services.NewsListService;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.HomePageUtil;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * News List Service Implementation containing all the filtration methods,
 * Search Query for the News List component.
 * 
 */
@Component(immediate = true, service = { NewsListService.class }, enabled = true)
public class NewsListServiceImpl implements NewsListService {

	private static final Logger LOGGER = LoggerFactory.getLogger(NewsListServiceImpl.class);

	private static final String PWC_CONTENT_TYPE_VAL = "ContentType_News_FW";
	private static final String CQ_PAGE = "cq:Page";
	private static final String PWC_TOPIC_BODY = "/jcr:content/root/maincontainer/readerrow/bodycontainer/docreader/contentbody/topicbody/pwc-topic";
	private static final String SHORT_DESCRIPTION_NODE_PATH = PWC_TOPIC_BODY+"/shortdesc/_text";
	private static final String PAGE_LIMIT = "10";
	private static final String OFFSET = "0";
	private static final String ORDER_BY = "desc";
	private static final String SYMB_AT = "@";
	private static final String FALSE_TEXT = "false";
	private static final String EXISTS = "exists";
	private static final String TEXT = "text";
	private static final int LOWER_LIMIT = 2;

	private ResourceResolver resourceResolver;
	private SimpleDateFormat dateFormatter = new SimpleDateFormat(MadisonConstants.HOMEPAGE_COMPONENTS_DATE_FORMAT);
	private Session session;
	private Boolean isPublish = true;

	private String domain = StringUtils.EMPTY;

	@Reference
	private transient ResourceResolverFactory resourceResolverFactory;

	@Reference
	private transient MadisonDomainsService madisonDomainsService;

	@Reference
	private QueryBuilder queryBuilder;

	@Reference
	private SlingSettingsService slingSettingsService;

	@Activate
	protected void activate() {
		isPublish = this.slingSettingsService.getRunModes().contains(MadisonConstants.PUBLISH_INSTANCE);
		LOGGER.debug("NewsListService Activate()");
		LOGGER.debug("mode isPublish ? :: "+isPublish);
	}

	@Override
	public List<NewsListItem> getFilteredList(final List<String> dynamicList, final List<String> fallbackList,
			final UserProfile user, final String dateFormat) throws RepositoryException {
		List<NewsListItem> filteredList = new ArrayList<>();
		final List<String> newsPagePathList = new ArrayList<>();
		final Boolean preferenceFlag = true;
		SearchResult result = null;
		try {
			resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
					MadisonConstants.MADISON_READ_SUB_SERVICE);
			session = resourceResolver.adaptTo(Session.class);
			if(!dynamicList.isEmpty()) {
				for (final String newsItem : dynamicList) {
					newsPagePathList.add(newsItem);
				}
				if(!isPublish) 
					result = getSearchResultSet(newsPagePathList, session);
				else
					result = getSearchResultSet(newsPagePathList, user, preferenceFlag, session);
				if (null != result) {
					for (final Hit hit : result.getHits()) {
						NewsListItem newsItem = new NewsListItem();
						newsItem = populateNewsItem(hit, dateFormat);
						filteredList.add(newsItem);
					}
				}
			}

			if (filteredList.size() < LOWER_LIMIT) {
				filteredList = getFilteredListViaFallbackItems(filteredList, fallbackList, user, session, dateFormat);
				filteredList = getSortedList(filteredList);
			}
			removeDuplicates(filteredList);
		} catch (final Exception e) {
			LOGGER.error("Error in getFilteredList() method of NewsListServiceImpl : {} ", e);
		}finally {
			HomePageUtil.closeResourceResolver(session, resourceResolver);
		}
		return filteredList;
	}

	/**
	 * This method is used to remove all the duplicate items from the final news list.
	 * 
	 * @return {@link Void}
	 */
	private void removeDuplicates(List<NewsListItem> filteredList) {
		Set<NewsListItem> set = new TreeSet<NewsListItem>(new Comparator<NewsListItem>() {
			@Override
			public int compare(NewsListItem item1, NewsListItem item2) {
				if(item1.getNewsPagePath().equalsIgnoreCase(item2.getNewsPagePath())) {
					return 0;
				}
				return 1;
			}
		});
		set.addAll(filteredList);
		filteredList.clear();
		filteredList.addAll(set);
	}

	/**
	 * This method is used to add the remaining items from fallbackList in
	 * filteredList to complete the list for 3 items.
	 * 
	 * @param filteredList
	 * 
	 * @param fallbackList
	 * 
	 * @param user
	 * 
	 * @param session
	 * 
	 * @param dateFormat
	 * 
	 * @return {@link List<NewsListItem>} returns null, if there is no item in the
	 *         filteredList
	 */
	private List<NewsListItem> getFilteredListViaFallbackItems(List<NewsListItem> filteredList,
			final List<String> fallbackList, final UserProfile user, final Session session, final String dateFormat) {
		final List<String> fallbackPagePathList = new ArrayList<>();
		final Boolean preferenceFlag = false;
		try {
			int itemsLeft = LOWER_LIMIT - filteredList.size();
			LOGGER.debug(
					"Initial Size of the List: " + filteredList.size() + " and Items need to be added: " + itemsLeft);
			for (final String item : fallbackList) {
				fallbackPagePathList.add(item);
			}
			final SearchResult result = getSearchResultSet(fallbackPagePathList, user, preferenceFlag, session);
			if (null != result) {
				for (final Hit hit : result.getHits()) {
					if (itemsLeft < LOWER_LIMIT || filteredList.size() == 0) {
						NewsListItem newsItem = new NewsListItem();
						newsItem = populateNewsItem(hit, dateFormat);
						filteredList.add(newsItem);
						itemsLeft = filteredList.size();
					} else {
						break;
					}
				}
			}
		} catch (final Exception e) {
			LOGGER.error("Error in getFilteredListViaFallbackItems() method of NewsListServiceImpl : {} ", e);
		}
		return filteredList;
	}

	/**
	 * This method is used to Populate NewsListItem Object.
	 * 
	 * @param hit
	 * 		{@link Hit}
	 * @param dateFormat 
	 * 	    {@link String}
	 * @throws RepositoryException
	 * 	
	 * @return
	 * 		{@link NewsListItem}
	 */
	private NewsListItem populateNewsItem(final Hit hit, final String dateFormat) throws RepositoryException {
		NewsListItem newsItem = null;
		try {
			final Resource res = resourceResolver.getResource(hit.getPath());
			if (res != null && res instanceof Resource) {
				final Page page = res.adaptTo(Page.class);
				if (page != null & page instanceof Page) {
					newsItem = new NewsListItem();
					newsItem = populateMetaProperties(res);
					final ValueMap properties = page.getProperties();
					final boolean hidePublicationDate = DITAUtils.isHidePublicationDate(page.getContentResource()).equals(MadisonConstants.YES) ? true : false;
					newsItem.setNewsPagePath(MadisonUtil.getUrlForPageResource(page.getPath()));
					newsItem.setHidePublicationDate(hidePublicationDate);
					final String publicationDate = properties.get(DITAConstants.META_PUBLICATION_DATE, String.class);
					if (publicationDate != null) {
						newsItem.setUnformattedPublicationDate(DITAUtils.formatDate(publicationDate, MadisonConstants.HOMEPAGE_COMPONENTS_DATE_FORMAT));
						if (!newsItem.isHidePublicationDate()) {
							newsItem.setDate(DITAUtils.formatDate(publicationDate, dateFormat));
						}
					}
				}
			}
		} catch (final Exception e) {
			LOGGER.error("Error in method populateNewsItem() : NewsListServiceImpl :  {} ", e);
		}
		return newsItem;
	}

	/**
	 * This method is used to populate News Source and link from the Node Hierarchy
	 * Structure.
	 * 
	 * @param resource
	 * 
	 * @return {@link NewsListItem}
	 */
	private NewsListItem populateMetaProperties(Resource resource) {
		final NewsListItem newsItem = new NewsListItem();
		domain = madisonDomainsService.getDefaultDomain();
		try {
			String sourceLink = StringUtils.EMPTY;
			String newsSource = StringUtils.EMPTY;
			String descText = StringUtils.EMPTY;

			final Resource pRes = resourceResolver.getResource(resource.getPath() + MadisonConstants.PWC_P_PATH);
			if (Objects.nonNull(pRes)) {
				final ValueMap pValueMap = pRes.getValueMap();
				if (pValueMap.containsKey(MadisonConstants.FLATTENED_PROPERTY)) {
					final String[] element = MadisonUtil.fetchElementsFromHTML(pValueMap, MadisonConstants.FLATTENED_NEWS);
					sourceLink = element[0];
					newsSource = element[1];
				} else {
					final Resource xrefRes = resourceResolver.getResource(resource.getPath() + MadisonConstants.PWC_XREF_PATH);
					if (Objects.nonNull(xrefRes)) {
						final ValueMap xrefValueMap = xrefRes.getValueMap();
						if (xrefValueMap.containsKey(MadisonConstants.FLATTENED_PROPERTY)) {
							final String[] element = MadisonUtil.fetchElementsFromHTML(xrefValueMap, MadisonConstants.FLATTENED_NEWS);
							sourceLink = element[0];
							newsSource = element[1];
						} else {
							sourceLink = xrefValueMap.get(MadisonConstants.PWC_XREF_LINK_POPERTY, String.class);
							newsSource = xrefValueMap.get(MadisonConstants.PWC_XREF_INFO_TEXT_PROPERTY, String.class);
						}
					}
				}
			}
			final Resource shortDescRes = resourceResolver.getResource(resource.getPath() + SHORT_DESCRIPTION_NODE_PATH);
			final ValueMap shortDescProps = shortDescRes.getValueMap();
			descText = shortDescProps.get(TEXT, String.class);
			newsItem.setNewsTitle(descText);
			newsItem.setNewsSource(newsSource);
			newsItem.setSourceLink(HomePageUtil.getValidLink(sourceLink));
			newsItem.setIsInternalUrl(HomePageUtil.isInternalUrl(sourceLink, domain));

		} catch (final Exception e) {
			LOGGER.error("NewsListServiceImpl :: populateMetaProperties () :: Error :: {} ", e);
		}
		return newsItem;
	}

	/**
	 * This method is used to return the Sorted List according to the latest
	 * Publication Date.
	 * 
	 * @param filteredList
	 * 
	 * @return 
	 * 		{@link List<NewsListItem>}
	 * 
	 */
	private List<NewsListItem> getSortedList(final List<NewsListItem> filteredList) {
		Collections.sort(filteredList, new Comparator<NewsListItem>() {
			@Override
			public int compare(NewsListItem item1, NewsListItem item2) {
				Date date1 = null;
				Date date2 = null;
				try {
					if (item1.getUnformattedPublicationDate() != null && item2.getUnformattedPublicationDate() != null) {
						date1 = dateFormatter.parse(item1.getUnformattedPublicationDate());
						date2 = dateFormatter.parse(item2.getUnformattedPublicationDate());
					}
				} catch (final ParseException e) {
					LOGGER.error("Error in getSortedList() method of NewsListServiceImpl : {} ", e);
				}
				return date2.compareTo(date1);
			}
		});
		return filteredList;
	}

	/**
	 * Method used to return the result when AEM is running in Author mode
	 * without personalization
	 * 
	 *  @param pagePathList
	 *  	{@link List<String>}
	 *  
	 * */
	private SearchResult getSearchResultSet(final List<String> pagePathList, final Session session) {
		SearchResult result = null;
		try {
			if(session != null) {
				final Map<String, String> params = new HashMap<String, String>();
				int count = 0;
				params.put("group.p.or", MadisonConstants.TRUE_TEXT);
				for (final String path : pagePathList) {
					if(path != null && !path.isEmpty()) {
						final int index = ++count;
						String group = "group."+index+"_group.";
						params.put(group.concat("path"), path);
						params.put(group.concat(DITAConstants.PROPERTY_TYPE), CQ_PAGE);
						params.put(group.concat("property"), JcrConstants.JCR_CONTENT + MadisonConstants.FORWARD_SLASH + DITAConstants.META_CONTENT_TYPE);
						params.put(group.concat("property.value"), PWC_CONTENT_TYPE_VAL);
					}
				}
				params.put("orderby", SYMB_AT + JcrConstants.JCR_CONTENT + MadisonConstants.FORWARD_SLASH + DITAConstants.META_PUBLICATION_DATE);
				params.put("p.limit", PAGE_LIMIT);
				LOGGER.debug("Query executed :: {}",params);

				if(!params.isEmpty()) {
					final Query query = queryBuilder.createQuery(PredicateGroup.create(params), session);
					result = query.getResult();
				}
			}
		}catch(Exception e) {
			LOGGER.error("Exception in NewsListServiceImpl :: getResultsOnAuthor :: {}",e);
		}
		return result;
	}
	/**
	 * The method is used to return the Search Result Items according to the given
	 * parameters
	 * 
	 * @param pagePathList
	 * 		{@link List<String>}
	 * 
	 * @param user
	 * 		{@link User}
	 * 
	 * @param preferenceFlag
	 * 		{@link Boolean}
	 * 
	 * @return {@link SearchResult} returns null, if there are no results found.
	 */
	private SearchResult getSearchResultSet(final List<String> pagePathList, final UserProfile user, final Boolean preferenceFlag, final Session session) {
		SearchResult result = null;
		try {
			if (session != null) {				
				final Map<String, String> params = new HashMap<String, String>();
				int count = 0;
				params.put("group.p.or", MadisonConstants.TRUE_TEXT);
				for (final String path : pagePathList) {
					if(path != null && !path.isEmpty()) {
						LOGGER.debug("Page Path :: " + path);
						final int index = ++count;
						params.put("group." + index + "_group.path", path);
						params.put("group." + index + "_group."+DITAConstants.PROPERTY_TYPE, CQ_PAGE);
						if(preferenceFlag) {
							// Nested Group for pwc-audience
							params.put("group." + index + "_group.1_group.p.or", MadisonConstants.TRUE_TEXT);
							params.put("group." + index + "_group.1_group.1_property",
									JcrConstants.JCR_CONTENT + MadisonConstants.FORWARD_SLASH + DITAConstants.META_AUDIENCE);
							params.put("group." + index + "_group.1_group.1_property.value", user != null
									? (user.getIsInternalUser().equals(true) ? AudienceType.INTERNAL_ONLY.getValue() : AudienceType.EXTERNAL_ONLY.getValue())
											: AudienceType.EXTERNAL_ONLY.getValue());
							params.put("group." + index + "_group.1_group.2_property", JcrConstants.JCR_CONTENT + MadisonConstants.FORWARD_SLASH + DITAConstants.META_AUDIENCE);
							params.put("group." + index + "_group.1_group.2_property.value", AudienceType.INTERNAL_AND_EXTERNAL.getValue());
							params.put("group." + index + "_group.1_group.3_property", JcrConstants.JCR_CONTENT + MadisonConstants.FORWARD_SLASH + DITAConstants.META_AUDIENCE);
							params.put("group." + index + "_group.1_group.3_property.operation", EXISTS);
							params.put("group." + index + "_group.1_group.3_property.value", FALSE_TEXT);
						}						
						//	Nested group for pwc-privateGroup 
						if(user != null && user.getIsInternalUser()) {
							LOGGER.debug("Is User internal :: " + user.getIsInternalUser());
							LOGGER.debug("User Groups :: " + user.getContentAccessInfo().getPrivateGroups());
							List<String> privateGroupsList = user.getContentAccessInfo().getPrivateGroups();
							if(privateGroupsList != null) {
								int privateGroupCount = 0;
								params.put("group." + index + "_group.1_group.3_property", JcrConstants.JCR_CONTENT + MadisonConstants.FORWARD_SLASH + DITAConstants.META_PRIVATE_GROUP);
								for(String privateGroup : privateGroupsList) {
									final int privateGroupCountNumber = privateGroupCount++;
									params.put("group." + index + "_group.1_group.3_property." + privateGroupCountNumber + "_value",privateGroup);
								}
							}
						}						
						//	Nested group for user
						if (user != null) {
							List<String> userTags = new ArrayList<>();
							if(!user.getIndustryTags().isEmpty()){
								user.getIndustryTags().forEach(industryTag -> userTags.add(industryTag));
							}
							if(!user.getTitleTags().isEmpty()){
								user.getTitleTags().forEach(titleTag -> userTags.add(titleTag));
							}
							if(!user.getTopicTags().isEmpty()){
								user.getTopicTags().forEach(topicTag -> userTags.add(topicTag));
							}

							if (!userTags.isEmpty() && preferenceFlag) {
								params.put("group." + index + "_group.2_group.tagid.or", MadisonConstants.TRUE_TEXT);
								params.put("group." + index + "_group.2_group.tagid.property",
										JcrConstants.JCR_CONTENT + MadisonConstants.FORWARD_SLASH + DITAConstants.META_TAGS);
								int counter = 0;
								for (final String tag : userTags) {
									final int indexer = ++counter;
									params.put("group." + index + "_group.2_group.tagid." + indexer + "_value", tag);
								}
							}
						}
						//	Nested group for content-type
						params.put("group." + index +"_group.3_group.1_property",
								JcrConstants.JCR_CONTENT + MadisonConstants.FORWARD_SLASH + DITAConstants.META_CONTENT_TYPE);
						params.put("group." + index +"_group.3_group.1_property.value", PWC_CONTENT_TYPE_VAL);
						params.put("orderby",
								SYMB_AT + JcrConstants.JCR_CONTENT + MadisonConstants.FORWARD_SLASH + DITAConstants.META_PUBLICATION_DATE);
					}
				}
				params.put("orderby.sort", ORDER_BY);
				params.put("p.offset", OFFSET);
				params.put("p.limit", PAGE_LIMIT);
				if (null != params.get("group." + 1 + "_group.path") || params.containsKey("group." + 1 + "_group.path")) {
					final Query query = queryBuilder.createQuery(PredicateGroup.create(params), session);
					result = query.getResult();
					LOGGER.debug("Query Executed.");
					LOGGER.debug("Query Params: " + params);
					LOGGER.debug("Total Matches in the Result Set:  " + result.getTotalMatches());
				}
			}
		} catch (final Exception e) {
			LOGGER.error("Error in searchResultQuery() method of NewsListServiceImpl : {}", e);
		}
		return result;
	}


}
