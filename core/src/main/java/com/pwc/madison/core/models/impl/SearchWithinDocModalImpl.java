package com.pwc.madison.core.models.impl;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.SearchWithinDocModel;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.services.UserRegPagesPathProvidesService;
import com.pwc.madison.core.util.MadisonUtil;
import java.util.Objects;
import javax.annotation.PostConstruct;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.xss.XSSAPI;
import org.apache.taglibs.standard.tlv.JstlCoreTLV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Model(adaptables = SlingHttpServletRequest.class,
        adapters = SearchWithinDocModel.class,
        resourceType = SearchWithinDocModalImpl.RESOURCE_TYPE)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
            extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class SearchWithinDocModalImpl implements SearchWithinDocModel {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass().getName());
    protected static final String RESOURCE_TYPE = "pwc-madison/components/commons/search-within-doc-modal";

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private ResourceResolver resolver;

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private UserRegPagesPathProvidesService userRegPagesPathProvidesService;

    @OSGiService
    private CountryTerritoryMapperService countryTerritoryMapperService;

    @OSGiService
    private XSSAPI xssapi;

    private static final String PROPERTY_SITE_TITLE = "siteTitle";
    private static final String JCR_CONTENT = "/jcr:content";
    private Resource headerResource;
    private String searchURL;
    private String pageLocale;
    private String pagePath;
    private String pubPointDocContext;
    private String title;

    @PostConstruct
    protected void init(){
        LOG.debug("inside SearchWithinDocModalImpl");
        try {
            pagePath = request.getRequestURI();
            Resource pageResource = request.getResourceResolver().resolve(pagePath);
            pagePath = pageResource != null ? pageResource.getPath() : pagePath;
            if (pagePath.startsWith(userRegPagesPathProvidesService.getBaseUserregPath())) {
                pageLocale = request.getParameter(MadisonConstants.LOCALE_QUERY_PARAM);
            } else if (pagePath.matches(MadisonConstants.MADISON_PAGE_HIERARCHY)) {
                pageLocale = MadisonUtil.getLocaleForPath(pagePath);
            } else {
                pageLocale = countryTerritoryMapperService.getDefaultLocale();
            }
            headerResource = MadisonUtil.getConfiguredResource(currentPage, MadisonConstants.HEADER_RELATIVE_PATH_FROM_PAGE, pageLocale, resolver, xssapi);
            if (Objects.nonNull(headerResource)) {
                ValueMap headerValueMap = headerResource.getValueMap();
                searchURL = headerValueMap.get(MadisonConstants.SEARCH_URL_PROPERTY_NAME, String.class);
            }
            pubPointDocContext = MadisonUtil.getPwcDocContext(currentPage.getPath(), resolver, true);
            String basePath = MadisonUtil.getBasePath(currentPage.getPath(), resolver);
            if(StringUtils.isNotBlank(basePath)){
                Resource basePageResource = resolver.getResource(basePath.concat(JCR_CONTENT));
                if(null != basePageResource){
                    ValueMap properties = basePageResource.getValueMap();
                    if(properties.containsKey(PROPERTY_SITE_TITLE)){
                        title = properties.get(PROPERTY_SITE_TITLE, String.class);
                    }
                }

            }

        }catch (Exception e){
            LOG.error("Exception on SearchWithinDocModalImpl", e);
        }
    }

    @Override
    public String getSearchURL() {
        return searchURL;
    }

    @Override
    public String getPubPointDocContext() {
        return pubPointDocContext;
    }

    @Override
    public String getTitle() {
        return title;
    }
}
