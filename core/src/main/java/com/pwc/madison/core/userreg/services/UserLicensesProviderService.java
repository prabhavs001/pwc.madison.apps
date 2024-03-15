package com.pwc.madison.core.userreg.services;

import java.util.Map;

/**
 * The service provides the User Licenses.
 *
 */
public interface UserLicensesProviderService {

    /**
     * Returns {@link Map} that contains {@link String} license code as key and {@link String} as license title. 
     * 
     * @return {@link Map}
     */
    public Map<String, String> getLicenseCodeToTitleMap();

}
