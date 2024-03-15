package com.pwc.madison.core.models.impl;

import com.adobe.cq.export.json.ExporterConstants;
import com.pwc.madison.core.models.OverviewComponent;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables = { SlingHttpServletRequest.class }, adapters = { OverviewComponent.class }, resourceType = {
        OverviewComponentImpl.RESOURCE_TYPE })

@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)

public class OverviewComponentImpl implements OverviewComponent {

    protected static final String RESOURCE_TYPE = "pwc-madison/components/structure/overview";

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String overviewTitle;

    @ValueMapValue
    @Default(values=StringUtils.EMPTY)
    private String overviewDescription;

    @Override
    public String getOverviewTitle() {
        return overviewTitle;
    }

    @Override
    public String getOverviewDescription() {
        return overviewDescription;
    }
}
