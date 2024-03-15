package com.pwc.madison.core.models;

import java.util.List;

/**
 * @author Divanshu
 *
 */

public interface AlertsModel {

    /**
     * Getter for the title List
     *
     * @return {@link titleList}
     */
    public List<Alerts> getAlertsList();
    
    public Boolean getIsContentAvailable();
}
