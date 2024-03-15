package com.pwc.madison.core.schedulers;

import com.pwc.madison.core.services.ExplicitSitemapGenerationService;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.schedulers.SitemapXmlGeneratorScheduler.SitemapXmlGeneratorSchedulerConfig;
import com.pwc.madison.core.services.SitemapGenerationService;

import java.io.PrintWriter;

/**
 *
 * Sitemap XML Generation Scheduler. The scheduler creates sitemap XML file for Viewpoint site language roots.
 *
 */
@Component(immediate = true, service = {SitemapXmlGeneratorScheduler.class, ExplicitSitemapGenerationService.class })
@Designate(ocd = SitemapXmlGeneratorSchedulerConfig.class)
public class SitemapXmlGeneratorScheduler implements Runnable,ExplicitSitemapGenerationService {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Reference
    private Scheduler scheduler;

    @Reference
    private SitemapGenerationService sitemapGenerationService;
    
    @Reference
    private XSSAPI xssapi;

    private int schedulerID;
    private String sitemapFileName;
    private int totalSitemapFiles;
    private int urlsPerSitemapFile;

    @Activate
    protected void activate(SitemapXmlGeneratorSchedulerConfig sitemapXmlGeneratorSchedulerConfig) {
        removeScheduler();
        schedulerID = sitemapXmlGeneratorSchedulerConfig.scheduler_name().hashCode();
        sitemapFileName = sitemapXmlGeneratorSchedulerConfig.sitemap_file_name();
        totalSitemapFiles = sitemapXmlGeneratorSchedulerConfig.sitemap_file_number();
        urlsPerSitemapFile = sitemapXmlGeneratorSchedulerConfig.sitemap_url_number();
        LOGGER.debug("SitemapXmlGeneratorScheduler : activate() : Scheduler ID {}", schedulerID);
        LOGGER.debug("SitemapXmlGeneratorScheduler : activate() : Sitemap File name {}", sitemapFileName);
        LOGGER.debug("SitemapXmlGeneratorScheduler : activate() : Schedule task concurrently {}",
                sitemapXmlGeneratorSchedulerConfig.scheduler_concurrent());
        LOGGER.debug("SitemapXmlGeneratorScheduler : activate() : Cron Expression {}",
                sitemapXmlGeneratorSchedulerConfig.scheduler_expression());
        LOGGER.debug("SitemapXmlGeneratorScheduler : activate() : Number of Sitemap files per locale {}",
                totalSitemapFiles);
        LOGGER.debug("SitemapXmlGeneratorScheduler : activate() : Number of urls per sitemap file {}",
                urlsPerSitemapFile);
        addScheduler(sitemapXmlGeneratorSchedulerConfig);
    }

    @Deactivate
    protected void deactivate(SitemapXmlGeneratorSchedulerConfig sitemapXmlGeneratorSchedulerConfig) {
        removeScheduler();
    }



    @ObjectClassDefinition(
            name = "PwC Viewpoint Sitemap XML Generation Scheduler Configuration",
            description = "Configuration allows to define the time interval after which a sitemap xml creation starts")
    public @interface SitemapXmlGeneratorSchedulerConfig {

        @AttributeDefinition(name = "Scheduler name", description = "Scheduler name", type = AttributeType.STRING)
        public String scheduler_name() default "Sitemap XML Generation Scheduler";

        @AttributeDefinition(
                name = "Concurrent",
                description = "Schedule task concurrently",
                type = AttributeType.BOOLEAN)
        boolean scheduler_concurrent() default true;

        @AttributeDefinition(name = "Enabled", description = "Enable Scheduler", type = AttributeType.BOOLEAN)
        boolean service_enabled() default true;

        @AttributeDefinition(
                name = "Expression",
                description = "Cron-job expression. Default: run every day at 12 am.",
                type = AttributeType.STRING)
        String scheduler_expression() default "0 0 0 * * ?";

        @AttributeDefinition(
                name = "Sitemap file name(without extension)",
                description = "Sitemap file map with which the file should be created",
                type = AttributeType.STRING)
        public String sitemap_file_name() default "sitemap";

        @AttributeDefinition(
                name = "Number of Sitemap files per locale",
                description = "Number of Sitemap files per locale",
                type = AttributeType.INTEGER)
        public int sitemap_file_number() default 25;

        @AttributeDefinition(
                name = "Number of URL entries per sitemap file",
                description = "Number of URL entries per sitemap file",
                type = AttributeType.INTEGER)
        public int sitemap_url_number() default 10000;

    }

    private void removeScheduler() {
        LOGGER.info("SitemapXmlGeneratorSchedulerConfig :  removeScheduler() : Removing Scheduler Job {}", schedulerID);
        scheduler.unschedule(String.valueOf(schedulerID));
    }

    private void addScheduler(SitemapXmlGeneratorSchedulerConfig sitemapXmlGeneratorSchedulerConfig) {
        if (sitemapXmlGeneratorSchedulerConfig.service_enabled()) {
            ScheduleOptions scheduleOptions = scheduler.EXPR(sitemapXmlGeneratorSchedulerConfig.scheduler_expression());
            scheduleOptions.name(String.valueOf(schedulerID));
            scheduleOptions.canRunConcurrently(sitemapXmlGeneratorSchedulerConfig.scheduler_concurrent());
            scheduler.schedule(this, scheduleOptions);
            LOGGER.info("SitemapXmlGeneratorScheduler :  addScheduler() : Scheduler added {}", scheduleOptions);
        } else {
            LOGGER.info("SitemapXmlGeneratorScheduler :  addScheduler( : Scheduler is Disabled");
        }
    }

    @Override
    public void run() {
        LOGGER.info("SitemapXmlGeneratorScheduler :  run() Running Scheduler with Scheduler ID {} at time {}",
                schedulerID, java.time.LocalDate.now() + " " + java.time.LocalTime.now());
        long startTime = System.currentTimeMillis();
        sitemapGenerationService.generateSitemapXml(sitemapFileName, totalSitemapFiles, urlsPerSitemapFile, null, null,null,null);
        LOGGER.info("SitemapXmlGeneratorScheduler :  run() Time Taken by Full Sitemap Creation in seconds : {}", (System.currentTimeMillis() - startTime)/1000);

    }
    @Override
    public void generateExplicitSitemap(String territory, PrintWriter printWriter) {
        String territoryVal= !territory.equals("") ?territory:"all";
        LOGGER.info("FullSitemapGeneration :sitemap generation for {} territory started at time {}", xssapi.encodeForHTML(territoryVal), java.time.LocalDate.now() + " " + java.time.LocalTime.now());
        if(printWriter!=null) {
            printWriter.println(xssapi.encodeForHTML("FullSitemapGeneration :sitemap generation for "+territoryVal+" territory started at time {}" + java.time.LocalDate.now() + " " + java.time.LocalTime.now()));
        }
        long startTime = System.currentTimeMillis();
        sitemapGenerationService.generateSitemapXml(sitemapFileName, totalSitemapFiles, urlsPerSitemapFile, null, null,territory,printWriter);
        if(printWriter!=null) {
            printWriter.println(xssapi.encodeForHTML("FullSitemapGeneration :Time Taken by Full Sitemap Creation  for "+territoryVal+" territory in seconds : {}" + (System.currentTimeMillis() - startTime) / 1000));
        }
        LOGGER.info("FullSitemapGeneration : Time Taken by Full Sitemap Creation for {} territory in seconds : {}", xssapi.encodeForHTML(territoryVal), (System.currentTimeMillis() - startTime)/1000);
    }

}
