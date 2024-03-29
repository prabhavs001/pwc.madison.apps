package com.pwc.madison.core.models.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.day.cq.commons.Externalizer;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.TwitterModel;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.util.MadisonUtil;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = TwitterModel.class,
    resourceType = TwitterModelImpl.RESOURCE_TYPE)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class TwitterModelImpl implements TwitterModel {

    @ScriptVariable
    private Page currentPage;

    @SlingObject
    private ResourceResolver resourceResolver;

    @Inject
    SlingSettingsService slingSettingsService;

    @Inject
    CountryTerritoryMapperService countryTerritoryMapperService;

    @Self
    private SlingHttpServletRequest request;

    private String title;
    private String pageUrl;
    private boolean isTwitterShareEnabled = false;

    protected static final String RESOURCE_TYPE = "pwc-madison/components/commons/twitter";

    private static final Logger LOGGER = LoggerFactory.getLogger(TwitterModelImpl.class);

    @PostConstruct
    protected void init() {
        title = StringUtils.isNotBlank(currentPage.getNavigationTitle()) ? currentPage.getNavigationTitle()
                : currentPage.getTitle();

        if (StringUtils.isBlank(title)) {
            title = currentPage.getName();
        }

        isTwitterShareEnabled = MadisonUtil.isShareEnabled(currentPage.getPath(), MadisonConstants.PN_TWITTER_SHARE,
                countryTerritoryMapperService);
        final Externalizer externalizer = resourceResolver.adaptTo(Externalizer.class);
        if (null!= currentPage && StringUtils.isNotBlank(currentPage.getPath()) && null != externalizer){
            String runMode = MadisonUtil.getCurrentRunmode(slingSettingsService);
            runMode = StringUtils.isNotBlank(runMode) ? runMode : Externalizer.PUBLISH;
            pageUrl = externalizer.externalLink(resourceResolver, runMode,
                resourceResolver.map(currentPage.getPath())) + MadisonConstants.HTML_EXTN;
            pageUrl = pageUrl + (request.getQueryString() != null ? "?" + request.getQueryString() : StringUtils.EMPTY);
        }else{
            pageUrl = request.getRequestURL().toString()
                + (request.getQueryString() != null ? "?" + request.getQueryString() : StringUtils.EMPTY);
        }


        try {
            title = URLEncoder.encode(title, MadisonConstants.UTF_8);
        } catch (final UnsupportedEncodingException e) {
            LOGGER.error("Error Encoding the URL/title::: {}", e);
        }
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getPageUrl() {
        return pageUrl;
    }

    @Override
    public boolean isTwitterShareEnabled() {
        return isTwitterShareEnabled;
    }

}
