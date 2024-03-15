package com.pwc.madison.core.models.impl;

import com.adobe.cq.export.json.ExporterConstants;
import com.pwc.madison.core.models.InfoTile;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Model(
        adaptables = { SlingHttpServletRequest.class },
        adapters = { InfoTile.class },
        resourceType = { InfoTileImpl.RESOURCE_TYPE })
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class InfoTileImpl implements InfoTile {

    private static final Logger LOGGER = LoggerFactory.getLogger(InfoTile.class);

    protected static final String RESOURCE_TYPE = "pwc-madison/components/content/info-tile";

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String title;

    @ValueMapValue(name = "infoContentText")
    @Optional
    private String[] infoTileContents;

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String[] getInfoTileContents() {
        return infoTileContents;
    }
}
