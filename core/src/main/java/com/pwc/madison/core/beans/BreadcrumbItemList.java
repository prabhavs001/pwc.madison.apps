package com.pwc.madison.core.beans;

import java.util.List;

/**
 * Bean used to collect list of BreadcrumbItem object
 */
public class BreadcrumbItemList {
    
    private List<BreadcrumbItem> items;

    public List<BreadcrumbItem> getBreadcrumbItemList() {
        return items;
    }

    public void setBreadcrumbItemList(List<BreadcrumbItem> items) {
        this.items = items;
    }

}
