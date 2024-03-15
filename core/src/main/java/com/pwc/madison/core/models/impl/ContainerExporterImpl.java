package com.pwc.madison.core.models.impl;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ContainerExporter;
import com.adobe.cq.export.json.SlingModelFilter;
import com.drew.lang.annotations.NotNull;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.factory.ModelFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Class ModernizedContainerExporterImpl.
 */
public abstract class ContainerExporterImpl implements ContainerExporter {

    @OSGiService
    private ModelFactory modelFactory;

    @OSGiService
    private SlingModelFilter slingModelFilter;

    /** The request. */
    @Self
    private SlingHttpServletRequest request;

    private Map<String, ComponentExporter> childModels = null;

    /**
     * Returns a map (resource name => Sling Model class) of the given resource
     * children's Sling Models that can be adapted to {@link T}.
     *
     * @param slingRequest the current request
     * @param modelClass   the Sling Model class to be adapted to
     * @return a map (resource name => Sling Model class) of the given resource
     *         children's Sling Models that can be adapted to {@link T}
     */
    @NotNull
    private <T> Map<String, T> getChildModels(@NotNull SlingHttpServletRequest slingRequest,
            @NotNull Class<T> modelClass) {
        Map<String, T> itemWrappers = new LinkedHashMap<String, T>();
        for (final Resource child : slingModelFilter.filterChildResources(request.getResource().getChildren())) {
            if (modelFactory.canCreateFromAdaptable(child, modelClass)) {
                itemWrappers.put(child.getName(),
                        modelFactory.getModelFromWrappedRequest(slingRequest, child, modelClass));
            }
        }

        return itemWrappers;
    }

    @NotNull
    @Override
    public Map<String, ? extends ComponentExporter> getExportedItems() {
        if (childModels == null) {
            childModels = getChildModels(request, ComponentExporter.class);
        }

        return childModels;
    }

    @Override
    public String[] getExportedItemsOrder() {
        Map<String, ? extends ComponentExporter> models = getExportedItems();
        if (models.isEmpty()) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        return models.keySet().toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }

}
