package com.pwc.madison.core.models.impl;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ExporterConstants;
import com.pwc.madison.core.models.TableHeaderModel;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { TableHeaderModel.class },
    resourceType = TableHeaderModelImpl.RESOURCE_TYPE)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class TableHeaderModelImpl implements TableHeaderModel {

    public static final String RESOURCE_TYPE = "pwc-madison/components/ditacontent/tableheader";
    public static final Logger LOGGER = LoggerFactory.getLogger(TableHeaderModelImpl.class);

    @SlingObject
    private Resource resource;

    private String cSSClassVal = StringUtils.EMPTY;
    private String tableHeader = StringUtils.EMPTY;

    @Override
    public String getTableHeader() {
        return tableHeader;
    }

    @Override
    public String getCSSClassVal() {
        return cSSClassVal;
    }

    @PostConstruct
    private void initModel() {

        final ValueMap valueMap = resource.getValueMap();
        cSSClassVal = valueMap.containsKey("outputclass") ? valueMap.get("outputclass", String.class)
                : StringUtils.EMPTY;

        final Resource titleTextRes = resource.getChild("_text");
        if (null != titleTextRes) {
            final ValueMap titleTextValueMap = titleTextRes.getValueMap();
            tableHeader = titleTextValueMap.containsKey("text") ? titleTextValueMap.get("text", String.class)
                    : StringUtils.EMPTY;
        }

    }

}
