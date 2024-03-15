package com.pwc.madison.core.models;

import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.pwc.madison.core.models.impl.LandingPageModuleImpl;

/**
 * LandingPageModule component's model.
 * LandingPageModule component is a container component for Landing pages used to create accordion layout for mobile.
 */
public interface LandingPageModule {

    /**
     * @return resource object representing overview component(first child resource of column control's first responsive grid).
     */
    Resource getOverviewComponentResource();

    /**
     * Returns a {@link Map} containing properties of resource representing accordion items. Key is resource's path and value is resource's {@link ValueMap}.
     * <br><br>
     * Each direct children of column control's responsive grid (applicable on nested column control also) having
     * {@value LandingPageModuleImpl#TITLE_PROPERTY_NAME} property will be qualified as accordion item.
     *
     * @return map of accordion items
     */
    Map<String, ValueMap> getResourcePropertiesMapForAccordionItem();

}
