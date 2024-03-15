package com.pwc.madison.core.models.impl;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.EmailShare;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


@Model(adaptables = SlingHttpServletRequest.class,adapters = EmailShare.class,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class EmailShareImpl implements EmailShare {

    @ScriptVariable
    private Page currentPage;

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private ResourceResolver resourceResolver;

    @Inject
    SlingSettingsService slingSettingsService;

    @Inject
    String bodyText;

    private String title;

    private String pageUrl;

    private String encodedBodyText = StringUtils.EMPTY;

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailShareImpl.class);
    @PostConstruct
    protected void init() {
        if(StringUtils.isNotBlank(bodyText)){
            try{
                encodedBodyText = URIUtil.encodeAll(bodyText, MadisonConstants.UTF_8);
            }catch (URIException e){
                LOGGER.error("Error encoding mail body - ", e);
            }
        }
        title = StringUtils.isNotBlank(currentPage.getNavigationTitle()) ? currentPage.getNavigationTitle()
            : currentPage.getTitle();
        if (StringUtils.isBlank(title)) {
            title = currentPage.getName();
        }
        try {
            // Need to encode to avoid special characters breaking mailto functionality
            title = URIUtil.encodeAll(title, "UTF-8");
        } catch (URIException e1) {
            LOGGER.error("Error Encoding the Page title::: {}", e1);
        }
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

        // Need to encode to url to avoid breaking mailto functionality
        try {
            pageUrl = URLEncoder.encode(pageUrl, "UTF-8");
        } catch (UnsupportedEncodingException e2) {
            LOGGER.error("Error Encoding the Page URL::: {}", e2);
        }
    }

    public String getTitle() {
        return title;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    @Override
    public String getEncodedBodyText() {
        return encodedBodyText;
    }
}
