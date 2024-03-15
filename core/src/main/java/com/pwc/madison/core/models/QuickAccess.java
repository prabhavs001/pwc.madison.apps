package com.pwc.madison.core.models;

import java.util.List;

/**
 * Quick Access Component's Model
 * This Component would be used for creating list of links.
 */
public interface QuickAccess {

    /**
     * @return Quick Access' title
     */
    String getTitle();

    /**
     * @return list of {@link QuickAccessContent}
     */
    List<QuickAccessContent> getContent();
}
