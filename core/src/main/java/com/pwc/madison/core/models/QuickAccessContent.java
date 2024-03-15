package com.pwc.madison.core.models;

import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import javax.annotation.PostConstruct;

/**
 * Quick Access Component's composite multifield content model represents a link which contains link text, url and option
 * to open the link in new tab.
 **/
@Model(adaptables = {Resource.class})
public class QuickAccessContent {

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String subsectionText;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String contentUrl;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String newWindow;

    /**
     * @return link text
     */
    public String getSubsectionText() {
        return subsectionText;
    }

    /**
     * @return link url
     */
    public String getContentUrl() {
        return contentUrl;
    }

    /**
     * @return _blank when link will open in new tab, "" otherwise
     */
    public String getNewWindow() {
        return newWindow;
    }

    @PostConstruct
    protected void init() {
        contentUrl += MadisonUtil.isLinkInternal(contentUrl) ? MadisonConstants.HTML_EXTN : "";
    }

}
