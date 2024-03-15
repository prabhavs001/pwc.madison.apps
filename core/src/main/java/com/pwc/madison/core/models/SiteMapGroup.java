/*
 * Bean class for populating multi field items.
 */
package com.pwc.madison.core.models;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.pwc.madison.core.constants.MadisonConstants;

@Model(adaptables = { Resource.class }, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class SiteMapGroup {
    
    @ValueMapValue
    private String groupText;

    @ValueMapValue
    @Default(values = "#")
    private String groupPath;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String useColumn;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String stackOn;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String openGroupNewWindow;
    
    @ValueMapValue
    @Default(values = {MadisonConstants.INTERNAL_USER})
    private String[] userType;

    private List<SiteMapLink> multiFieldSitemapLinksBeanList;

    public String getGroupText() {
        return groupText;
    }

    public String[] getUserType() {
        return userType;
    }

    public String getGroupPath() {
        return groupPath;
    }

    public void setGroupPath(String groupPath) {
        this.groupPath = groupPath;
    }

    public String getUseColumn() {
        return useColumn;
    }

    public String getStackOn() {
        return stackOn;
    }

    public String getOpenGroupNewWindow() {
        return openGroupNewWindow;
    }

    public List<SiteMapLink> getMultiFieldSitemapLinksBeanList() {
        return multiFieldSitemapLinksBeanList;
    }

    public void setMultiFieldSitemapLinksBeanList(final List<SiteMapLink> multiFieldSitemapLinksBeanList) {
        this.multiFieldSitemapLinksBeanList = multiFieldSitemapLinksBeanList;
    }
}
