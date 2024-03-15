package com.pwc.madison.core.services;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "PwC Viewpoint Replication Purge configuration",
    description = "Replication Purge configuration")
public @interface ReplicationPurgeSchedulerConfig {

    /**
     * schedulerName
     *
     * @return String name
     */
    @AttributeDefinition(name = "Scheduler name", description = "Scheduler name", type = AttributeType.STRING)
    public String schedulerName() default "Replication Purge Scheduler";

    /**
     * serviceEnabled
     *
     * @return serviceEnabled
     */
    @AttributeDefinition(name = "Enabled", description = "Enable Scheduler", type = AttributeType.BOOLEAN)
    boolean serviceEnabled() default true;

    /**
     * schedulerConcurrent
     *
     *
     * @return schedulerConcurrent
     */
    @AttributeDefinition(name = "Concurrent task", description = "Whether or not to schedule this task concurrently")
    boolean schedulerConcurrent() default false;

    /**
     * schedulerExpression
     *
     * @return schedulerExpression
     */
    @AttributeDefinition(
        name = "Expression",
        description = "Cron-job expression. Default: run once 0n every firday day at 23:00 hrs",
        type = AttributeType.STRING)
    public String schedulerExpression() default "0 0 23 ? * FRI *";

    /**
     * dateLimit
     *
     * @return dateLimit
     */
    @AttributeDefinition(
        name = "Delete Package Before Days",
        description = "Package to be deleted before Current Date",
        type = AttributeType.INTEGER)
    int dateLimit() default 7;

}
