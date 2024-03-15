package com.pwc.madison.core.models;

import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.authorization.enums.AccessLevel;
import com.pwc.madison.core.authorization.models.ContentAuthorization;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;

/**
 * LinkField represents a link which contains label, url and option to open the link in new tab.
 * <br/><br/>
 * Any resource having below properties can be adapted to a link: <br/>
 * linkLabel - link label, mandatory <br/>
 * linkUrl - link url, mandatory <br/>
 * newWindow - option to open the link in new window, optional (represents target attribute value of an anchor link, eg: '_blank') <br/><br/>
 * <p>
 * Note:  {@value MadisonConstants#HTML_EXTN} will be appended to linkUrl
 */
@Model(adaptables = Resource.class)
public class LinkField {

    @ValueMapValue
    private String linkLabel;

    @ValueMapValue
    private String linkUrl;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String newWindow;
    
    /** ResourceResolver reference. */
    @SlingObject
    private ResourceResolver resourceResolver;
    
    private String accessType;
	private String licenseTypes;
	private final String LICENSE_SEPRATOR=", ";

    /**
     * Appends {@value MadisonConstants#HTML_EXTN} to the linkUrl.
     */
    @PostConstruct
    protected void init() {
        
		if (MadisonUtil.isLinkInternal(linkUrl)) {
			Resource requestPageResource = resourceResolver.resolve(linkUrl);

			if (requestPageResource != null) {
				Page page = requestPageResource.adaptTo(Page.class);
				if (Objects.nonNull(page)) {
					final ContentAuthorization contentAuthorization = MadisonUtil.getPageContentAuthorization(page);

					accessType = contentAuthorization.getAccessLevel();

					if (AccessLevel.LICENSED.getValue().equals(contentAuthorization.getAccessLevel())) {
						StringBuilder liceses = new StringBuilder();
						final String[] contentLicenses = contentAuthorization.getLicenses();
						List<String> contentLicensesList = Arrays.asList(contentLicenses);
						for (final String license : contentLicensesList) {
							liceses.append(license + LICENSE_SEPRATOR);
						}
						licenseTypes = StringUtils.substringBeforeLast(liceses.toString(), LICENSE_SEPRATOR);
					}
				}
			}
			linkUrl += MadisonConstants.HTML_EXTN;
		}
    }

    /**
     * @return link label
     */
    public String getLinkLabel() {
        return linkLabel;
    }

    /**
     * @return link url
     */
    public String getLinkUrl() {
        return linkUrl;
    }

    /**
     * @return _blank when link will open in new tab, "" otherwise
     */
    public String getNewWindow() {
        return newWindow;
    }

	public String getAccessType() {
		return accessType;
	}

	public String getLicenseTypes() {
		return licenseTypes;
	}
    
}
