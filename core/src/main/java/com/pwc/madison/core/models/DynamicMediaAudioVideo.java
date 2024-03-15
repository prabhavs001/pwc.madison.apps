package com.pwc.madison.core.models;

/**
 * Dynamic media audio video component's model.
 * Dynamic media audio video component is an extension of OOTB dynamic media component to support audio files..
 */
public interface DynamicMediaAudioVideo {

    /**
     * @return true if the authored asset is an audio file, false otherwise
     */
    boolean isAudioFile();

    /**
     * Returns scene7 publish url generated using scene7 properties of the asset.
     * <br>
     * Note: This is for audio assets only and might return an invalid URL for images and videos.
     *
     * @return scene7 publish URL for the asset.
     */
    String getS7PublishUrl();

    /**
     * @return thumbnail path of the asset
     */
    String getThumbnailPath();

}
