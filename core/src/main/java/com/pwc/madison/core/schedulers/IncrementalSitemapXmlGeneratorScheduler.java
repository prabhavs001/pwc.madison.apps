package com.pwc.madison.core.schedulers;

import com.pwc.madison.core.services.ExplicitSitemapGenerationService;
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

import com.pwc.madison.core.schedulers.IncrementalSitemapXmlGeneratorScheduler.IncrementalSitemapXmlGeneratorSchedulerConfig;
import com.pwc.madison.core.services.SitemapGenerationService;

import java.io.PrintWriter;

/**
 *
 * Incremental Sitemap XML Generation Scheduler. The scheduler creates incremental sitemap XML file for viewpoint site
 * language roots. It considers the last replication time of the page to decide whether a page should be included in XML
 * file or not. Thus, only pages activated in last configured replication minutes are present in sitemap XML file.
 *
 */
@Component(immediate = true, service = {IncrementalSitemapXmlGeneratorScheduler.class, ExplicitSitemapGenerationService.class})
@Designate(ocd = IncrementalSitemapXmlGeneratorSchedulerConfig.class)
public class IncrementalSitemapXmlGeneratorScheduler implements Runnable,ExplicitSitemapGenerationService {

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
    private int totalSitemapFiles;
    private int urlsPerSitemapFile;
    private int urlsPerLanguage;

    @Activate
    @Modified
    protected void activate(
            IncrementalSitemapXmlGeneratorSchedulerConfig incrementalSitemapXmlGeneratorSchedulerConfig) {
        removeScheduler();
        schedulerID = incrementalSitemapXmlGeneratorSchedulerConfig.scheduler_name().hashCode();
        sitemapFileName = incrementalSitemapXmlGeneratorSchedulerConfig.sitemap_file_name();
        replicationMinutes = incrementalSitemapXmlGeneratorSchedulerConfig.replication_minutes();
        totalSitemapFiles = incrementalSitemapXmlGeneratorSchedulerConfig.sitemap_file_number();
        urlsPerSitemapFile = incrementalSitemapXmlGeneratorSchedulerConfig.sitemap_url_number();
        urlsPerLanguage = incrementalSitemapXmlGeneratorSchedulerConfig.sitemap_url_number_per_language();
        LOGGER.debug("IncrementalSitemapXmlGeneratorScheduler : activate() : Scheduler ID {}", schedulerID);
        LOGGER.debug("IncrementalSitemapXmlGeneratorScheduler : activate() : Sitemap File name {}", sitemapFileName);
        LOGGER.debug("IncrementalSitemapXmlGeneratorScheduler : activate() : Relication Minutes {}",
                replicationMinutes);
        LOGGER.debug("IncrementalSitemapXmlGeneratorScheduler : activate() : Schedule task concurrently {}",
                incrementalSitemapXmlGeneratorSchedulerConfig.scheduler_concurrent());
        LOGGER.debug("IncrementalSitemapXmlGeneratorScheduler : activate() : Cron Expression {}",
                incrementalSitemapXmlGeneratorSchedulerConfig.scheduler_expression());
        LOGGER.debug("IncrementalSitemapXmlGeneratorScheduler : activate() : Number of Sitemap files per locale {}",
                totalSitemapFiles);
        LOGGER.debug("IncrementalSitemapXmlGeneratorScheduler : activate() : Number of urls per sitemap file {}",
                urlsPerSitemapFile);
        LOGGER.debug("IncrementalSitemapXmlGeneratorScheduler : activate() : Number of urls per language {}",
                urlsPerSitemapFile);
        addScheduler(incrementalSitemapXmlGeneratorSchedulerConfig);
    }

    @Deactivate
    protected void deactivate(
            IncrementalSitemapXmlGeneratorSchedulerConfig incrementalSitemapXmlGeneratorSchedulerConfig) {
        removeScheduler();
    }
    @ObjectClassDefinition(
            name = "PwC Viewpoint Incremental Sitemap XML Generation Scheduler Configuration",
            description = "Configuration allows to define the time interval after which a incremental sitemap xml creation starts")
    public @interface IncrementalSitemapXmlGeneratorSchedulerConfig {

        @AttributeDefinition(name = "Scheduler name", description = "Scheduler name", type = AttributeType.STRING)
        public String scheduler_name() default "Incremental Sitemap XML Generation Scheduler";

        @AttributeDefinition(
                name = "Concurrent",
                description = "Schedule task concurrently",
                type = AttributeType.BOOLEAN)
        boolean scheduler_concurrent() default true;

        @AttributeDefinition(name = "Enabled", description = "Enable Scheduler", type = AttributeType.BOOLEAN)
        boolean service_enabled() default true;

