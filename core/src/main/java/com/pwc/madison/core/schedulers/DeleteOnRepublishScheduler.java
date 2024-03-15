package com.pwc.madison.core.schedulers;

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

import com.pwc.madison.core.services.DeleteOnRepublishSchedulerConfig;
import com.pwc.madison.core.services.RecentlyViewedService;

/**
 * Scheduler to delete pages from recently 
 * viewed temp database after republish 
 * 
 * */

@Component(immediate = true, service = DeleteOnRepublishScheduler.class)
@Designate(ocd = DeleteOnRepublishSchedulerConfig.class)
public class DeleteOnRepublishScheduler implements Runnable{

	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	
	@Reference
	private Scheduler scheduler;
	
	@Reference 
	RecentlyViewedService recentlyViewedService;
	
	private int schedulerID;
	
	@Activate
	protected void activate(DeleteOnRepublishSchedulerConfig config) {
		LOGGER.debug("DeleteOnRepublishScheduler :: activate :: inside activate method");
		schedulerID = config.schedulerName().hashCode();
		removeScheduler();
		schedulerID = config.schedulerName().hashCode(); // update schedulerID
		addScheduler(config);
	}

	@Deactivate
	protected void deactivate() {
		LOGGER.debug("DeleteOnRepublishScheduler :: deactivate :: inside activate method");
	}
	
	/**
	 * Add a scheduler based on the scheduler ID
	 */
	private void addScheduler(DeleteOnRepublishSchedulerConfig config) {
		LOGGER.debug("DeleteOnRepublishScheduler :: addScheduler :: inside add scheduler Method");
		if (config.serviceEnabled()) {
			ScheduleOptions sopts = scheduler.EXPR(config.schedulerExpression());
			sopts.name(String.valueOf(schedulerID));
			sopts.canRunConcurrently(config.schedulerConcurrent());
			scheduler.schedule(this, sopts);
			LOGGER.debug("DeleteOnRepublishScheduler added succesfully");
		} else {
			LOGGER.debug("DeleteOnRepublishScheduler is Disabled, no scheduler job created");
		}
	}
	
	/**
	 * Remove a scheduler based on the scheduler ID
	 */
	private void removeScheduler() {
		LOGGER.debug("DeleteOnRepublishScheduler :: removeScheduler :: Removing Scheduler Job '{}'", schedulerID);
		scheduler.unschedule(String.valueOf(schedulerID));
	}
	
	/** 
	 * Run Method that runs when cron is satisfied 
	 * 
	 * @return void 
	 * */
	@Override
	public void run() {
		LOGGER.debug("DeleteOnRepublishScheduler :: run :: Inside run method");
		if(recentlyViewedService.removeRecentlyViewedItemFromTempTable()) {
			LOGGER.debug("DeleteOnRepublishScheduler :: run :: deleted successfully");
		}else {
			LOGGER.debug("DeleteOnRepublishScheduler :: run :: Issue contacting UMS ");
		}
		
	}

}
