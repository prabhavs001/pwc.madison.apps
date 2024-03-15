package com.pwc.madison.core.fusionSearch.adapter.factory;

import com.pwc.madison.core.fusionSearch.adapter.SearchFilterAdapter;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import com.pwc.madison.core.fusionSearch.models.SearchFilter;

/**
 * Adapter Factory to adapt a Resource to a {@link SearchFilter}.
 */
@Component(
        service = AdapterFactory.class,
        property = { AdapterFactory.ADAPTABLE_CLASSES + "=org.apache.sling.api.resource.Resource",
                AdapterFactory.ADAPTER_CLASSES + "=com.pwc.madison.core.fusionSearch.models.SearchFilter" })
public class SearchFilterAdapterFactory implements AdapterFactory {

    @Override
    public <AdapterType> AdapterType getAdapter(final Object adaptable, final Class<AdapterType> typeClass) {
        if (adaptable instanceof Resource) {
            return typeClass.cast(new SearchFilterAdapter().adaptResourceToSearchFilter((Resource) adaptable));
        }
        return null;
    }

}
