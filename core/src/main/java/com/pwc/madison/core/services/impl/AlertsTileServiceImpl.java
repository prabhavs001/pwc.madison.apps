package com.pwc.madison.core.services.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import org.apache.sling.api.resource.ValueMap;

import javax.jcr.Session;
import javax.servlet.http.Cookie;

import com.pwc.madison.core.userreg.Constants;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.query.SlingQuery;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.xss.XSSAPI;
import org.json.JSONArray;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.pwc.madison.core.authorization.enums.AudienceType;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.Alerts;
import com.pwc.madison.core.services.AlertsTileService;
import com.pwc.madison.core.services.MadisonDomainsService;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.userreg.utils.UserRegUtil;
import com.pwc.madison.core.util.HomePageUtil;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Alert tile Component service Implementation
 *
 * @author Divanshu
 *
 */

@Component(
		service = { AlertsTileService.class },
		immediate = true,
		property = { "service.description=" + "AlertStyleService component service implementation" })

public class AlertsTileServiceImpl implements AlertsTileService {
	/** Default Logger */
	private final Logger LOGGER = LoggerFactory.getLogger(AlertsTileServiceImpl.class);

	private Session session;
	private ResourceResolver resourceResolver;

	@Reference
	QueryBuilder queryBuilder;

	@Reference
	private CryptoSupport cryptoSupport;

	@Reference
	private MadisonDomainsService domainService;

	@Reference
	private transient ResourceResolverFactory resourceResolverFactory;

	@Reference
	private XSSAPI xssAPI;

	@Reference
	private SlingSettingsService slingSettingsService;


	public static String DOMAIN = StringUtils.EMPTY;

	private Boolean isContentAvailable = false;
	private Boolean isPublish = true;
	private static final String PWC_TOPIC_BODY = "/jcr:content/root/maincontainer/readerrow/bodycontainer/docreader/contentbody/topicbody/pwc-topic";
    private static final String PWC_XREF_PATH = PWC_TOPIC_BODY + "/pwc-body/p/pwc-xref";
    private static final String LINK_TEXT_PATH = PWC_XREF_PATH + "/xref-info/_text";
    private static final String TITLE_NODE_PATH = PWC_TOPIC_BODY + "/title/_text";
    private static final String SHORT_DESCRIPTION_NODE_PATH = PWC_TOPIC_BODY + "/shortdesc/_text";


	@Activate
	protected void Activate() {
		LOGGER.debug("AlertsTile Service Activated");
		DOMAIN = domainService.getDefaultDomain();
		isPublish = this.slingSettingsService.getRunModes().contains(MadisonConstants.PUBLISH_INSTANCE);
	}


	@Override
	public Boolean getIsContentAvailable() {
		LOGGER.debug("inside");
		LOGGER.debug("value for iscontentavailable is :: "+ isContentAvailable);
		return isContentAvailable;
	}

	/**
	 * Method to create list after filtering query results that can be used to populate on client side
	 *
	 * @param result
	 *            {@link SearchResult} returned by query builder
	 * @param dismissPagesCk
	 *            {@link Cookie} from request containing user dismissed pages
	 * @param titleList
	 *            {@link List<Alerts>} of alerts
	 *
	 * @return {@link Void}
	 */
	private void populateResult(final SearchResult result, final String dismissPagesCk, final List<Alerts> alertsList) {
		try {
			for (final Hit hit : result.getHits()) {

				final String resultPath = hit.getPath();
				Alerts alertsObj = new Alerts();
				boolean isValidAlertDismissCookie = true;
				boolean isValidReadAlertsCk = true;

				if (dismissPagesCk != null && dismissPagesCk != StringUtils.EMPTY ) {
					if (!dismissPagesCk.contains(resultPath)) {
						isValidAlertDismissCookie = true;
					}else {
						isValidAlertDismissCookie = false;
					}
				}
				if(isValidAlertDismissCookie && isValidReadAlertsCk) {
					alertsObj = getMetaProperties(resultPath);
					alertsList.add(alertsObj);
				}else {
					LOGGER.debug("item not added");
				}

				LOGGER.debug(alertsObj.getTopicText());
			}
		} catch (final Exception e) {
			LOGGER.error("AlertTileServiceImpl :: populateResult() :: Exception in method {}", e);
		}
	}

