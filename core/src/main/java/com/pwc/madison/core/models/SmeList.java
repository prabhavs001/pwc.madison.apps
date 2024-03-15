package com.pwc.madison.core.models;

import java.util.Collection;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Defines the {@code List} Sling Model used for the {@code /apps/pwc-madison/components/content/sme-list} component.
 * This component currently only supports page lists.
 *
 */
@ConsumerType
public interface SmeList extends MadisonList {

    /**
     * Returns the list's items collection, as {@link SmeListItem}s elements.
     *
     * @return {@link Collection} of {@link SmeListItem}s
     */
    default Collection<SmeListItem> getSmeListItems() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns itemType property.
     * 
     * @return {@link String}
     */
    public String getItemType();

}
