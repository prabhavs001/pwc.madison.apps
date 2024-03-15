package com.pwc.madison.core.models;

import java.util.List;
import java.util.Map;

public interface ArticleMultimediaModal {

    Map<String, List<String>> getMultimediaMap();

    String getPageTitle();

    String getContentId();

    String getContentType();

    String getCountryLabel();

    String getFormattedRevisedDate();

    String getViewDate();

    Boolean getHidePublicationDate();

}
