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
import com.pwc.madison.core.models.Buzzsprout;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { Buzzsprout.class },
    resourceType = BuzzsproutImpl.RESOURCE_TYPE)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)

public class BuzzsproutImpl implements Buzzsprout {

    private static final String PN_DATA = "data";
    public static final Logger LOGGER = LoggerFactory.getLogger(BuzzsproutImpl.class);
    public static final String RESOURCE_TYPE = "pwc-madison/components/ditacontent/pwcbuzzsprout";

    @SlingObject
    private Resource resource;

    String data = StringUtils.EMPTY;

    @PostConstruct
    private void initModel() {
        final ValueMap valueMap = resource.getValueMap();
        data = valueMap.containsKey(PN_DATA) ? valueMap.get(PN_DATA, String.class) : StringUtils.EMPTY;
        if (StringUtils.isNotBlank(data) && data.contains("<iframe")) {
            data = StringUtils.EMPTY;
        }
    }

    @Override
    public String getData() {
        return data;
    }

}
