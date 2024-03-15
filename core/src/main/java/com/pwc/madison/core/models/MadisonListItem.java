package com.pwc.madison.core.models;

import javax.annotation.Nullable;
import java.util.Calendar;

public interface MadisonListItem {

    /**
     * Returns the URL of this {@code ListItem}.
     *
     * @return the URL of this navigation item or {@code null}
     * @since com.adobe.cq.wcm.core.components.models 12.2.0
     */
    @Nullable
    default String getURL() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the title of this {@code ListItem}.
     *
     * @return the title of this navigation item or {@code null}
     * @since com.adobe.cq.wcm.core.components.models 12.2.0
     */
    @Nullable
    default String getTitle() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the description of this {@code ListItem}.
     *
     * @return the description of this navigation item or {@code null}
     * @since com.adobe.cq.wcm.core.components.models 12.2.0
     */
    @Nullable
    default String getDescription() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the date when this {@code ListItem} was last modified.
     *
     * @return the last modified date of this item or {@code null}
     * @since com.adobe.cq.wcm.core.components.models 12.2.0
     */
    @Nullable
    default Calendar getLastModified() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the path of this {@code ListItem}.
     *
     * @return the list item path or {@code null}
     * @since com.adobe.cq.wcm.core.components.models 12.2.0
     */
    @Nullable
    default String getPath() {
        throw new UnsupportedOperationException();
    }
}
