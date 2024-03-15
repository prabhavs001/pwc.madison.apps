package com.pwc.madison.core.models;

import java.util.List;
import java.util.Map;
import java.util.Set;


import org.apache.sling.api.resource.Resource;

/**
 * Sling model that lists the publishing points and fetches the topics sent for the workflow
 */
public interface PublishingPointsModel {
    List<Resource> getTopicList();

    Map<String, String> getPublishingPoints();

    Map<String, Set<String>> getDitaMapList();

    Set<String> getVisibleTopicList();

    }
