package com.pwc.madison.core.reports;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

import javax.inject.Inject;

/* This class used to get configured values for xrefs report */

@Model(adaptables = Resource.class)
public interface SeeAlsoBrokenLinksReportConfig {

    @Inject
    int getPageSize();
}
