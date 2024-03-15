package com.pwc.madison.core.models.impl;

import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.models.PodcastModel;
import com.pwc.madison.core.models.PodcastWidget;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.util.MadisonUtil;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = PodcastModel.class,
    resourceType = PodcastModelImpl.RESOURCE_TYPE)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)

public class PodcastModelImpl implements PodcastModel {

    @OSGiService
    CountryTerritoryMapperService countryTerritoryMapperService;

    @ScriptVariable
    private Page currentPage;

    private static final Logger LOG = LoggerFactory.getLogger(PodcastModel.class);

    private boolean podcastEnabled;
    private List<PodcastWidget> podcastList = null;

    protected static final String RESOURCE_TYPE = "pwc-madison/components/ditacontent/podcast";

    @PostConstruct
    protected void init() {

        final ValueMap valueMap = currentPage.getProperties();
        if (valueMap.containsKey(DITAConstants.META_TAGS) && valueMap.containsKey(DITAConstants.META_AUDIENCE)) {
            final List<String> tags = Arrays.asList(valueMap.get(DITAConstants.META_TAGS, String[].class));
            final String audience = valueMap.get(DITAConstants.META_AUDIENCE, String.class);
            LOG.debug("list of tags assosiated for this page: {}", tags);
            if (tags.contains("pwc:media_type/podcast")
                    && (DITAConstants.AUDIENCE_INTERNAL_EXTERNAL.equalsIgnoreCase(audience)
                            || DITAConstants.AUDIENCE_EXTERNAL_ONLY.equalsIgnoreCase(audience))) {

                final String countryCode = MadisonUtil.getTerritoryCodeForPath(currentPage.getPath());
                final String localeCode = MadisonUtil.getLanguageCodeForPath(currentPage.getPath());
                podcastList = countryTerritoryMapperService.getPodcastListByTerritoryLocale(countryCode, localeCode);
                LOG.debug("list of podcast assosiated for this page: {}", podcastList);
                if (!podcastList.isEmpty()) {
                    podcastEnabled = true;
                }
            }
        }
    }

    @Override
    public Boolean isPodcastEnabled() {
        return podcastEnabled;
    }

    @Override
    public List<PodcastWidget> getPodcastList() {
        return podcastList;
    }

}
