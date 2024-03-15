package com.pwc.madison.core.models;

import java.util.List;

/**
 * Topic FAQ Tile component's model.
 * Topic FAQ Tile Component is to be used for displaying frequently asked questions and their answers.
 */
public interface TopicFaqTile {

    /**
     * @return Title of the FAQ tile component
     */
    String getTitle();

    /**
     *
     * @return List of {@link TopicFaqTileContent}
     */
    List<TopicFaqTileContent> getTopicFaqTileContentList();

}
