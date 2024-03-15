package com.pwc.madison.core.models;

import java.util.List;

public interface SiteMap {

    public static final String FREE_EXT_USER = "free";
    
    public static final String PREMIUM_EXT_USER = "premium";
    
    
    /***
     * @return sitemap label.
     */
    
    public String getSiteMapLabel();
    
    /***
     * @return a list of MultiFieldBean for Reference Links.
     */
    
    public List<List<SiteMapGroup>> getSiteMapItems();

}
