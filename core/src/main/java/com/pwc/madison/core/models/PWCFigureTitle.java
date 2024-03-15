package com.pwc.madison.core.models;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Defines the {@code PWCTopicHeader} Sling Model used for the
 * {@code /apps/pwc-madison/components/ditacontent/figuretitle} component.
 *
 */
@ConsumerType
public interface PWCFigureTitle {

    /**
     * @return FigureID
     */
    public String getFigureID();

    /**
     * @return FigureTitle
     */
    public String getFigureTitle();
}
