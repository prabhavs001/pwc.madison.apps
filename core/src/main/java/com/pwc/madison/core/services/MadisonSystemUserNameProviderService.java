package com.pwc.madison.core.services;

/**
 * The service provide system user name for the AEM instance.
 */
public interface MadisonSystemUserNameProviderService {

    /**
     * Returns the FMDITA service username of Viewpoint.
     * 
     * @return {@link String}
     */
    public String getFmditaServiceUsername();

    /**
     * Returns the replication service username of Viewpoint.
     * 
     * @return {@link String}
     */
    public String getReplicationServiceUsername();

}
