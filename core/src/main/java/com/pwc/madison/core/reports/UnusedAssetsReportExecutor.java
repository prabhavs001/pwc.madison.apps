package com.pwc.madison.core.reports;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;

/**
 * The Class UnusedAssetsReportExecutor is a Extension of ACS Commons Query Report Executor, which gets the assets and
 * filters based on references to find unused assets.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class UnusedAssetsReportExecutor extends AssetsReportExecutorModel {

}
