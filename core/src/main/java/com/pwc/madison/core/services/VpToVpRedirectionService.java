package com.pwc.madison.core.services;

import java.util.List;

import com.pwc.madison.core.beans.VpRedirectionModifyEventInfo;

/**
 * 
 * Viewpoint to viewpoint redirect service helps to manage the redirection from viewpoint.
 *
 */
public interface VpToVpRedirectionService {

    /**
     * Returns the new viewpoint redirected path for non existing old path.
     * 
     * @param oldPath
     *            {@link String}
     * @return {@link String} returns NULL if redirection path for non existing old path does not exists.
     */
    String getRedirectPath(final String oldPath);

    /**
     * It takes {@link List} of {@link VpRedirectionModifyEventInfo} which contains required information around URL
     * changes. It processes and filters data to check if the modified URL needs to be captured and saves the final
     * filtered List for VP to VP redirection.
     * 
     * @param modifiedUrlsInfoList
     *            {@link List} of {@link VpRedirectionModifyEventInfo}
     */
    void captureModifiedUrls(final List<VpRedirectionModifyEventInfo> modifiedUrlsInfoList);

}
