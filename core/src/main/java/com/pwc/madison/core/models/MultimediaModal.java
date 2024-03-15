package com.pwc.madison.core.models;

import java.util.List;

/**
 * Multimedia Modal component's model.
 * MultimediaModal component is a container component for playing video/podcasts.
 */
public interface MultimediaModal {

    /**
     * @return List of modal items. Each item contains a list of media with there related media of same type.
     */
    List<List<MultimediaWrapperPageWithRelatedLinks>> getMultimediaModalsList();

}
