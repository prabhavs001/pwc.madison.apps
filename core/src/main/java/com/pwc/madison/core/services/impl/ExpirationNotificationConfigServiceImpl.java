package com.pwc.madison.core.services.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.services.ExpirationNotificationConfigService;
import com.pwc.madison.core.services.ExpirationNotificationConfiguration;

@Component(service = ExpirationNotificationConfigService.class, immediate = true)
@Designate(ocd = ExpirationNotificationConfiguration.class)

public class ExpirationNotificationConfigServiceImpl implements ExpirationNotificationConfigService {

	private String inboxTitle;

	private String inboxMessage;

	private String ditaExpirationReportLink;

	@Activate
	@Modified
	public void activate(final ExpirationNotificationConfiguration config) {
		inboxTitle = config.inbox_title();
		inboxMessage = config.inbox_message();
		ditaExpirationReportLink = config.inbox_content_link();
	}

	@Override
	public String getInboxTitle() {
		return inboxTitle;
	}

	@Override
	public String getInboxMessage() {
		return inboxMessage;
	}

	@Override
	public String getDitaExpirationReportLink() {
		return ditaExpirationReportLink;
	}

}
