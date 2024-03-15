/*
 * Bean class for populating multi field items. 
 */
package com.pwc.madison.core.models;

import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import javax.annotation.PostConstruct;

@Model(adaptables = { Resource.class })
public class FooterLink {
	@ValueMapValue
	private String linkTitle;

	@ValueMapValue
	private String linkPath;

	@ValueMapValue
	@Default(values = StringUtils.EMPTY)
	private String openInNewWindow;

	public String getLinkTitle() {
		return linkTitle;
	}

	public String getLinkPath() {
		return linkPath;
	}

	public String getOpenInNewWindow() {
		return openInNewWindow;
	}
	@PostConstruct
	protected void init(){
		linkPath += MadisonUtil.isLinkInternal(linkPath) ? MadisonConstants.HTML_EXTN : "";
	}
}
