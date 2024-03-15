package com.pwc.madison.core.schedulers;

import java.io.PrintWriter;

import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.schedulers.DeleteSitemapGeneratorScheduler.DeleteSitemapGeneratorSchedulerConfig;
import com.pwc.madison.core.services.ExplicitSitemapGenerationService;
import com.pwc.madison.core.services.SitemapGenerationService;

/**
 *
 * Delete Sitemap Generation Scheduler. The scheduler creates delete sitemap txt file for viewpoint site language roots.
 * It considers the /var/audit audit logs to decide whether a page should be included in delete file or not. Only pages
 * deleted in last configured minutes are present in delete txt file.
 *
 */
@Component(immediate = true, service = {DeleteSitemapGeneratorScheduler.class, ExplicitSitemapGenerationService.class })
@Designate(ocd = DeleteSitemapGeneratorSchedulerConfig.class)
public class DeleteSitemapGeneratorScheduler implements Runnable,ExplicitSitemapGenerationService {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Reference
    private Scheduler scheduler;

    @Reference
    private SitemapGenerationService sitemapGenerationService;
    
    @Reference
    private XSSAPI xssapi;

    private int schedulerID;
    private String sitemapFileName;
    private int replicationMinutes;
    private int urlsPerLanguage;

    @Activate
    @Modified
    protected void activate(DeleteSitemapGeneratorSchedulerConfig deleteSitemapGeneratorSchedulerConfig) {
        removeScheduler();
        schedulerID = deleteSitemapGeneratorSchedulerConfig.scheduler_name().hashCode();
        sitemapFileName = deleteSitemapGeneratorSchedulerConfig.sitemap_file_name();
        replicationMinutes = deleteSitemapGeneratorSchedulerConfig.replication_minutes();
        urlsPerLanguage = deleteSitemapGeneratorSchedulerConfig.sitemap_url_number_per_language();
        LOGGER.debug("DeleteSitemapGeneratorScheduler : activate() : Scheduler ID {}", schedulerID);
        LOGGER.debug("DeleteSitemapGeneratorScheduler : activate() : Sitemap File name {}", sitemapFileName);
        LOGGER.debug("DeleteSitemapGeneratorScheduler : activate() : Relication Minutes {}", replicationMinutes);
        LOGGER.debug("DeleteSitemapGeneratorScheduler : activate() : Schedule task concurrently {}",
                deleteSitemapGeneratorSchedulerConfig.scheduler_concurrent());
        LOGGER.debug("DeleteSitemapGeneratorScheduler : activate() : Cron Expression {}",
                deleteSitemapGeneratorSchedulerConfig.scheduler_expression());
        LOGGER.debug("DeleteSitemapGeneratorScheduler : activate() : Number of urls per language {}", urlsPerLanguage);
        addScheduler(deleteSitemapGeneratorSchedulerConfig);
    }

    @Deactivate
    protected void deactivate(DeleteSitemapGeneratorSchedulerConfig deleteSitemapGeneratorSchedulerConfig) {
        removeScheduler();
    }

    @ObjectClassDefinition(
            name = "PwC Viewpoint Delete Sitemap Generation Scheduler Configuration",
            description = "Configuration allows to define the time interval after which a delete sitemap creation starts")
    public @interface DeleteSitemapGeneratorSchedulerConfig {

        @AttributeDefinition(name = "Scheduler name", description = "Scheduler name", type = AttributeType.STRING)
        public String scheduler_name() default "Delete Sitemap XML Generation Scheduler";

        @AttributeDefinition(
                name = "Concurrent",
                description = "Schedule task concurrently",
                type = AttributeType.BOOLEAN)
        boolean scheduler_concurrent() default true;

        @AttributeDefinition(name = "Enabled", description = "Enable Scheduler", type = AttributeType.BOOLEAN)
        boolean service_enabled() default true;

