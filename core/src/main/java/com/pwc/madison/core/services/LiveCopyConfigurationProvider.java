package com.pwc.madison.core.services;

/**
 * 
 * Service to provide viewpoint live copy required configurations.
 *
 */
public interface LiveCopyConfigurationProvider {

    /**
     * Returns the array of {@link String} of authorizables that is groups/users to which live copy notifications should
     * be sent.
     * 
     * @param territoryCode
     *            {@link String} The territory code for which the authorizables is required.
     * @return Array of {@link String}
     */
    public String[] getNotificationAuthorizablesByTerritory(final String territoryCode);

}
