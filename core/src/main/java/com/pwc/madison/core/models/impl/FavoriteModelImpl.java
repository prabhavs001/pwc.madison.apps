package com.pwc.madison.core.models.impl;

import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.FavoriteModel;
import com.pwc.madison.core.util.MadisonUtil;
import javax.annotation.PostConstruct;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = FavoriteModel.class,
    resourceType = FavoriteModelImpl.RESOURCE_TYPE)
public class FavoriteModelImpl implements FavoriteModel {

    @SlingObject
    ResourceResolver resourceResolver;

    @OSGiService
    SlingSettingsService slingSettingsService;

    @ScriptVariable
    private Page currentPage;

    protected static final String RESOURCE_TYPE = "pwc-madison/components/ditacontent/ditadocumentheader";
    private Logger LOG = LoggerFactory.getLogger(this.getClass());

    private String currentPageURL = StringUtils.EMPTY;
    private String runMode = StringUtils.EMPTY;

    @PostConstruct
    protected void init(){
        runMode = MadisonUtil.getCurrentRunmode(slingSettingsService);
        Externalizer externalizer = resourceResolver.adaptTo(Externalizer.class);
        currentPageURL = externalizer.externalLink(resourceResolver, runMode, resourceResolver.map(currentPage.getPath())) + MadisonConstants.HTML_EXTN;
        LOG.debug("FavoriteModelImpl - currentPageURL: {}", currentPageURL);
    }

    @Override
    public String getCurrentPageURL() {
        return currentPageURL;
    }
}
