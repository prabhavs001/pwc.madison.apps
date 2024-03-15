package com.pwc.madison.core.adapter.factory;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;

import com.pwc.madison.core.adapter.TerritoryAdapter;
import com.pwc.madison.core.models.Territory;

/**
 * Adapter Factory to adapt a Resource to a {@link Territory}.
 */
@Component(
        service = AdapterFactory.class,
        property = { AdapterFactory.ADAPTABLE_CLASSES + "=org.apache.sling.api.resource.Resource",
                AdapterFactory.ADAPTER_CLASSES + "=com.pwc.madison.core.models.Territory" })
public class TerritoryAdapterFactory implements AdapterFactory {
    @Override
    public <AdapterType> AdapterType getAdapter(final Object adaptable, final Class<AdapterType> typeClass) {
        if (adaptable instanceof Resource) {
            return typeClass.cast(new TerritoryAdapter().adaptResourceToTerritory((Resource) adaptable));

        }
        return null;
    }
}
