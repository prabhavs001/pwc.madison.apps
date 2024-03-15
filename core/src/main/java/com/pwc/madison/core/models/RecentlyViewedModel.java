package com.pwc.madison.core.models;

import java.util.List;

import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface RecentlyViewedModel {

    public List<RecentlyViewedItem> getRecentlyViewedItemsList();
    public boolean isUserLoggedIn();
    public String getComponentName();
}
