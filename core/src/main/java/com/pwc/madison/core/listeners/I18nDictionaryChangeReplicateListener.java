package com.pwc.madison.core.listeners;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.List;

/**
 * Resource Change Listener that listens to changes to the i18n dictionaries and replicates them to the publish
 * environment.
 * <br/>
 * <br/>
 **/
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        service = ResourceChangeListener.class)
@Designate(ocd = I18nDictionaryChangeReplicateListener.Config.class)
public class I18nDictionaryChangeReplicateListener implements ResourceChangeListener {
    @Reference
    private ResourceResolverFactory resolverFactory;
    @Reference
    private Replicator replicator;
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    private static final Logger log = LoggerFactory.getLogger(I18nDictionaryChangeReplicateListener.class);
    @Override
    public void onChange(List<ResourceChange> changes) {
        log.info("Changes detected");
        for (final ResourceChange resourceChange : changes) {
            // After replication, another event is fired by the replication-service. Ignore these onChanges to avoid an onChange event loop
            final String userId = resourceChange.getUserId();
            if (StringUtils.isNotBlank(userId) && userId.contains(madisonSystemUserNameProviderService.getReplicationServiceUsername())) {
                return;
            }
            log.debug("Changed Path: {}", resourceChange.getPath());
            replicateChangedResource(resourceChange);
        }
    }

    private void replicateChangedResource(ResourceChange resourceChange) {
        ResourceResolver resourceResolver = null;
        try {
            log.debug("Handling resource Change (type = {} ) with path : {}", resourceChange.getType(),
                    resourceChange.getPath());
            resourceResolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getReplicationServiceUsername());
            if(resourceChange.getType()== ResourceChange.ChangeType.REMOVED){
                replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.DEACTIVATE,
                        resourceChange.getPath());
            }else {
                replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.ACTIVATE,
                        resourceChange.getPath());
            }
        } catch (ReplicationException e) {
            log.error("Exception occurred during i18n replication", e);
        } finally {
            if (resourceResolver != null && resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }
    }


    @ObjectClassDefinition(name = "I18n Dictionary Change Listener Config")
    public @interface Config {
        @AttributeDefinition(name=ResourceChangeListener.PATHS, description = "Configurable paths for event listener", type = AttributeType.STRING, cardinality = 10)
        String[] resource_paths();
        @AttributeDefinition(name=ResourceChangeListener.CHANGES, description = "Event types to handle", type = AttributeType.STRING, cardinality = 10)
        String[] resource_change_types();
    }

}
