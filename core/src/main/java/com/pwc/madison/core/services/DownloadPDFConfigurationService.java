package com.pwc.madison.core.services;

/**
 *
 * Service that provides PDF download related configuration.
 *
 */
public interface DownloadPDFConfigurationService {

    /**
     * Returns array of paths where PDF Download needs to be disabled.
     *
     * @return {@link String[]}
     */
    String[] getExcludedPaths();

    /**
     * Returns array of paths where PDF needs to be downloaded as a full guide irrespective of map type.
     *
     * @return {@link String[]}
     */
    String[] getFullGuidePaths();

}
