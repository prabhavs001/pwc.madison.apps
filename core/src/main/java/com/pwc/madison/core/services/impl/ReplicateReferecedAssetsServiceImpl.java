package com.pwc.madison.core.services.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.AssetReferenceSearch;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.day.cq.search.QueryBuilder;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.services.ReplicateReferecedAssetsService;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Replicate referenced assets service implementation
 */

@Component(service = { ReplicateReferecedAssetsService.class },
           immediate = true,
           property = { "service.description=" + "Replicates all the referenced assets on generated AEM pages" })

public class ReplicateReferecedAssetsServiceImpl implements ReplicateReferecedAssetsService {
    /**
     * Default Logger
     */
    private final Logger log = LoggerFactory.getLogger(ReplicateReferecedAssetsServiceImpl.class);
    @Reference
    ResourceResolverFactory resolverFactory;
    @Reference
    QueryBuilder queryBuilder;
    @Reference
    Replicator replicator;
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    @Override
    public void replicateReferencedAssets(Resource resource) {
        ResourceResolver resourceResolver = MadisonUtil
                .getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
        try {
            if (null == resourceResolver) {
                log.error("Workflow ResourceResolver/ null. Cannot proceed with replication.");
                return;
            }
            Session session = resourceResolver.adaptTo(Session.class);
            if (null != resource) {
                Resource resouceJcr = resourceResolver
                        .getResource(resource.getPath() + MadisonConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
                Node jcrNode = resouceJcr.adaptTo(Node.class);
                AssetReferenceSearch ref = new AssetReferenceSearch(jcrNode, DamConstants.MOUNTPOINT_ASSETS,
                        resourceResolver);
                Map<String, Asset> allref = new HashMap<String, Asset>();
                allref.putAll(ref.search());

                for (Map.Entry<String, Asset> entry : allref.entrySet()) {
                    String assetPath = entry.getKey();
                    Asset asset = entry.getValue();

                    if (assetPath.endsWith(DITAConstants.DITAMAP_EXT) || assetPath
                            .endsWith(DITAConstants.DITA_EXTENSION)) {
                        continue;
                    } else {
                        Resource assetJcr = resourceResolver.getResource(
                                asset.getPath() + MadisonConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
                        if (null != assetJcr) {
                            ValueMap valueMap = assetJcr.getValueMap();
                            String repStatus = valueMap.containsKey(MadisonConstants.REPLICATION_ACTION_PROPERTY) ?
                                    valueMap.get(MadisonConstants.REPLICATION_ACTION_PROPERTY, String.class) :
                                    StringUtils.EMPTY;
                            Date lastModifiedDate = valueMap.containsKey(JcrConstants.JCR_LASTMODIFIED) ?
                                    valueMap.get(JcrConstants.JCR_LASTMODIFIED, Date.class) :
                                    null;
                            Date lastReplicatedDate = valueMap.containsKey(DITAConstants.META_LAST_REPLICATED_DATE) ?
                                    valueMap.get(DITAConstants.META_LAST_REPLICATED_DATE, Date.class) :
                                    null;
                            Boolean needReplication = true;
                            if (null != lastModifiedDate && null != lastReplicatedDate) {
                                if (lastModifiedDate.getTime() <= lastReplicatedDate.getTime()) {
                                    needReplication = false;
                                }
                            }
                            if (repStatus.equals(MadisonConstants.REPLICATION_ACTION_ACTIVATE) && !needReplication) {
                                continue;
                            } else {
                                log.debug("assetpaths : {}", assetPath);
                                replicator.replicate(session, ReplicationActionType.ACTIVATE, assetPath);
                            }
                        }
                    }
                }
            } else {
                return;
            }
        } catch (ReplicationException e) {
            log.error("ReplicationException in ReplicateReferecedAssets {}", e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }
}
