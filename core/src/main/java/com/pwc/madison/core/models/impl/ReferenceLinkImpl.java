package com.pwc.madison.core.models.impl;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.LinkField;
import com.pwc.madison.core.models.ReferenceLink;
import com.pwc.madison.core.models.ReferenceSearchTerms;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.services.UserRegPagesPathProvidesService;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.*;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Model(
        adaptables = { SlingHttpServletRequest.class },
        adapters = { ReferenceLink.class },
        resourceType = { ReferenceLinkImpl.RESOURCE_TYPE })

@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class ReferenceLinkImpl implements ReferenceLink {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceLink.class);

    protected static final String RESOURCE_TYPE = "pwc-madison/components/content/referencelinks";
    protected static final String searchURLConstant = "searchURL";

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private ResourceResolver resolver;

    private String searchUrl;
    private String pagePath;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String referenceLinkType;

    private List<LinkField> referenceLinkList;

    private List<ReferenceSearchTerms> suggestedTermList;

    @ChildResource(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Resource referenceLinks;

    @ChildResource(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Resource suggestedTerms;

    @OSGiService
    private CountryTerritoryMapperService countryTerritoryMapperService;

    @OSGiService
    private UserRegPagesPathProvidesService userRegPagesPathProvidesService;

    @OSGiService
    private XSSAPI xssAPI;

    @PostConstruct
    protected void init(){
        LOGGER.debug("Reference Link Content Type: {}",referenceLinkType);

        LOGGER.debug("Helpful Link resource : {}", referenceLinks);
        if(referenceLinks != null){
            referenceLinkList = new ArrayList<>();
            for(Resource resource : referenceLinks.getChildren()){
                LinkField referenceLink = resource.adaptTo(LinkField.class);
                if(Objects.nonNull(referenceLink)){
                    referenceLinkList.add(referenceLink);
                }
            }
            LOGGER.debug("Helpful link list: {}", referenceLinkList);
        }

        LOGGER.debug("Suggested Term resource : {}", suggestedTerms);
        if(suggestedTerms != null){
            suggestedTermList = new ArrayList<>();
            for(Resource resource : suggestedTerms.getChildren()) {
                ReferenceSearchTerms referenceSearchTerm = resource.adaptTo(ReferenceSearchTerms.class);
                if(Objects.nonNull(referenceSearchTerm))
                {
                    suggestedTermList.add(referenceSearchTerm);
                }
            }
            LOGGER.debug("Suggested term list {}", suggestedTermList);
            pagePath = request.getRequestURI();
            String pageLocale;
            if (pagePath.startsWith(userRegPagesPathProvidesService.getBaseUserregPath())) {
                pageLocale = request.getParameter(MadisonConstants.LOCALE_QUERY_PARAM);
            } else if (pagePath.matches(MadisonConstants.MADISON_PAGE_HIERARCHY)) {
                pageLocale = MadisonUtil.getLocaleForPath(pagePath);
            } else {
                pageLocale = countryTerritoryMapperService.getDefaultLocale();
            }
            LOGGER.debug("Page Locale {} ", xssAPI.encodeForHTML(pageLocale));
            Resource header = MadisonUtil.getConfiguredResource(currentPage, MadisonConstants.HEADER_RELATIVE_PATH_FROM_PAGE,pageLocale,resolver, xssAPI);
            LOGGER.debug("Header Resource Node {}",header);
            if(header != null) {
                searchUrl = (String) header.getValueMap().get(searchURLConstant);
                if (searchUrl != null && searchUrl.length() > 0)
                    searchUrl +=MadisonConstants.HTML_EXTN;
            }
            LOGGER.debug("Search Url From Header Node {}", searchUrl);
        }
    }

    @Override
    public String getReferenceLinkType() {
        return referenceLinkType;
    }

    @Override
    public List<LinkField> getReferenceLinkList() {
        return referenceLinkList;
    }

    @Override
    public List<ReferenceSearchTerms> getSuggestedTermList() {
        return suggestedTermList;
    }

    @Override
    public String getSearchUrl() {
        return searchUrl;
    }
}
