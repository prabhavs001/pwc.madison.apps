package com.pwc.madison.core.services;


import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * This Service Provides Implementation to copy Dita/Ditamap and other Assets from Source Folder to Destination.
 */
public interface SyndicationService {

    /**
     * Copy dita and dita map from source to destination and conref the bodydiv
     *
     * @param sourceResource
     * @param destinationResource
     * @return
     */
    public String processSyndication(Resource sourceResource, Resource destinationResource);

    /**
     * Copy List of dita and ditaMap from SourceBaseFolder to DestinationFolder.
     *
     * @param resources
     * @param sourceResource
     * @param destinationResource
     * @return
     */
    public String processSyndication(Set<String> resources, Resource sourceResource, Resource destinationResource);

    /**
     * Copy Non editable metadata from source to subscribing territory
     *
     * @param toBeUpdatedAssets
     * @param syndicationSource
     * @param territory
     */
    public void copyNonEditableMetadata(Set<Resource> toBeUpdatedAssets, Resource syndicationSource,
            Resource territory, ResourceResolver resourceResolver);
}
