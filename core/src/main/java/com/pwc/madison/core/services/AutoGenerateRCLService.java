package com.pwc.madison.core.services;

import org.apache.sling.api.resource.ResourceResolver;

/**
 * DITA in-line Links Service Interface
 *
 * @author sevenkat
 *
 */
public interface AutoGenerateRCLService {
	
	/**
	 * Method to update in-line links section on a given DITA
	 * @param ditaPath
	 * ditaPath is the asset which is being authored
	 * @param resolver 
	 * @return
	 * Success message when in-line links has been updated
	 */
	public void updateInlineLinks(final String ditaPath, ResourceResolver resolver);

}
