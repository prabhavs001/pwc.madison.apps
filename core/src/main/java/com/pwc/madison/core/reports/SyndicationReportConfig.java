package com.pwc.madison.core.reports;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

import javax.inject.Inject;

/* This class used to get configured values for syndication report */

@Model(adaptables = Resource.class)
public interface SyndicationReportConfig {

    @Inject
    int getPageSize();
}
