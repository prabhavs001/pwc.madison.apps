package com.pwc.madison.core.adapter;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.models.PodcastWidget;

/**
 * Adapter to map the properties of a {@link Resource} to a {@link Podcast}.
 */
public class PodcastAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PodcastAdapter.class);

    public static final String PN_IMAGE = "image";
    public static final String PN_NAME = "name";
    public static final String PN_URL = "url";

    /**
     * Returns a new {@link PodcastWidget} object after mapping the required properties of a Resource.
     *
     * @param territoryResource
     *            {@link Resource} The properties of this resource will be added to the Territory
     * @return {@link PodcastWidget}
     */
    public PodcastWidget adaptResourceToPodcastWidget(final Resource podcastWidgetResource) {
        PodcastWidget podcastWidget = null;
        if (null != podcastWidgetResource) {
            final ValueMap podcastWidgetResValueMap = podcastWidgetResource.getValueMap();
            podcastWidget = new PodcastWidget();
            podcastWidget.setImgPath(podcastWidgetResValueMap.containsKey(PN_IMAGE)
                    ? podcastWidgetResValueMap.get(PN_IMAGE, String.class)
                    : StringUtils.EMPTY);
            podcastWidget.setName(
                    podcastWidgetResValueMap.containsKey(PN_NAME) ? podcastWidgetResValueMap.get(PN_NAME, String.class)
                            : StringUtils.EMPTY);
            podcastWidget.setUrl(
                    podcastWidgetResValueMap.containsKey(PN_URL) ? podcastWidgetResValueMap.get(PN_URL, String.class)
                            : StringUtils.EMPTY);
            LOGGER.debug(
                    "PodcastAdapter adaptResourceToTerritory() : Adapting resource at path {} to PodcastWidget: {}",
                    podcastWidgetResource.getPath(), podcastWidget.toString());
        }
        return podcastWidget;
    }

}
