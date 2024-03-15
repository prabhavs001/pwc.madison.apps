package com.pwc.madison.core.services;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration file for InsertOnRepublishScheduler
 * 
 * @author Divanshu 
 * 
 */
@ObjectClassDefinition(name = "PwC Viewpoint Insert on Republish content configuration", description = "Insert on Republish content configuration")
public @interface InsertOnRepublishSchedulerConfig {
/**
 * schedulerName
 * 
 * @return String name
 */
 @AttributeDefinition(name = "Scheduler name", description = "Scheduler name", type = AttributeType.STRING)
 public String schedulerName() default "Insert on Republish content Scheduler";	
 
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
 @AttributeDefinition(name = "Concurrent task",
         description = "Whether or not to schedule this task concurrently")
 boolean schedulerConcurrent() default false;
 
 /**
  * schedulerExpression
  * 
  * @return schedulerExpression
  */
  @AttributeDefinition(name = "Expression", description = "Cron-job expression. Default: run once a day", type = AttributeType.STRING)
  public String schedulerExpression() default "0 1 * * *";
  
  /**
   * schedularFrequency
   * 
   * @return schedularFrequency
   * */
  @AttributeDefinition(name = "schedular frequency", description = "Schedular frequency in hours before", type = AttributeType.INTEGER)
  int schedularFrequency() default 24;
 
  /**
   * queryLimit
   * 
   * @return queryLimit
   * */
  @AttributeDefinition(name = "Query Limit", description = "Number of results to be processed", type = AttributeType.INTEGER)
  int queryLimit() default 500;
  
	
}
