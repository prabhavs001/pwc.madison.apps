package com.pwc.madison.core.schedulers;

import java.io.PrintWriter;

import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
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

import com.pwc.madison.core.schedulers.RecordsXmlGeneratorScheduler.RecordsXmlGeneratorSchedulerConfig;
import com.pwc.madison.core.services.ExplicitRecordsGenerationService;
import com.pwc.madison.core.services.RecordsGenerationService;

/**
 *
 * Records XML Generation Scheduler. The scheduler creates records XML file for Viewpoint site paths.
 *
 */
@Component(immediate = true, service = { RecordsXmlGeneratorScheduler.class, ExplicitRecordsGenerationService.class })
@Designate(ocd = RecordsXmlGeneratorSchedulerConfig.class)
public class RecordsXmlGeneratorScheduler implements Runnable, ExplicitRecordsGenerationService {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Reference
    private Scheduler scheduler;

    @Reference
    private RecordsGenerationService recordsGenerationService;

    private int schedulerID;
    private String recordsFileName;

    @Activate
    protected void activate(RecordsXmlGeneratorSchedulerConfig recordsXmlGeneratorSchedulerConfig) {
        removeScheduler();
        schedulerID = recordsXmlGeneratorSchedulerConfig.scheduler_name().hashCode();
        recordsFileName = recordsXmlGeneratorSchedulerConfig.records_file_name();
        LOGGER.debug("RecordsXmlGeneratorScheduler : activate() : Scheduler ID {}", schedulerID);
        LOGGER.debug("RecordsXmlGeneratorScheduler : activate() : Records File name {}", recordsFileName);
        LOGGER.debug("RecordsXmlGeneratorScheduler : activate() : Schedule task concurrently {}",
                recordsXmlGeneratorSchedulerConfig.scheduler_concurrent());
        LOGGER.debug("RecordsXmlGeneratorScheduler : activate() : Cron Expression {}",
                recordsXmlGeneratorSchedulerConfig.scheduler_expression());
        addScheduler(recordsXmlGeneratorSchedulerConfig);
    }

    @Deactivate
    protected void deactivate(RecordsXmlGeneratorSchedulerConfig recordsXmlGeneratorSchedulerConfig) {
        removeScheduler();
    }

    @ObjectClassDefinition(
            name = "PwC Viewpoint Records XML Generation Scheduler Configuration",
            description = "Configuration allows to define the time interval after which a records xml creation starts")
    public @interface RecordsXmlGeneratorSchedulerConfig {

        @AttributeDefinition(name = "Scheduler name", description = "Scheduler name", type = AttributeType.STRING)
        public String scheduler_name() default "Records XML Generation Scheduler";

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
                name = "Records file name(without extension)",
                description = "Records file map with which the file should be created",
                type = AttributeType.STRING)
        public String records_file_name() default "records";

    }

    private void removeScheduler() {
        LOGGER.info("RecordsXmlGeneratorSchedulerConfig :  removeScheduler() : Removing Scheduler Job {}", schedulerID);
        scheduler.unschedule(String.valueOf(schedulerID));
    }

    private void addScheduler(RecordsXmlGeneratorSchedulerConfig recordsXmlGeneratorSchedulerConfig) {
        if (recordsXmlGeneratorSchedulerConfig.service_enabled()) {
            ScheduleOptions scheduleOptions = scheduler.EXPR(recordsXmlGeneratorSchedulerConfig.scheduler_expression());
            scheduleOptions.name(String.valueOf(schedulerID));
            scheduleOptions.canRunConcurrently(recordsXmlGeneratorSchedulerConfig.scheduler_concurrent());
            scheduler.schedule(this, scheduleOptions);
            LOGGER.info("RecordsXmlGeneratorScheduler :  addScheduler() : Scheduler added {}", scheduleOptions);
        } else {
            LOGGER.info("RecordsXmlGeneratorScheduler :  addScheduler() : Scheduler is Disabled");
        }
    }

    @Override
    public void run() {
        LOGGER.info("RecordsXmlGeneratorScheduler :  run() : Records generation started at time {}",
                java.time.LocalDate.now() + " " + java.time.LocalTime.now());
        generateRecordsXml(null);
    }

    @Override
    public void generateRecordsXml(final PrintWriter printWriter) {
        LOGGER.info("RecordsXmlGeneratorScheduler :  generateRecordsXml() : Records generation started at time {}",
                java.time.LocalDate.now() + " " + java.time.LocalTime.now());
        long startTime = System.currentTimeMillis();
        recordsGenerationService.generateRecordsXml(recordsFileName, printWriter);
        if (printWriter != null) {
            printWriter.println(
                    "RecordsXmlGeneratorScheduler :  generateRecordsXml() :Time Taken by Records XML Creation in seconds "
                            + (System.currentTimeMillis() - startTime) / 1000);
        }
        LOGGER.info(
                "RecordsXmlGeneratorScheduler :  generateRecordsXml() :Time Taken by Records XML Creation in seconds : {}",
                (System.currentTimeMillis() - startTime) / 1000);
    }

}