	@Override
	public void filterAlertsList(final List<String> alertItemsList, final UserProfile userProfile,
			final List<Alerts> alertsList, final String dismissPagesCk) {
		try {
			resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
					MadisonConstants.MADISON_READ_SUB_SERVICE);
			session = resourceResolver.adaptTo(Session.class);
			SearchResult result = null;
			if(isPublish) 
				result= getSearchResults(alertItemsList, userProfile, session);
			else
				result = getSearchResults(alertItemsList, session);
			populateResult(result, dismissPagesCk, alertsList);
		} catch (final Exception e) {
			LOGGER.error("AlertTileServiceImpl :: filterTitleList() :: Exception in method {}", e);
		}finally {
			HomePageUtil.closeResourceResolver(session, resourceResolver);
		}
	}

	/** 
	 * Method to get personalized results on Publish instance 
	 * 
	 * @param alertItemsList
	 *  		{@link List<String>}
	 *  @param userProfile
	 *  		{@link UserProfile}}
	 *  @param sesion
	 *  		{@link Session};
	 * */
	private SearchResult getSearchResults(final List<String> alertItemsList,final UserProfile userProfile, Session session) {
		List<String> tagList = new ArrayList<String>();
		SearchResult result = null;
		try {
			if (session != null) {
				final Map<String, String> params = new HashMap<String, String>();
				int count = 0;
				params.put("group.p.or", MadisonConstants.TRUE_TEXT);
				for (final String var : alertItemsList) {
					final Alerts alert = parseAlerts(var);
					LOGGER.debug("Alerts Component path ::" + alert.getArticlePagePath());
					final String path = alert.getArticlePagePath();
					if (path != null && !path.isEmpty()) {
						LOGGER.debug("The path ::" + path);
						final int index = ++count;
						params.put("group." + index + "_group.path", path);
						params.put("group." + index + "_group." + DITAConstants.PROPERTY_TYPE, MadisonConstants.CQ_PAGE);
						// For date range
						params.put("group." + index + "_group.daterange.property", JcrConstants.JCR_CONTENT
								+ MadisonConstants.FORWARD_SLASH + DITAConstants.META_PUBLICATION_DATE);
						params.put("group." + index + "_group.daterange.upperBound", currentDate());
						params.put("group." + index + "_group.daterange.lowerBound", lowerBoundDate(MadisonConstants.NUMBER_OF_DAYS));

						// Nested Group for pwc-audience
						params.put("group." + index + "_group.1_group.p.or", MadisonConstants.TRUE_TEXT);
						params.put("group." + index + "_group.1_group.1_property", JcrConstants.JCR_CONTENT
								+ MadisonConstants.FORWARD_SLASH + DITAConstants.META_AUDIENCE);
						params.put("group." + index + "_group.1_group.1_property.value", userProfile != null
								? userProfile.getIsInternalUser().equals(true) ? AudienceType.INTERNAL_ONLY.getValue()
										: AudienceType.EXTERNAL_ONLY.getValue()
										: AudienceType.EXTERNAL_ONLY.getValue());
						params.put("group." + index + "_group.1_group.2_property", JcrConstants.JCR_CONTENT
								+ MadisonConstants.FORWARD_SLASH + DITAConstants.META_AUDIENCE);
						params.put("group." + index + "_group.1_group.2_property.value", AudienceType.INTERNAL_AND_EXTERNAL.getValue());

						// Nested group for user
						if (userProfile != null) {

							if(!userProfile.getIndustryTags().isEmpty()) {
								userProfile.getIndustryTags().forEach(industryTag-> tagList.add(industryTag));
							}

							if(!userProfile.getTitleTags().isEmpty()) {
								userProfile.getTitleTags().forEach(titleTag-> tagList.add(titleTag));
							}

							if(!userProfile.getTopicTags().isEmpty()) {
								userProfile.getTopicTags().forEach(topicTag -> tagList.add(topicTag));
							}

							if (!tagList.isEmpty()) {
								params.put("group." + index + "_group.2_group.tagid.or", MadisonConstants.TRUE_TEXT);
								params.put("group." + index + "_group.2_group.tagid.property", JcrConstants.JCR_CONTENT
										+ MadisonConstants.FORWARD_SLASH + DITAConstants.META_TAGS);
								int counter = 0;
								for (final String tag : tagList) {
									final int indexer = ++counter;
									params.put("group." + index + "_group.2_group.tagid." + indexer + "_value", tag);
								}
							}

							// Nested Group for pwc-privateGroup
							if (userProfile.getIsInternalUser()) {
								LOGGER.debug("User Groups :: " + userProfile.getContentAccessInfo().getPrivateGroups());
								final List<String> privateGroupsList = userProfile.getContentAccessInfo().getPrivateGroups();
								if (privateGroupsList != null) {
									int privateGroupCount = 0;
									params.put("group." + index + "_group.1_group.3_property", JcrConstants.JCR_CONTENT
											+ MadisonConstants.FORWARD_SLASH + DITAConstants.META_PRIVATE_GROUP);
									for (final String privateGroup : privateGroupsList) {
										final int privateGroupCountNumber = privateGroupCount++;
										params.put("group." + index + "_group.1_group.3_property."
												+ privateGroupCountNumber + "_value", privateGroup);
									}
								}
							}

						}
						// Nested group for content-type
						params.put("group." + index + "_group.3_group.1_property", JcrConstants.JCR_CONTENT
								+ MadisonConstants.FORWARD_SLASH + DITAConstants.META_CONTENT_TYPE);
						params.put("group." + index + "_group.3_group.1_property.value", MadisonConstants.PWC_CONTENT_TYPE_VAL);
						params.put("orderby", MadisonConstants.SYMB_AT + JcrConstants.JCR_CONTENT + MadisonConstants.FORWARD_SLASH
								+ DITAConstants.META_PUBLICATION_DATE);
					}
				}
				// Sort and limit queries
				params.put("orderby.sort", MadisonConstants.ORDER_BY);
				params.put("p.offset", MadisonConstants.OFFSET);
				params.put("p.limit", MadisonConstants.P_LIMIT);
				if (null != params.get("group." + 1 + "_group.path")
						|| params.containsKey("group." + 1 + "_group.path")) {
					final Query query = queryBuilder.createQuery(PredicateGroup.create(params), session);
					result = query.getResult();
					LOGGER.debug("size of get result is :: "+ result.getHits().size());
					if(result.getHits().size() > 0) {
						LOGGER.debug("inside if for iscontentavailable");
						isContentAvailable = true;
					}else {
						isContentAvailable = false;
					}
					LOGGER.debug("Query Executed.");
					LOGGER.debug("Query Params: " + params);
					LOGGER.debug("Total Matches in the Result Set:  " + result.getTotalMatches());
				}
			}
		}catch(Exception e) {
			LOGGER.error("Exception in AlertTileServiceImpl :: Method is :: getSearchResults() :: Exception is ",e);
		}
		return result;
	}


	/** 
	 * Method to get non personalized results on Author instance
	 * 
	 * @param alertItemsList
	 *  		{@link List<String>}
	 *  @param sesion
	 *  		{@link Session};
	 * */
	private SearchResult getSearchResults(final List<String> alertItemsList, Session session) {
		SearchResult result = null;
		try {
			if (session != null) {
				final Map<String, String> params = new HashMap<String, String>();
				int count = 0;
				params.put("group.p.or", MadisonConstants.TRUE_TEXT);
				for (final String alertItem : alertItemsList) {
					final Alerts alert = parseAlerts(alertItem);
					LOGGER.debug("Alerts Component path ::" + alert.getArticlePagePath());
					final String path = alert.getArticlePagePath();
					if (path != null && !path.isEmpty()) {
						final int index = ++count;
						final String group = "group." + index +"_group.";
						params.put(group.concat("path"), path);
						params.put(group.concat(DITAConstants.PROPERTY_TYPE), MadisonConstants.CQ_PAGE);
						params.put(group.concat("property"), JcrConstants.JCR_CONTENT + MadisonConstants.FORWARD_SLASH + DITAConstants.META_CONTENT_TYPE);
						params.put(group.concat("property.value"),MadisonConstants.PWC_CONTENT_TYPE_VAL);
						params.put(group.concat("daterange.property"), JcrConstants.JCR_CONTENT + MadisonConstants.FORWARD_SLASH + DITAConstants.META_PUBLICATION_DATE);
						params.put(group.concat("daterange.upperBound"), currentDate());
						params.put(group.concat("daterange.lowerbound"), lowerBoundDate(MadisonConstants.NUMBER_OF_DAYS));
					}
				}
				params.put("p.limit", MadisonConstants.P_LIMIT);
				params.put("orderby.sort", MadisonConstants.ORDER_BY);
				params.put("p.offset", MadisonConstants.OFFSET);

				if(!params.isEmpty()) {
					final Query query = queryBuilder.createQuery(PredicateGroup.create(params), session);
					result = query.getResult();
					LOGGER.debug("size of get result is :: "+ result.getHits().size());
					if(result.getHits().size() > 0) {
						LOGGER.debug("inside if for iscontentavailable");
						isContentAvailable = true;
					}else {
						isContentAvailable = false;
					}
				}
			}
		}catch(Exception e) {
			LOGGER.error("Exception in service Implementation for AlertTileServiceImpl :: getSearchResults() :: Exception is :: {}",e);
		}
		return result;
	}

	/**
	 * Method to Get Current Date
	 *
	 * @return {@link String}
	 */
	private String currentDate() {
		final TimeZone tz = TimeZone.getTimeZone("UTC");
		final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		df.setTimeZone(tz);
		final String nowAsISO = df.format(new Date());
		return nowAsISO;
	}

	/**
	 * Method to generate lower Bound Date
	 *
	 * @param days
	 *            {@link Integer} Takes integer days value
	 * @return nowAsISO {@link String} Returns String date in ISO Format
	 */
	private String lowerBoundDate(final Integer days) {
		final Date dateBefore30Days = DateUtils.addDays(new Date(), days);
		final TimeZone tz = TimeZone.getTimeZone("UTC");
		final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		df.setTimeZone(tz);
		final String nowAsISO = df.format(dateBefore30Days);
		return nowAsISO;
	}

	/**
	 * Method to parse JSONString object to gsonObject
	 *
	 * @param alert
	 *            {@link String} Alert object from request
	 * @return gsonAlert {@link Alerts} alert object after parsing json to gson
	 */
	private Alerts parseAlerts(final String alert) {
		Alerts alertObj = new Alerts();
		alertObj.setArticlePagePath(alert);
		return alertObj;
	}

	/**
	 * Method to get MetaProperties from Dita Alert type content having meta properties nested inside the node using
	 * {@link SlingQuery}
	 *
	 *
	 * @param Path
	 *
	 *            Parent path of the alert content type returned from query builder API matching all the filtered
	 *            Criteria having {@link String} as it's data type
	 *
	 * @return
	 *
	 *         A new object {@link Alerts} created after iterating nodes to get properties and Returns Empty
	 *         {@link Alerts} Object if invalid parameters are passed
	 */
	private Alerts getMetaProperties(final String path) {
        final Alerts alertObj = new Alerts();
        String ctaLink = StringUtils.EMPTY;
        String shortDescription = StringUtils.EMPTY;
        String title = StringUtils.EMPTY;
        String ctaLinkText = StringUtils.EMPTY;

        try {

            final Resource shortDescRes = resourceResolver.getResource(path.concat(SHORT_DESCRIPTION_NODE_PATH));
            final Resource titleRes = resourceResolver.getResource(path.concat(TITLE_NODE_PATH));
            final Resource pRes = resourceResolver.getResource(path.concat(MadisonConstants.PWC_P_PATH));

            if (Objects.nonNull(shortDescRes)) {
                ValueMap shortDescriptionProp = shortDescRes.getValueMap();
                if (shortDescriptionProp.containsKey(MadisonConstants.NODE_PROP_TEXT))
                    shortDescription = shortDescriptionProp.get(MadisonConstants.NODE_PROP_TEXT, String.class);
            }
            if (Objects.nonNull(titleRes)) {
                ValueMap shortDescriptionProp = titleRes.getValueMap();
                if (shortDescriptionProp.containsKey(MadisonConstants.NODE_PROP_TEXT))
                    title = shortDescriptionProp.get(MadisonConstants.NODE_PROP_TEXT, String.class);
            }
            
			if (Objects.nonNull(pRes)) {
				final ValueMap pValueMap = pRes.getValueMap();
				if (pValueMap.containsKey(MadisonConstants.FLATTENED_PROPERTY)) {
					final String[] element = MadisonUtil.fetchElementsFromHTML(pValueMap, MadisonConstants.FLATTENED_ALERTS);
					ctaLink = element[0];
					ctaLinkText = element[1];
				} else {
					final Resource xrefRes = resourceResolver.getResource(path.concat(MadisonConstants.PWC_XREF_PATH));
					if (Objects.nonNull(xrefRes)) {
						final ValueMap xrefProps = xrefRes.getValueMap();
						if (xrefProps.containsKey(MadisonConstants.FLATTENED_PROPERTY)) {
							final String[] element = MadisonUtil.fetchElementsFromHTML(xrefProps, MadisonConstants.FLATTENED_ALERTS);
							ctaLink = element[0];
							ctaLinkText = element[1];
						} else {
							ctaLink = xrefProps.get(MadisonConstants.PWC_XREF_LINK_POPERTY, String.class);
							final Resource linkTextRes = resourceResolver.getResource(path.concat(LINK_TEXT_PATH));
							if (Objects.nonNull(linkTextRes)) {
								final ValueMap linkTextValueMap = linkTextRes.getValueMap();
								if (linkTextValueMap.containsKey(MadisonConstants.PWC_XREF_INFO_TEXT_PROPERTY))
									ctaLinkText = linkTextValueMap.get(MadisonConstants.PWC_XREF_INFO_TEXT_PROPERTY, String.class);
							}
						}
					}
				}
			}
			alertObj.setArticlePagePath(path);
			alertObj.setTopicText(shortDescription);
			alertObj.setTopicTitle(title);
			alertObj.setLinkText(ctaLinkText);
			alertObj.setCtaLink(HomePageUtil.getValidLink(ctaLink));
			alertObj.setIsInternalUrl(HomePageUtil.isInternalUrl(ctaLink,DOMAIN));

        } catch (final Exception e) {
			LOGGER.error("AlertTileServiceImpl :: getMetaProperties() :: Exception in method {}", e);
        }
        return alertObj;
    }

	@Override
	public void setPageCookie(final SlingHttpServletResponse response, final int cookieExpiryHours,final String cookieName,final JSONArray cookieValue,
			String refererPage) {
		try {
			if (null != cookieValue) {
				final String encodedCookie = cryptoSupport.protect(cookieValue.toString());
				boolean isHttps = domainService.getDefaultDomain().contains(Constants.HTTPS);
				UserRegUtil.setCookie(response, cookieName, encodedCookie, cookieExpiryHours, isHttps, false, refererPage);
			}
		} catch (final Exception e) {
			LOGGER.error("AlertsTileServiceImpl :: setPageCookie() :: Exception in method {}", e);
		}
	}

	@Override
	public String getAlertsCookie(final SlingHttpServletRequest request, final String cookieName) {
		String decodedCookie = "";
		try {
			final Cookie Cookie = UserRegUtil.getCookieByName(request, cookieName);
			if (Cookie != null) {
				decodedCookie = cryptoSupport.unprotect(Cookie.getValue());
			}
		} catch (final Exception e) {
			LOGGER.error("AlertsTileServiceImpl :: getAlertsCookie :: Exception in method {}", e);
		}
		return decodedCookie;
	}


}
