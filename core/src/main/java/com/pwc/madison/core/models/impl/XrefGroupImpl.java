package com.pwc.madison.core.models.impl;

import com.adobe.fmdita.custom.common.LinkUtils;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.XrefGroup;
import com.pwc.madison.core.services.XrefGroupService;
import com.pwc.madison.core.util.DITAUtils;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Model(
        adaptables = { SlingHttpServletRequest.class },
        adapters = { XrefGroup.class },
        resourceType = { XrefGroupImpl.RESOURCE_TYPE },
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class XrefGroupImpl implements XrefGroup{

    protected static final String RESOURCE_TYPE = "pwc-madison/components/ditacontent/xrefgroup";
    private static final Logger LOGGER = LoggerFactory.getLogger(XrefGroupImpl.class);
    public static final String THROUGH_STR = " through ";

    private Iterator<Resource> resourceChildren;
    private String anchorText = StringUtils.EMPTY;
    private String anchorLink = StringUtils.EMPTY;

    @SlingObject
    private Resource resource;

    @ScriptVariable
    ResourceResolver resolver;

    @OSGiService
    XrefGroupService xrefGroupService;

    @PostConstruct
    protected void init() {
        if(null != resource && resource.hasChildren()) {
            resourceChildren = resource.listChildren();
        }
        List<Resource> xrefList = getXrefList(resourceChildren);

        fetchAnchorLink(xrefList);
        if(StringUtils.isNotEmpty(anchorLink)) {
            fetchAnchorText(xrefList);
        }
    }

    private List<Resource> getXrefList(Iterator<Resource> resourceChildren) {
        List<Resource> childList = IteratorUtils.toList(resourceChildren);
        return childList.stream().filter(res -> res.getName().contains(DITAConstants.DITA_TAG_XREF)).collect(Collectors.toList());
    }

    private void fetchAnchorLink(List<Resource> xrefList) {
        if(resourceChildren != null) {
            Resource xrefResource = xrefList.get(0);
            String pageLink = xrefResource.getValueMap().getOrDefault(DITAConstants.PROPERTY_LINK, StringUtils.EMPTY).toString();
            String ext = FilenameUtils.getExtension(pageLink);
            if (ext.equalsIgnoreCase(DITAConstants.DITA_EXTENSION)){
                LOGGER.info("Link has dita extension");
                return;
            }
            if(pageLink!=null) {
                ext = FilenameUtils.getExtension(pageLink);
                if(pageLink.indexOf("#") > 0) {
                    ext =   FilenameUtils.getExtension(pageLink.substring(0,pageLink.indexOf("#")));
                    if (ext.isEmpty() && !pageLink.isEmpty()) {
                        pageLink = pageLink.substring(0,pageLink.indexOf("#")) + ".html" + pageLink.substring(pageLink.indexOf("#"));
                    }
                } else if(ext.isEmpty() && !pageLink.isEmpty()) {
                    pageLink = pageLink + ".html";
                }
            }
            anchorLink=pageLink;
        }
    }

    protected void fetchAnchorText(List<Resource> xrefList){
        if(resourceChildren != null){
            StringBuilder anchorTextBuilder = new StringBuilder();

            //1. get page's anchor text
            Resource firstXrefResource = xrefList.get(0);
            String pageAnchorFullText = getPageAnchorText(firstXrefResource);
            if(pageAnchorFullText.isEmpty()){
                return;
            }
            String pageAnchorText = pageAnchorFullText.substring(0, pageAnchorFullText.lastIndexOf(MadisonConstants.HYPHEN));

            if(StringUtils.isNotEmpty(pageAnchorText)) {
                anchorTextBuilder.append(pageAnchorText);

                //2. get section's anchor text for 0th xref element
                String firstSectionAnchorText = xrefGroupService.getReferencedSectionAnchorText(firstXrefResource);
                if (StringUtils.isNotEmpty(firstSectionAnchorText)) {
                    anchorTextBuilder.append(MadisonConstants.HYPHEN).append(firstSectionAnchorText);

                    //3. get section's anchor text for last xref element
                    Resource lastXrefResource = xrefList.get(xrefList.size() - 1);
                    String lastSectionAnchorText = xrefGroupService.getReferencedSectionAnchorText(lastXrefResource);
                    if (StringUtils.isNotEmpty(lastSectionAnchorText)) {
                        anchorTextBuilder.append(THROUGH_STR).append(lastSectionAnchorText);
                    }
                }
            }
            anchorText = anchorTextBuilder.toString();
        }
    }

    private String getPageAnchorText(Resource xrefResource) {
        String pageFullLink = xrefResource.getValueMap().getOrDefault(DITAConstants.PROPERTY_LINK, StringUtils.EMPTY).toString();
        String pageAnchorText = StringUtils.EMPTY;

        if(pageFullLink.contains(DITAConstants.DITA_EXTENSION)) {
            LOGGER.info("Link contains Dita extension, hence omitted");
            return StringUtils.EMPTY;
        }
        String pageLinkWithExtension = pageFullLink.split(DITAConstants.HASH_STR)[0];

        // Add Dita link check
        // Add if there is no pagelink ahead or relative path, then consider it current page.

        String pageLink = pageLinkWithExtension.split(DITAConstants.HTML_EXT)[0];

        Page referencedPage = resolver.getResource(pageLink).adaptTo(Page.class);
        if (Objects.nonNull(referencedPage)) {
            pageAnchorText = DITAUtils.getDitaAncestryValue(referencedPage, resolver);
        }
        return pageAnchorText;
    }

    @Override
    public String getAnchorText() {
        return anchorText;
    }

    @Override
    public String getAnchorLink() {
        return anchorLink;
    }
}
