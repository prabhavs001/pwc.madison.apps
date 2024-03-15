package com.pwc.madison.core.models;

import java.util.List;

public interface RelatedContentModel {
    List<FeaturedContentItem> getFilteredList();

    String getHeading();
}
