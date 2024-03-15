package com.pwc.madison.core.models;

/**
 * The service is responsible for providing caching related information used by
 * header/footer and global navigation component.
 */
public interface StructureComponentCaching {

	/**
	 * Returns true in case dispatcher caching is enabled with AEM instance.
	 * 
	 * @return {@link Boolean}
	 */
	public boolean isCachingEnabled();

	/**
	 * Returns true if logged in user is internal user other false. It returns false
	 * in case user is not logged in.
	 * 
	 * @return {@link Boolean}
	 */
	public boolean isInternalUser();

	/**
	 * Returns the Global Navigation configured component path for the current page.
	 * The path returns can also be the parent global navigation path depending upon
	 * if the global navigation is inherited from parent.
	 * 
	 * @return {@link String}
	 */
	public String getGlobalNavigationResourcePath();

	/**
	 * Returns the Footer configured component path for the current page. The path
	 * returns can also be the parent Footer path depending upon if the Footer is
	 * inherited from parent.
	 * 
	 * @return {@link String}
	 */
	public String getFooterResourcePath();

	/**
	 * Returns the Header configured component path for the current page. The path
	 * returns can also be the parent Header path depending upon if the Header is
	 * inherited from parent.
	 * 
	 * @return {@link String}
	 */
	public String getHeaderResourcePath();

	/**
	 * Returns the header HTML file path in repository.
	 * 
	 * @return {@link String}
	 */
	public String getHeaderHtmlPath();

	/**
	 * Returns the Territory Mapping configured component path for the current page.
	 * @return {@link String}
	 */
	public String getTerritoryMapperResourcePath();

}
