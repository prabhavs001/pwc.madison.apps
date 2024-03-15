package com.pwc.madison.core.services;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "PwC Viewpoint Auto-Publish Scheduler configuration",
    description = "This configuration is used for auto-publish scheduler.")
public @interface AutoPublishDitamapSchedulerConfig {

    /**
     * schedulerName
     *
     * @return String name
     */
    @AttributeDefinition(name = "Scheduler name", description = "Scheduler name", type = AttributeType.STRING)
    public String schedulerName() default "Auto-Publish Scheduler";

    /**
     * serviceEnabled
     *
     * @return serviceEnabled
     */
    @AttributeDefinition(name = "Enabled", description = "Enable Scheduler", type = AttributeType.BOOLEAN)
    boolean serviceEnabled() default true;

    /**
     * schedulerExpression
     *
     * @return schedulerExpression
     */
    @AttributeDefinition(
        name = "Expression",
        description = "Cron-job expression. Default: every 5 minutes",
        type = AttributeType.STRING)
    String schedulerExpression() default "*/5 * * * *";

}
