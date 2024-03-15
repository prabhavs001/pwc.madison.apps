package com.pwc.madison.core.adapter.factory;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;

import com.pwc.madison.core.adapter.PreferenceAdapter;
import com.pwc.madison.core.models.Preference;

/**
 * Adapter Factory to adapt a Resource to a {@link Preference}.
 */
@Component(
        service = AdapterFactory.class,
        property = { AdapterFactory.ADAPTABLE_CLASSES + "=org.apache.sling.api.resource.Resource",
                AdapterFactory.ADAPTER_CLASSES + "=com.pwc.madison.core.models.Preference" })
public class PreferenceAdapterFactory implements AdapterFactory {

    @Override
    public <AdapterType> AdapterType getAdapter(final Object adaptable, final Class<AdapterType> typeClass) {
        if (adaptable instanceof Resource) {
            return typeClass.cast(new PreferenceAdapter().adaptResourceToPreference((Resource) adaptable));
        }
        return null;
    }
}
