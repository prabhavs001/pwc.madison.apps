package com.pwc.madison.core.schedulers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.InsertOnRepublishSchedulerConfig;
import com.pwc.madison.core.services.RecentlyViewedService;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Scheduler to insert pages from recently 
 * viewed database after republish 
 * 
 * */
@Component(immediate = true, service = InsertOnRepublishScheduler.class)
@Designate(ocd = InsertOnRepublishSchedulerConfig.class)
public class InsertOnRepublishScheduler implements Runnable{
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	@Reference
	private Scheduler scheduler;

	@Reference
	QueryBuilder queryBuilder;

	@Reference
	private transient ResourceResolverFactory resourceResolverFactory;

	@Reference 
	RecentlyViewedService recentlyViewedService;

	private int schedulerID;
	private int hoursAgo ;
	private int queryLimit = 0;
	private int staticLimit;
	private int offset = 0;
	private long totalCount;
	private Boolean serverResponseStatus = true;
	private Session session;
	private ResourceResolver resourceResolver;

	//	CONSTANTS DEFINITIONS START
	private static final String TEMPLATE_1 = "/conf/pwc-madison/settings/wcm/templates/dita-content-template";
	private static final String TEMPLATE_2 = "/conf/pwc-madison/settings/wcm/templates/content-page";
	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm'Z'";
	private static final String TIME_ZONE = "UTC";

	//	CONSTANTS DEFINITIONS END

	@Activate
	protected void activate(InsertOnRepublishSchedulerConfig config) {
		LOGGER.debug("InsertOnRepublishScheduler :: activate() :: activate method called");
		removeScheduler();
		schedulerID = config.schedulerName().hashCode();
		addScheduler(config);
		hoursAgo  = -config.schedularFrequency();
		staticLimit = config.queryLimit();
		LOGGER.debug(hoursAgo + " :: hours ago");
	}

	@Deactivate
	protected void deactivate() {
		if (session != null) {
			session.logout();
		}
	}
	/**
	 * Remove a scheduler based on the scheduler ID
	 */
	private void removeScheduler() {
		LOGGER.debug("InsertOnRepublishScheduler :: removeScheduler() :: Removing Scheduler Job '{}'", schedulerID);
		scheduler.unschedule(String.valueOf(schedulerID));
	}

	/**
	 * Add a scheduler based on the scheduler ID
	 */
	private void addScheduler(InsertOnRepublishSchedulerConfig config) {
		LOGGER.debug("InsertOnRepublishScheduler :: addScheduler :: inside add scheduler Method");
		if (config.serviceEnabled()) {
			ScheduleOptions sopts = scheduler.EXPR(config.schedulerExpression());
			sopts.name(String.valueOf(schedulerID));
			sopts.canRunConcurrently(config.schedulerConcurrent());
			scheduler.schedule(this, sopts);
			LOGGER.debug("InsertOnRepublishScheduler added succesfully");
		} else {
			LOGGER.debug("InsertOnRepublishScheduler is Disabled, no scheduler job created");
		}
	}

	@Override
	public void run() {
		resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
				MadisonConstants.MADISON_READ_SUB_SERVICE);
		session = resourceResolver.adaptTo(Session.class);
		offset = 0;
		totalCount = 0;
		queryLimit = staticLimit;
		LOGGER.debug("value of queryLimit is :: "+ queryLimit + "value of totalcount is ::"+  totalCount +"value of offset is :: "+ offset+ "value of staticLimit = "+ staticLimit);
		insertRepublishedPagesInDatabase();
	}

	private void insertRepublishedPagesInDatabase() {
		try {
			if(serverResponseStatus) {
				SearchResult queryResults = getResults(queryLimit, offset);
				totalCount = queryResults.getTotalMatches();
				if(queryLimit < totalCount) {
					offset = queryLimit;
					queryLimit = staticLimit + offset;
					insertRepublishedPagesInDatabase();
				}
			}else {
				LOGGER.debug("InsertOnRepublishScheduler :: insertRepublishedPagesInDatabase() :: UMS server is not responding");
			}
		}catch(Exception ex) {
			LOGGER.error("InsertOnRepublishSchedular :: insertRepublishedPagesIntoDatabase() :: Exception is ", ex); 
		}
	}

	/**
	 * Method to Get SearchResult 
	 * after querying from AEM using Querybuilder
	 *
	 * @return {@link SearchResult}
	 */
	private SearchResult getResults(int limit, int offset) {
		List<String> republishedLinks = new ArrayList<String>();
		SearchResult result = null;
		try {

			if (session != null) {
				final Map<String, String> params = new HashMap<String, String>();

				params.put(DITAConstants.PROPERTY_TYPE, MadisonConstants.CQ_PAGE);
				params.put(DITAConstants.PATH_PROP_NAME, MadisonConstants.PWC_MADISON_CONTENT_BASEPATH);
				params.put("daterange.property", JcrConstants.JCR_CONTENT
						+ MadisonConstants.FORWARD_SLASH+DITAConstants.META_LAST_REPLICATED_DATE);
				params.put("daterange.upperBound",currentDate());
				params.put("daterange.lowerBound",lowerBoundDate(hoursAgo));
				params.put("group.p.or", MadisonConstants.TRUE_TEXT);
				params.put("group.1_property", JcrConstants.JCR_CONTENT
						+ MadisonConstants.FORWARD_SLASH+DITAConstants.META_TEMPLATE_TYPE);
				params.put("group.1_property.value", TEMPLATE_1);
				params.put("group.2_property", JcrConstants.JCR_CONTENT
						+ MadisonConstants.FORWARD_SLASH+DITAConstants.META_TEMPLATE_TYPE);
				params.put("group.2_property.value", TEMPLATE_2);
				params.put("p.limit", Integer.toString(limit));
				params.put("p.offset", Integer.toString(offset));

				final Query query = queryBuilder.createQuery(PredicateGroup.create(params), session);
				result = query.getResult();

				for(final Hit hit : result.getHits()) {
					if(hit.getPath().contains(" ")) {
						republishedLinks.add(hit.getPath().replaceAll(" ", "%20"));
					}else {
						republishedLinks.add(hit.getPath());
					}
				}
				serverResponseStatus = recentlyViewedService.insertRecentlyViewedItemInTempTable(republishedLinks);
				LOGGER.debug("InsertOnRepublishScheduler :: QUERY IS :: "+params);
				LOGGER.debug("InsertOnRepublishScheduler :: links to be inserted from database :: " + republishedLinks.toString());
			}
		}catch(Exception e) {
			LOGGER.error("exception in  insertRepublishedPages() :: insertOnRepublishScheduler",e);
		}
		return result;
	}


	/**
	 * Method to Get Current Date
	 *
	 * @return {@link String}
	 */
	private String currentDate() {
		final TimeZone tz = TimeZone.getTimeZone(TIME_ZONE);
		final DateFormat df = new SimpleDateFormat(DATE_FORMAT);
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
	private String lowerBoundDate(final Integer hours) {
		final Date dateBeforeHours = DateUtils.addHours(new Date(), hours);
		final TimeZone tz = TimeZone.getTimeZone(TIME_ZONE);
		final DateFormat df = new SimpleDateFormat(DATE_FORMAT);
		df.setTimeZone(tz);
		final String nowAsISO = df.format(dateBeforeHours);
		LOGGER.debug(nowAsISO);
		return nowAsISO;
	}

}
