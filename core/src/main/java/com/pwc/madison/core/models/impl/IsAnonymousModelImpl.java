package com.pwc.madison.core.models.impl;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.Cookie;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.models.IsAnonymousModel;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;

/**
 * Sling Model for checking user anonymous or not
 *
 * @author Divanshu
 *
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = IsAnonymousModel.class)
public class IsAnonymousModelImpl implements IsAnonymousModel{
	/** Default Logger */
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	
	@Inject
	SlingHttpServletRequest request;

	@Inject
	SlingHttpServletResponse response;
	
	Boolean isAnonymousUser = true;
	
	@PostConstruct
	private void init() {
		try {
			final Cookie madisonCookie = UserInformationUtil.getMadisonUserProfileCookie(request);
			if (null != madisonCookie) {
				isAnonymousUser = false;
			}
	}catch(Exception e) {
		LOGGER.error("exception in isAnonymousModelImpl",e);	
	}
	}
	
	@Override
	public Boolean getIsAnonymousUser() {
		return isAnonymousUser;
	}

}
