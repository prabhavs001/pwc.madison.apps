package com.pwc.madison.core.adapter;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.models.Preference;

/**
 * Adapter to map the properties of a {@link Resource} to a {@link Preference}.
 */
public class PreferenceAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreferenceAdapter.class);

    private static final String PROP_TITLE = "title";
    private static final String PROP_I18N_KEY = "i18nKey";
    private static final String PROP_TAGS = "tags";

    /**
     * Returns a new {@link Preference} object after mapping the required properties of a Resource.
     *
     * @param preferenceResource
     *            {@link Resource} The properties of this resource will be added to the {@link Preference}
     * @return {@link Preference}
     */
    public Preference adaptResourceToPreference(final Resource preferenceResource) {
        Preference preference = null;
        if (null != preferenceResource) {
            final ValueMap properties = preferenceResource.getValueMap();
            final String id = preferenceResource.getPath();
            final String title = properties.get(PROP_TITLE, preferenceResource.getName());
            final String i18nKey = properties.get(PROP_I18N_KEY, String.class);
            final String[] tags = properties.get(PROP_TAGS, String[].class);
            preference = new Preference(id, title, preferenceResource.getParent().getName(), i18nKey, tags);
            LOGGER.debug("Preference adaptResourceToPreference() : Adapting resource at path {} to Preference: {}",
                    preferenceResource.getPath(), preference.toString());
        }
        return preference;
    }

}
