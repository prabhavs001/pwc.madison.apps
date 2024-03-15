package com.pwc.madison.core.reports.configs;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

/* This class used to get configured values for syndication report */

@Model(adaptables = Resource.class)
public interface VitalStatsReportConfig {
    
    @Inject
    int getPageSize();
}
