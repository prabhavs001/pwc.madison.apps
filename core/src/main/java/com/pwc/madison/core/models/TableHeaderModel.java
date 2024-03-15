package com.pwc.madison.core.models;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Defines the {@code PWCTopicHeader} Sling Model used for the
 * {@code /apps/pwc-madison/components/ditacontent/tableheader} component.
 *
 */
@ConsumerType
public interface TableHeaderModel {

    /**
     * Returns the Header Text Configured for the table.
     *
     * @return table Header.
     * @see #getTableHeader()
     *
     */
    public String getTableHeader();

    /**
     * Returns the CSS Value for the tableHeader.
     *
     * @return Table CSS Class Value.
     * @see #getCSSClassVal()
     *
     */
    public String getCSSClassVal();

}
