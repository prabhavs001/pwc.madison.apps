package com.pwc.madison.core.models;

import java.util.List;

/**
 * Interface for Pending Text Component
 */
public interface PendingText {

    /**
     * @return dateeffective
     */
    String getDateEffective();

    /**
     * @return transitionRef
     */
    String getTransitionRef();

    /**
     * @return list of acs-text
     */
    List<String> getAscTextList();
}
