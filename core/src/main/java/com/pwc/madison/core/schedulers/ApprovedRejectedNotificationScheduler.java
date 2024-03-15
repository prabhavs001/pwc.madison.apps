package com.pwc.madison.core.schedulers;

import com.adobe.acs.commons.notifications.InboxNotificationSender;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.ApprovedRejectedNotificationConfiguration;
import com.pwc.madison.core.services.ExpirationNotificationConfigService;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.ExpirationNotificationUtil;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
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

import javax.jcr.Session;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduler to notify Publishers every day about the DITA Topics being approved
 * or rejected
 *
 */
@Component(immediate = true, service = ApprovedRejectedNotificationScheduler.class)
@Designate(ocd = ApprovedRejectedNotificationConfiguration.class)
public class ApprovedRejectedNotificationScheduler implements Runnable {

	@Reference
	private Scheduler scheduler;
	private int schedulerID;
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	@Reference
	private QueryBuilder queryBuilder;

	@Reference
	private ExpirationNotificationConfigService expirationNotificationConfig;

	@Reference
	transient InboxNotificationSender inboxNotificationSender;
	
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

	final String EXPIRATION_PUBLISHER_GROUP_SUFFIX = "-ops-publishers";

	@Activate
	protected void activate(ApprovedRejectedNotificationConfiguration config) {
		schedulerID = config.schedulerName().hashCode();
		addScheduler(config);
	}

	@Modified
	protected void modified(ApprovedRejectedNotificationConfiguration config) {
		removeScheduler();
		schedulerID = config.schedulerName().hashCode(); // update schedulerID
		addScheduler(config);
	}

	@Deactivate
	protected void deactivate(ApprovedRejectedNotificationConfiguration config) {
		removeScheduler();
	}

	/**
	 * Remove a scheduler based on the scheduler ID
	 */
	private void removeScheduler() {
		LOGGER.debug("Removing Scheduler Job '{}'", schedulerID);
		scheduler.unschedule(String.valueOf(schedulerID));
	}

	/**
	 * Add a scheduler based on the scheduler ID
	 */
	private void addScheduler(ApprovedRejectedNotificationConfiguration config) {
		if (config.serviceEnabled()) {
			ScheduleOptions sopts = scheduler.EXPR(config.schedulerExpression());
			sopts.name(String.valueOf(schedulerID));
			sopts.canRunConcurrently(false);
			scheduler.schedule(this, sopts);
			LOGGER.error("ApprovedRejectedNotificationScheduler added succesfully");
		} else {
			LOGGER.error("ApprovedRejectedNotificationScheduler is Disabled, no scheduler job created");
		}
	}

	@Override
	public void run() {
		LOGGER.debug("Inside ApprovedRejectedNotificationScheduler run Method");
		sendNotifications();
	}

	/**
	 * Send Inbox and Email Notifications to all publishers of DITA topics
	 */
	private void sendNotifications() {
		Map<Authorizable, List<String>> publishersMap = findPublishers();
		if (null != publishersMap) {
			// send inbox notification to publishers
			ExpirationNotificationUtil.sendInboxNotification(resourceResolverFactory, expirationNotificationConfig,
					inboxNotificationSender, publishersMap, madisonSystemUserNameProviderService.getFmditaServiceUsername());
		}
	}

	private List<Hit> findApprovedRejectedAssets(ResourceResolver resourceResolver) {
		Session session = resourceResolver.adaptTo(Session.class);
		List<Hit> resultList = new ArrayList<Hit>();

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date todaysDate = new Date();
		Date yesterday = DateUtils.addDays(new Date(), -1);

		try {
			final Map<String, String> map = new HashMap<String, String>();
			map.put("type", "dam:Asset");
			map.put("path", MadisonConstants.PWC_MADISON_DITAROOT_DAM_PATH);
			map.put("daterange.property", "jcr:content/metadata/pwc-approvedRejectedDate");
			map.put("daterange.lowerBound", dateFormat.format(yesterday));
			map.put("daterange.lowerOperation", ">");
			map.put("daterange.upperBound", dateFormat.format(todaysDate));
			map.put("daterange.upperOperation", "<=");
			map.put("p.limit", "-1");
			Query query = queryBuilder.createQuery(PredicateGroup.create(map), session);
			SearchResult result = query.getResult();
			resultList = result.getHits();
		} catch (Exception e) {
			LOGGER.error(
					"ApprovedRejectedNotificationScheduler findExpiringAssets() : Exception while finding expiring assets {}",
					e);
		}

		return resultList;
	}

	/**
	 * Find all the publishers who have access to DITA Topics being approved or
	 * rejected for expiration
	 *
	 * @return publishers versus expiring DITA topics map
	 */
	private Map<Authorizable, List<String>> findPublishers() {
		Map<Authorizable, List<String>> expirationPublisherMap = new HashMap<Authorizable, List<String>>();
		ResourceResolver resourceResolver = null;
		try{
			resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
					madisonSystemUserNameProviderService.getFmditaServiceUsername());
			List<Hit> approvedOrRejectedAssets = findApprovedRejectedAssets(resourceResolver);
			if (null != approvedOrRejectedAssets) {
				expirationPublisherMap = ExpirationNotificationUtil.findExpirationGroups(resourceResolverFactory,
						approvedOrRejectedAssets, EXPIRATION_PUBLISHER_GROUP_SUFFIX, madisonSystemUserNameProviderService.getFmditaServiceUsername());
			}
		}catch (Exception e){
			LOGGER.error("Error while getting publishers: ", e);
		}finally {
			if(null != resourceResolver){
				resourceResolver.close();
			}
		}
		return expirationPublisherMap;
	}

}
