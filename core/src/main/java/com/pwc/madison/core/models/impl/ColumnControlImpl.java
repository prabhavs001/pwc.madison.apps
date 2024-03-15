/*
 * Model class for populating the authorable columnControl component fields.
 */
package com.pwc.madison.core.models.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.models.ColumnControl;

@Model(
        adaptables = { SlingHttpServletRequest.class },
        adapters = { ColumnControl.class },
        resourceType = { ColumnControlImpl.RESOURCE_TYPE },
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)

public class ColumnControlImpl implements ColumnControl {

    protected static final String RESOURCE_TYPE = "pwc-madison/components/structure/columncontrol";
    private static final String COLUMN_CONTROL_FIELD_NAME = "column";

    private static final Logger LOGGER = LoggerFactory.getLogger(ColumnControlImpl.class);

    @ChildResource(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Resource columnControl;

    private List<Long> columns = new ArrayList<>();

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String pageType;

    @PostConstruct
    protected void init() {
        if (columnControl != null) {

            for (Resource value : columnControl.getChildren()) {
                Long column = value.adaptTo(ValueMap.class).get(COLUMN_CONTROL_FIELD_NAME, Long.class);
                columns.add(column);
            }

        } else {
            LOGGER.debug("Generate ColumControl null");
        }
    }

    @Override
    public List<Long> getColumns() {
        return columns;

    }

    public String getPageType() {
        return pageType;
    }

}
