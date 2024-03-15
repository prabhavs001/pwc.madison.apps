package com.pwc.madison.core.models.impl;

import com.day.cq.wcm.api.TemplatedResource;
import com.pwc.madison.core.models.LandingPageModule;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

@Model(adaptables = SlingHttpServletRequest.class, adapters = LandingPageModule.class)
public class LandingPageModuleImpl implements LandingPageModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(LandingPageModule.class);

    private static final String RESPONSIVE_GRID_NODE_NAME_PREFIX = "parsys-";
    private static final String TITLE_PROPERTY_NAME = "title";
    private static final String COLUMN_CONTROL_RESOURCE_TYPE = "pwc-madison/components/structure/columncontrol";

    @SlingObject
    private Resource currentResource;

    @SlingObject
    private SlingHttpServletRequest slingHttpServletRequest;

    /**
     * First child resource of column control's first responsive grid.
     */
    private Resource overviewComponentResource;

    /**
     * Map containing properties of resource representing accordion items. Key is resource's path and value is resource's {@link ValueMap}.
     * <br><br>
     * Each direct children of column control's responsive grid (applicable on nested column control also) having
     * {@value LandingPageModuleImpl#TITLE_PROPERTY_NAME} property will be qualified as accordion item.
     */
    private Map<String, ValueMap> resourcePropertiesMapForAccordionItem;

    private static final Comparator<Resource> RESOURCE_BY_NAME_COMPARATOR = Comparator.comparing(Resource::getName);

    /**
     * Initialize {@link LandingPageModuleImpl#overviewComponentResource} and {@link LandingPageModuleImpl#resourcePropertiesMapForAccordionItem}
     */
    @PostConstruct
    protected void init() {

        Resource landingPageModuleTemplatedResource = getEffectiveResource();

        Iterator<Resource> landingPageModuleChildrenIterator = landingPageModuleTemplatedResource.listChildren();
        if (landingPageModuleChildrenIterator.hasNext()) {
            Resource columnControlResource = landingPageModuleChildrenIterator.next();
            LOGGER.debug("columnControlResource: {}", columnControlResource);

            TreeSet<Resource> responsiveGridResourceSet = getColumnControlResponsiveGridResourceSet(columnControlResource);

            Resource overviewResponsiveGridResource = responsiveGridResourceSet.pollFirst();
            if (Objects.nonNull(overviewResponsiveGridResource)) {
                Iterator<Resource> overviewResponsiveGridChildrenIterator = overviewResponsiveGridResource.getChildren().iterator();
                if (overviewResponsiveGridChildrenIterator.hasNext()) {
                    overviewComponentResource = overviewResponsiveGridChildrenIterator.next();
                }
                LOGGER.debug("overviewComponentResource: {}", overviewComponentResource);
            }

            resourcePropertiesMapForAccordionItem = new LinkedHashMap<>();

            addAccordionItems(responsiveGridResourceSet);
            LOGGER.debug("Accordion items for column control {}: {}", columnControlResource.getPath(), resourcePropertiesMapForAccordionItem.keySet());

        }
    }

    @Override
    public Resource getOverviewComponentResource() {
        return overviewComponentResource;
    }

    @Override
    public Map<String, ValueMap> getResourcePropertiesMapForAccordionItem() {
        return resourcePropertiesMapForAccordionItem;
    }

    /**
     * Returns a set of all child resources of given resource whose name starts with {@value LandingPageModuleImpl#RESPONSIVE_GRID_NODE_NAME_PREFIX}.
     *
     * @param columnControlResource {@link Resource}
     * @return set of column control's responsive grid resources.
     */
    private TreeSet<Resource> getColumnControlResponsiveGridResourceSet(final Resource columnControlResource) {
        TreeSet<Resource> responsiveGridResourceSet = new TreeSet<>(RESOURCE_BY_NAME_COMPARATOR);
        for (Resource columnControlChildResource : columnControlResource.getChildren()) {
            if (columnControlChildResource.getName().startsWith(RESPONSIVE_GRID_NODE_NAME_PREFIX)) {
                responsiveGridResourceSet.add(columnControlChildResource);
            }
        }

        LOGGER.debug("Found {} responsive grids under {}", responsiveGridResourceSet.size(), columnControlResource.getPath());
        return responsiveGridResourceSet;
    }

    /**
     * Add accordion item candidates in {@link LandingPageModuleImpl#resourcePropertiesMapForAccordionItem}.
     * <br><br>
     * Each direct children of column control's responsive grid (applicable on nested column control also) having
     * {@value LandingPageModuleImpl#TITLE_PROPERTY_NAME} property will be qualified as accordion item.
     *
     * @param responsiveGridResourceSet Ordered {@link java.util.Set} of column control's responsive grid.
     */
    private void addAccordionItems(final TreeSet<Resource> responsiveGridResourceSet) {
        responsiveGridResourceSet.forEach(responsiveGridResource ->
                responsiveGridResource.getChildren().forEach(componentResource -> {
                            if (COLUMN_CONTROL_RESOURCE_TYPE.equals(componentResource.getResourceType())) {
                                TreeSet<Resource> columnControlResponsiveGridResourceSet = getColumnControlResponsiveGridResourceSet(componentResource);
                                addAccordionItems(columnControlResponsiveGridResourceSet);
                            }
                            ValueMap componentProperties = componentResource.adaptTo(ValueMap.class);
                            if (Objects.nonNull(componentProperties.get(TITLE_PROPERTY_NAME))) {
                                resourcePropertiesMapForAccordionItem.put(componentResource.getPath(), componentProperties);
                            }
                        }
                ));
    }

    /**
     * Returns {@link TemplatedResource} for landingPageModule component.
     * {@link TemplatedResource} handles editable components behaviour of dynamic templates.
     *
     * @return returns the {@link TemplatedResource} for landingPageModule component
     */
    private Resource getEffectiveResource() {
        if (currentResource instanceof TemplatedResource) {
            return currentResource;
        } else {
            Resource templatedResource = slingHttpServletRequest.adaptTo(TemplatedResource.class);
            return templatedResource == null ? currentResource : templatedResource;
        }
    }

}
