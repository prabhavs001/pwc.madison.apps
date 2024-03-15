/*
 * Bean class for populating multi field items.
 */
package com.pwc.madison.core.models;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.pwc.madison.core.constants.MadisonConstants;

import java.util.List;

@Model(adaptables = { Resource.class }, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class GlobalNavigationLink {

    @ValueMapValue
    private String addToGroup;

    @ValueMapValue
    private String navigationText;

    @ValueMapValue
    private String navigationURL;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String openLinkNewWindow;
    
    @ValueMapValue
    @Default(values = {MadisonConstants.INTERNAL_USER})
    private String[] userType;

    private List<GlobalNavigationLink> multiFieldGlobalNavigationLinksBeanList;
    
    public String getAddToGroup() {
        return addToGroup;
    }

    public String getNavigationText() {
        return navigationText;
    }

    public String getNavigationURL() {
        return navigationURL;
    }

    public void setNavigationURL(String navigationURL) {
        this.navigationURL = navigationURL;
    }

    public String getOpenLinkNewWindow() {
        return openLinkNewWindow;
    }

    public void setOpenLinkNewWindow(String openLinkNewWindow) {
        this.openLinkNewWindow=openLinkNewWindow;
    }

    public String[] getUserType() {
        return userType;
    }

    public List<GlobalNavigationLink> getMultiFieldGlobalNavigationLinksBeanList() {
        return multiFieldGlobalNavigationLinksBeanList;
    }

    public void setMultiFieldGlobalNavigationLinksBeanList(
            List<GlobalNavigationLink> multiFieldGlobalNavigationLinksBeanList) {
        this.multiFieldGlobalNavigationLinksBeanList = multiFieldGlobalNavigationLinksBeanList;
    }
}
