package com.pwc.madison.core.services;

/**
 *
 * Service that provides list of Domains for External Redirect to Viewpoint.
 *
 */
public interface ExternalRedirectDomainConfigurationService {

	/**
	 * Returns array of domains for external redirect
	 *
	 * @return {@link String[]}
	 */
	String[] getDomainList();
	
	/**
	 * Returns Inform URL configured for external redirect
	 *
	 * @return {@link String}
	 */
	String getInformDomain();

	/**
	 * Returns CSV of territories for external redirect using query parameter
	 *
	 * @return {@link String}
	 */
	String getTerritories();

}
