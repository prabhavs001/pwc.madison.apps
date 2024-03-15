package com.pwc.madison.core.models.impl;

import com.adobe.cq.export.json.ExporterConstants;

import com.pwc.madison.core.models.InLoopTableModel;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Class TextModelImpl.
 */
@Model(adaptables = { SlingHttpServletRequest.class,
        Resource.class }, adapters = InLoopTableModel.class, resourceType = TableModelImpl.RESOURCE_TYPE, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class TableModelImpl implements InLoopTableModel {

    /**
     * The Constant LOGGER.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TableModelImpl.class);
    /**
     * The Class RESOURCE_TYPE.
     */
    public static final String RESOURCE_TYPE = "pwc-madison/components/inloop/table";

    /**
     * The tableData .
     */
    @ValueMapValue private String tableData;

    /**
     * The textIsRich.
     */
    @ValueMapValue private String textIsRich;

    /**
     * Gets the textIsRich.
     *
     * @return the textIsRich
     */
    @Override public String hasRichText() {
        return textIsRich;
    }

    /**
     * Gets the tableData.
     *
     * @return the tableData
     */
    @Override public String getTableData() {
        return tableData;
    }

}
