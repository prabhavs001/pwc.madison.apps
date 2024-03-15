package com.pwc.madison.core.models;

import java.util.Collection;

import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ConsumerType;

import com.adobe.cq.export.json.ComponentExporter;
import com.day.cq.wcm.api.Page;
/**
 * Defines the {@code MadisonList} Sling Model used for a generic madison list component in the future. This component
 * currently only supports page lists.
 *
 */@ConsumerType
public interface MadisonList extends ComponentExporter {


    /**
     * Returns the list's items collection, as {@link MadisonListItem}s elements.
     *
     * @return {@link Collection} of {@link MadisonListItem}s
     * @since com.adobe.cq.wcm.core.components.models 12.2.0
     */
    @Nonnull
    default Collection<MadisonListItem> getListItems() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see ComponentExporter#getExportedType()
     * @since com.adobe.cq.wcm.core.components.models 12.2.0
     */
    @Nonnull
    @Override
    default String getExportedType() {
        throw new UnsupportedOperationException();
    }
}
