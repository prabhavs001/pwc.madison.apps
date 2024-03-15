package com.pwc.madison.core.userreg.services;

/**
 * The service provides user registration page paths.
 *
 */
public interface UserRegPagesPathProvidesService {

    /**
     * Returns the user registration terms and conditions path. Path contains the domain as well.
     * 
     * @return {@link String}
     */
    public String getTermsAndConditionPagePath();

    /**
     * Returns base path where User registration pages exist.
     * 
     * @return {@link String}
     */
    public String getBaseUserregPath();

    /**
     * Returns the user registration gated content page path. Path contains the domain as well.
     * 
     * @return {@link String}
     */
    public String getGatedContentpagePath();
    
}
