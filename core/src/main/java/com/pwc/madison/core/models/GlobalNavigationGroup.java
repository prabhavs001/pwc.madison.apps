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
public class GlobalNavigationGroup {
    
    @ValueMapValue
    private String groupText;

    @ValueMapValue
    @Default(values = "#")
    private String groupPath;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String openGroupNewWindow;
    
    @ValueMapValue
    @Default(values = {MadisonConstants.INTERNAL_USER})
    private String[] userType;

    private List<GlobalNavigationLink> multiFieldGlobalNavigationLinksBeanList;
    

    public String getGroupText() {
        return groupText;
    }

    public String getGroupPath() {
        return groupPath;
    }
    
    public String[] getUserType() {
        return userType;
    }

    public void setGroupPath(String groupPath) {
        this.groupPath = groupPath;
    }

    public String getOpenGroupNewWindow() {
        return openGroupNewWindow;
    }

    public List<GlobalNavigationLink> getMultiFieldGlobalNavigationLinksBeanList() {
        return multiFieldGlobalNavigationLinksBeanList;
    }

    public void setMultiFieldGlobalNavigationLinksBeanList(
            List<GlobalNavigationLink> multiFieldGlobalNavigationLinksBeanList) {
        this.multiFieldGlobalNavigationLinksBeanList = multiFieldGlobalNavigationLinksBeanList;
    }
}
