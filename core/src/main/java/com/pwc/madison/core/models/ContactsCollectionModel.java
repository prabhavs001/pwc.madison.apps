package com.pwc.madison.core.models;

import java.util.List;

public interface ContactsCollectionModel {
    List<SmeListItem> getContacts();

    String getHeading();
}
