package com.pwc.madison.core.models;

import java.util.List;

public interface GlobalNavigation {
    
    /***
     * @return a list of MultiFieldBean for Reference Links.
     */
    public List<GlobalNavigationGroup> getGlobalNavigationItems();
    
    /**
     * Returns the analytics component's name.
     * 
     * @return {@link String}
     */
    public String getComponentName();

}
