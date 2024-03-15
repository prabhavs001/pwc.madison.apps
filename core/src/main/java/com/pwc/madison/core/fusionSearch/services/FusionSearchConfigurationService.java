package com.pwc.madison.core.fusionSearch.services;

import java.util.List;

/**
 *
 * Service that provides Fusion Search Configurations.
 *
 */
public interface FusionSearchConfigurationService {

	/**
	 * Returns Username of the Fusion Search configuration for Viewpoint
	 *
	 * @return {@link String}
	 */
	String getUsername();

	/**
	 * Returns Password of the Fusion Search configuration for Viewpoint
	 *
	 * @return {@link String}
	 */
	String getPassword();

	/**
	 * Returns Search Endpoint of the Fusion Search configuration for Viewpoint
	 *
	 * @return {@link String}
	 */
	String getSearchEndpoint();

	/**
	 * Returns Typeahead Endpoint of the Fusion Search configuration for Viewpoint
	 *
	 * @return {@link String}
	 */
	String getTypeaheadEndpoint();

    /**
     * Returns Signal Endpoint of the Fusion Search configuration for Viewpoint
     *
     * @return {@link String}
     */
    String getSignalEndpoint();

    /**
	 * Returns Indexing Endpoint of the Fusion Search configuration for Viewpoint
	 *
	 * @return {@link String}
	 */
	String getIndexingEndpoint();

	/**
	 * Returns Indexing Locales of the Fusion Search configuration for Viewpoint
	 *
	 * @return {@link String}
	 */
	List <String> getIndexingLocales();

}