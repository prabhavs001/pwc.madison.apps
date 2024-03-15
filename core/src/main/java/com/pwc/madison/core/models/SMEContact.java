package com.pwc.madison.core.models;

import java.util.List;

/**
 * SME Contact component's model
 * This component would be used for displaying contact information for SME’s.
 */
public interface SMEContact {

    /**
     * @return Component's title
     */
    String getTitle();

    /**
     * @return list of {@link SmeListItem}
     */
    List<SmeListItem> getContacts();

}
