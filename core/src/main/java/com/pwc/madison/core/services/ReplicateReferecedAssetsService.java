/**
 *
 */
package com.pwc.madison.core.services;

import org.apache.sling.api.resource.Resource;

/**
 * The Interface ReplicateReferecedAssetsService for replicating assets referenced assets on generated AEM pages
 *
 */
public interface ReplicateReferecedAssetsService {
    public void replicateReferencedAssets(Resource resource);
}
