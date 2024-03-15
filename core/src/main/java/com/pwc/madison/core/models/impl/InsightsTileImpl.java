package com.pwc.madison.core.models.impl;

import com.adobe.cq.export.json.ExporterConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.InsightsTile;
import com.pwc.madison.core.models.InsightsTileContent;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Model(
        adaptables = {SlingHttpServletRequest.class},
        adapters = {InsightsTile.class},
        resourceType = {InsightsTileImpl.RESOURCE_TYPE})
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class InsightsTileImpl implements InsightsTile {

    protected static final String RESOURCE_TYPE = "pwc-madison/components/content/insights-tile";

    private static final Logger LOGGER = LoggerFactory.getLogger(InsightsTile.class);

    private List<InsightsTileContent> insightsTiles;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String title;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String insightsDefaultLinkText;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String insightsDefaultLink;

    @ChildResource(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Resource insightsContent;

    @PostConstruct
    protected void init() {
        insightsDefaultLink += MadisonUtil.isLinkInternal(insightsDefaultLink) ? MadisonConstants.HTML_EXTN : "";

        LOGGER.debug("Insights Tile Content multifield resource: {}", insightsContent);
        if (insightsContent != null) {
            insightsTiles = new ArrayList<>();
            for (Resource resource : insightsContent.getChildren()) {
                InsightsTileContent insightsTileContent = resource.adaptTo(InsightsTileContent.class);
                if (Objects.nonNull(insightsTileContent)) {
                    LOGGER.debug("Modified Insights Links Multifield object with page title and date for {}: {}", resource.getPath(), insightsTileContent);
                    insightsTiles.add(insightsTileContent);
                }
            }
        }
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getInsightsDefaultLinkText() {
        return insightsDefaultLinkText;
    }

    @Override
    public String getInsightsDefaultLink() {
        return insightsDefaultLink;
    }

    @Override
    public List<InsightsTileContent> getInsightsTiles() {
        return insightsTiles;
    }
}
