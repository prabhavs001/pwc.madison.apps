package com.pwc.madison.core.services;

/**
 * 
 * Service provides configuration required to include one trust scripts on viewpoint pages.
 *
 */
public interface OneTrustCookieConfigurationService {

    /**
     * Returns one trust script.
     * @return {@link String}
     */
    String getOneTrustScript();
        
}