        @AttributeDefinition(
                name = "Expression",
                description = "Cron-job expression. Default: run every hour.",
                type = AttributeType.STRING)
        String scheduler_expression() default "0 0/4 * * * ? *";

        @AttributeDefinition(
                name = "Delete Sitemap file name(without extension)",
                description = "Delete Sitemap file map with which the file should be created",
                type = AttributeType.STRING)
        public String sitemap_file_name() default "sitemap_delete";

        @AttributeDefinition(
                name = "Deleted Pages Elapsed Minutes",
                description = "Minutes to be consided from time scheduler runs within which the pages got deleted. Only these pages are to be included in delete sitemap file.",
                type = AttributeType.INTEGER)
        public int replication_minutes() default 240;

        @AttributeDefinition(
                name = "Number of URL entries per language",
                description = "Number of URL entries per sitemap file",
                type = AttributeType.INTEGER)
        public int sitemap_url_number_per_language() default 10000;

    }

    private void removeScheduler() {
        LOGGER.info("DeleteSitemapGeneratorScheduler :  removeScheduler() : Removing Scheduler Job {}", schedulerID);
        scheduler.unschedule(String.valueOf(schedulerID));
    }

    private void addScheduler(DeleteSitemapGeneratorSchedulerConfig deleteSitemapGeneratorSchedulerConfig) {
        if (deleteSitemapGeneratorSchedulerConfig.service_enabled()) {
            ScheduleOptions scheduleOptions = scheduler
                    .EXPR(deleteSitemapGeneratorSchedulerConfig.scheduler_expression());
            scheduleOptions.name(String.valueOf(schedulerID));
            scheduleOptions.canRunConcurrently(deleteSitemapGeneratorSchedulerConfig.scheduler_concurrent());
            scheduler.schedule(this, scheduleOptions);
            LOGGER.info("DeleteSitemapGeneratorScheduler :  addScheduler() : Scheduler added {}", scheduleOptions);
        } else {
            LOGGER.info("DeleteSitemapGeneratorScheduler :  addScheduler( : Scheduler is Disabled");
        }
    }

    @Override
    public void run() {
        LOGGER.info("DeleteSitemapGeneratorScheduler :  run() Running Scheduler with Scheduler ID {} at time {}",
                schedulerID, java.time.LocalDate.now() + " " + java.time.LocalTime.now());
        long startTime = System.currentTimeMillis();
        sitemapGenerationService.generateDeleteSitemap(sitemapFileName, replicationMinutes, urlsPerLanguage,null,null);
        LOGGER.info("DeleteSitemapGeneratorScheduler :  run() Time Taken by Delete Sitemap Creation in seconds : {}", (System.currentTimeMillis() - startTime)/1000);
    }
    @Override
    public void generateExplicitSitemap(String language, PrintWriter printWriter) {
        String territoryVal= !language.equals("") ?language:"all";
        LOGGER.info("DeleteSitemapGeneration :sitemap generation for {} language started at time {}", xssapi.encodeForHTML(territoryVal), java.time.LocalDate.now() + " " + java.time.LocalTime.now());
        if(printWriter!=null) {
            printWriter.println(xssapi.encodeForHTML("DeleteSitemapGeneration :sitemap generation for "+ territoryVal + java.time.LocalDate.now() + " " + java.time.LocalTime.now()));
        }
        long startTime = System.currentTimeMillis();
        sitemapGenerationService.generateDeleteSitemap(sitemapFileName, replicationMinutes, urlsPerLanguage,language,printWriter);
        if(printWriter!=null) {
            printWriter.println(xssapi.encodeForHTML("DeleteSitemapGeneration :Time Taken by Delete Sitemap Creation for "+ territoryVal +" language  in seconds :" + (System.currentTimeMillis() - startTime) / 1000));
        }
        LOGGER.info("DeleteSitemapGeneration : Time Taken by Delete Sitemap Creation for {} language in seconds : {}", xssapi.encodeForHTML(territoryVal), (System.currentTimeMillis() - startTime)/1000);
    }
}
