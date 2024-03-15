package com.pwc.madison.core.services;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration file for ExpirationNotificationScheduler
 * 
 */
@ObjectClassDefinition(name = "PwC Viewpoint Approved/Rejected Notification Scheduler Configuration", description = "Approved/Rejected Notification Scheduler Configuration")
public @interface ApprovedRejectedNotificationConfiguration {
	/**
	 * schedulerName
	 * 
	 * @return String name
	 */
	@AttributeDefinition(name = "Scheduler name", description = "Scheduler name", type = AttributeType.STRING)
	public String schedulerName() default "Expiration Notification Scheduler";

	/**
	 * schedulerConcurrent
	 * 
	 * @return schedulerConcurrent
	 */
	@AttributeDefinition(name = "Concurrent", description = "Schedule task concurrently", type = AttributeType.BOOLEAN)
	boolean schedulerConcurrent() default true;

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
	@AttributeDefinition(name = "Expression", description = "Cron-job expression. Default: run every hour.", type = AttributeType.STRING)
	String schedulerExpression() default "0 0 5 1/1 * ? *";
}