package com.pwc.madison.core.models;

import java.util.List;

/*
 * Model class for populating the authorable columnControl component fields.
 */
public interface ColumnControl {
    /**
     * @return a list of columns for Column Control.
     */
    List<Long> getColumns();

    /**
     * @return Page Type
     */
    public String getPageType();

}
