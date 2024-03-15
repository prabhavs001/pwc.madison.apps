package com.pwc.madison.core.models;


/**
 * Info Tile Component Model
 * This component would be used for displaying some tips and tricks in relation to template in tile format.
 */
public interface InfoTile {

    /**
     * @return Info Tile's Title
     */
    String getTitle();

    /**
     * @return list of Content Text
     */
    String[] getInfoTileContents();
}
