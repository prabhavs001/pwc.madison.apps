package com.pwc.madison.core.models.impl;

import com.day.cq.commons.Externalizer;
import com.pwc.madison.core.constants.MadisonConstants;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.models.DitaDocumentHeader;
import com.pwc.madison.core.services.DownloadPDFConfigurationService;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = DitaDocumentHeader.class,
    resourceType = DitaDocumentHeaderImpl.RESOURCE_TYPE)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)

public class DitaDocumentHeaderImpl implements DitaDocumentHeader {

    @SlingObject
    private Resource currentResource;

    @SlingObject
    private ResourceResolver resourceResolver;

    @OSGiService
    private DownloadPDFConfigurationService downloadPDFConfigService;

    @ScriptVariable
    private Page currentPage;
    
    @Inject
    SlingSettingsService slingSettingsService;

    private String tocTitle;
    private Boolean isShareViaEmailOnly;
    private Boolean disablePDFDownload;
    private Boolean hideSearchWithInDocument;
    private Boolean showStaticToc;
    private String runMode;

    private static final Logger LOG = LoggerFactory.getLogger(DitaDocumentHeader.class);
    private static final String YES = "yes";
    private static final String NO = "no";

    protected static final String RESOURCE_TYPE = "pwc-madison/components/ditacontent/ditadocumentheader";

    @PostConstruct
    protected void init() throws RepositoryException {
        if(currentPage.getPath().matches(MadisonConstants.FASB_CONTENT_REGEX)){
            String ditaAncestryValue = DITAUtils.getDitaAncestryValue(currentPage, resourceResolver);
            String longTitle = DITAUtils.getLongTitle(currentPage, resourceResolver);
            String title = StringUtils.isNotBlank(longTitle) ? longTitle : (StringUtils.isNotBlank(currentPage.getNavigationTitle()) ? currentPage.getNavigationTitle() : currentPage.getPageTitle());
            if(Objects.nonNull(ditaAncestryValue)){
                tocTitle = new StringBuilder(ditaAncestryValue).append(" ").append(title).toString();
            }else {
                tocTitle = title;
            }
        }else{
            tocTitle = StringUtils.isNotBlank(currentPage.getNavigationTitle()) ? currentPage.getNavigationTitle()
                : currentPage.getPageTitle();
        }
        if (StringUtils.isBlank(tocTitle)) {
            tocTitle = currentPage.getName();
        }

        // Get Content Visibility;
        if (currentPage.getPath() != null && resourceResolver != null) {
            isShareViaEmailOnly = DITAUtils.isShareWithMail(currentPage.getPath(), resourceResolver);
        }

        final String[] excludedPaths = downloadPDFConfigService.getExcludedPaths();
        for (final String excludedPath : excludedPaths) {
            try {
                final Pattern pattern = Pattern.compile(excludedPath);
                final Matcher m = pattern.matcher(currentPage.getPath());
                if (m.matches()) {
                    disablePDFDownload = true;
                }
            } catch (final PatternSyntaxException e) {
                LOG.error("Error in pattern matching the exluded paths", e);
            }
        }

        final InheritanceValueMap inheritanceValueMap = new HierarchyNodeInheritanceValueMap(
                currentPage.getContentResource());
        final String disablePDFProperty = inheritanceValueMap.getInherited(DITAConstants.META_DISABLE_PDF_DWNLD, NO);
        if (YES.equals(disablePDFProperty)) {
            disablePDFDownload = true;
        }

        if (StringUtils.isBlank(currentPage.getProperties().get("downloadPDFPath", String.class))) {
            disablePDFDownload = true;
        }

        if(currentPage.getPath() != null && resourceResolver != null){
            hideSearchWithInDocument = DITAUtils.isHideSearchWithInDocIcon(currentPage.getPath(),resourceResolver);
            showStaticToc = DITAUtils.showStaticToc(currentPage.getPath(),resourceResolver);
        }
        
        runMode = MadisonUtil.getCurrentRunmode(slingSettingsService);
    }

    @Override
    public String getTocTitle() {
        return tocTitle;
    }

    @Override
    public Boolean isShareViaEmailOnly() {
        return isShareViaEmailOnly;
    }

    @Override
    public Boolean disablePDFDownload() {
        return disablePDFDownload;
    }

    @Override
    public Boolean hideSearchWithInDocument() {
        return hideSearchWithInDocument;
    }

    @Override
    public Boolean showStaticToc() {
        return showStaticToc;
    }

    @Override
    public String getRunMode() {
        return runMode;
    }

}
