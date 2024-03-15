package com.pwc.madison.core.reports;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

@Model(adaptables = Resource.class)
public interface AssetsReportConfig {

    @Inject
    int getPageSize();
}
