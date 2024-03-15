package com.pwc.madison.core.models;

import java.util.List;

/**
 *  Insights Link Component's Model.
 *  This component would be used for displaying promotional messaging for PwC content.
 */
public interface InsightsTile {

    /**
     * @return Insights link's title
     */
    String getTitle();

    /**
     * @return Insights Default Link Text
     */
    String getInsightsDefaultLinkText();

    /**
     * @return Insights Default Link Url
     */
    String getInsightsDefaultLink();

    /**
     * @return list of {@link InsightsTileContent}
     */
    List<InsightsTileContent> getInsightsTiles();
}
