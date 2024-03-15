package com.pwc.madison.core.models;

import java.util.List;

/**
 * Featured Content Tile Model.
 * This Component would be used for displaying the title and content of different page.
 */
public interface FeaturedContentTile {

	/**
	* @return FeaturedContentTile title
	*/
	String getTitle();

	/**
	 * @return FeaturedContentTile content
	 */
	List<FeaturedContentTileContent> getTileContent();

}