        @AttributeDefinition(
                name = "Expression",
                description = "Cron-job expression. Default: run every four hours.",
                type = AttributeType.STRING)
        String scheduler_expression() default "0 0/4 * * * ? *";

        @AttributeDefinition(
                name = "Incremental Sitemap file name(without extension)",
                description = "Incremental Sitemap file map with which the file should be created",
                type = AttributeType.STRING)
        public String sitemap_file_name() default "sitemap_incremental";

        @AttributeDefinition(
                name = "Last Relicated Pages Elapsed Minutes",
                description = "Minutes to be consided from time scheduler runs within which the pages got activated. Only these pages are to be included in incremental sitemap file.",
                type = AttributeType.INTEGER)
        public int replication_minutes() default 240;

        @AttributeDefinition(
                name = "Number of Sitemap files per locale",
                description = "Number of Sitemap files per locale",
                type = AttributeType.INTEGER)
        public int sitemap_file_number() default 5;

        @AttributeDefinition(
                name = "Number of URL entries per sitemap file",
                description = "Number of URL entries per sitemap file",
                type = AttributeType.INTEGER)
        public int sitemap_url_number() default 5000;

        @AttributeDefinition(
                name = "Number of URL entries per language",
                description = "Number of URL entries per sitemap file",
                type = AttributeType.INTEGER)
        public int sitemap_url_number_per_language() default 10000;

    }

    private void removeScheduler() {
        LOGGER.info("IncrementalSitemapXmlGeneratorSchedulerConfig :  removeScheduler() : Removing Scheduler Job {}",
                schedulerID);
        scheduler.unschedule(String.valueOf(schedulerID));
    }

    private void addScheduler(
            IncrementalSitemapXmlGeneratorSchedulerConfig incrementalSitemapXmlGeneratorSchedulerConfig) {
        if (incrementalSitemapXmlGeneratorSchedulerConfig.service_enabled()) {
            ScheduleOptions scheduleOptions = scheduler
                    .EXPR(incrementalSitemapXmlGeneratorSchedulerConfig.scheduler_expression());
            scheduleOptions.name(String.valueOf(schedulerID));
            scheduleOptions.canRunConcurrently(incrementalSitemapXmlGeneratorSchedulerConfig.scheduler_concurrent());
            scheduler.schedule(this, scheduleOptions);
            LOGGER.info("IncrementalSitemapXmlGeneratorSchedulerConfig :  addScheduler() : Scheduler added {}",
                    scheduleOptions);
        } else {
            LOGGER.info("IncrementalSitemapXmlGeneratorSchedulerConfig :  addScheduler( : Scheduler is Disabled");
        }
    }

    @Override
    public void run() {
        LOGGER.info(
                "IncrementalSitemapXmlGeneratorScheduler :  run() Running Scheduler with Scheduler ID {} at time {}",
                schedulerID, java.time.LocalDate.now() + " " + java.time.LocalTime.now());
        long startTime = System.currentTimeMillis();
        sitemapGenerationService.generateSitemapXml(sitemapFileName, totalSitemapFiles, urlsPerSitemapFile,
                replicationMinutes, urlsPerLanguage,null,null);
        LOGGER.info("IncrementalSitemapXmlGeneratorScheduler :  run() Time Taken by Incremental Sitemap Creation in seconds : {}", (System.currentTimeMillis() - startTime)/1000);
    }
    @Override
    public void generateExplicitSitemap(String territory, PrintWriter printWriter) {
        String territoryVal= !territory.equals("") ?territory:"all";
        LOGGER.info("IncrementalSitemapGeneration :sitemap generation for {} territory started at time {}", xssapi.encodeForHTML(territoryVal), java.time.LocalDate.now() + " " + java.time.LocalTime.now());
        if(printWriter!=null) {
            printWriter.println(xssapi.encodeForHTML("IncrementalSitemapGeneration :sitemap generation for "+territoryVal+" territory started at time {}" + java.time.LocalDate.now() + " " + java.time.LocalTime.now()));
        }
        long startTime = System.currentTimeMillis();
        sitemapGenerationService.generateSitemapXml(sitemapFileName, totalSitemapFiles, urlsPerSitemapFile,
                replicationMinutes, urlsPerLanguage,territory,printWriter);
        if(printWriter!=null) {
            printWriter.println(xssapi.encodeForHTML("IncrementalSitemapGeneration :Time Taken by Incremental Sitemap Creation for "+territoryVal+" territory in seconds : {}" +(System.currentTimeMillis() - startTime) / 1000));
        }
        LOGGER.info("IncrementalSitemapGeneration : Time Taken by Incremental Sitemap Creation for {} territory in seconds : {}", xssapi.encodeForHTML(territoryVal), (System.currentTimeMillis() - startTime)/1000);
    }

}
