package com.pwc.madison.core.models;

import org.apache.sling.api.resource.Resource;

import java.util.List;

/**
 * Sling model that defines the topics list and generated page list which are shown on /apps/fmdita/report/topics-regeneration.html
 */
public interface TopicOutputGenerationModel {
    List<Resource> getTopicList();
    List<DITAGeneratedOutputBean> getPageList();
    String getDitaMap();
}
