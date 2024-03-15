package com.pwc.madison.core.models;

import java.util.List;

public interface PodcastModel {

    /**
     * @return isPodcastEnabled
     */
    public Boolean isPodcastEnabled();

    /**
     * @return List of Podcast Widgets
     */
    public List<PodcastWidget> getPodcastList();

}
