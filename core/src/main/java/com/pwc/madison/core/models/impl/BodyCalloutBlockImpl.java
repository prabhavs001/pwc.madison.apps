package com.pwc.madison.core.models.impl;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ExporterConstants;
import com.pwc.madison.core.models.BodyCalloutBlock;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { BodyCalloutBlock.class },
    resourceType = BodyCalloutBlockImpl.RESOURCE_TYPE, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)

public class BodyCalloutBlockImpl implements BodyCalloutBlock {

    public static final Logger LOGGER = LoggerFactory.getLogger(BodyCalloutBlockImpl.class);
    public static final String RESOURCE_TYPE = "pwc-madison/components/ditacontent/inbodycallout/pwccalloutblock";
    
    @ValueMapValue
    private String bgColor;

    @ValueMapValue
    private String type;
    
    @Override
	public String getBgColor() {
		return bgColor;
	}
    
	@Override
	public String getType() {
		return type;
	}

}
