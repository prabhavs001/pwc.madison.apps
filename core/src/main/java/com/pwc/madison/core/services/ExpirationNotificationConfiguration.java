package com.pwc.madison.core.services;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "PwC Viewpoint Expiration Notification Configuration", description = "Expiration Notification Configuration")
public @interface ExpirationNotificationConfiguration {

	@AttributeDefinition(name = "Inbox Title", description = "Title of Inbox Notification")
	String inbox_title() default "PwC Madison Content has been Approved or Rejected for Expiry";

	@AttributeDefinition(name = "Inbox Message", description = "Message of Inbox Notification")
	String inbox_message() default "DITA Expiring in 24 hours and not yet approved";

	@AttributeDefinition(name = "DITA Expiration Report Link", description = "Link of DITA Expiration Report")
	String inbox_content_link();
}
