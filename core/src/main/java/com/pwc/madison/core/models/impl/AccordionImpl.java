package com.pwc.madison.core.models.impl;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.Accordion;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Sling model representing accordion container component.
 */
@Model(
        adaptables = SlingHttpServletRequest.class,
        adapters = Accordion.class,
        resourceType = AccordionImpl.ACCORDION_RESOURCE_TYPE,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)

public class AccordionImpl implements Accordion {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccordionImpl.class);

    public static final String ACCORDION_RESOURCE_TYPE = "pwc-madison/components/content/accordion/accordion-container";

    private static final String SHOW_ACCORDION_REQUEST_ATTRIBUTE = "showAccordion";

    @Inject
    SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    @SlingObject
    private ResourceResolver resourceResolver;

    @SlingObject
    private Resource resource;

    private boolean show;

    @PostConstruct
    protected void init() throws RepositoryException {
        Boolean showRequestAttribute = (Boolean) request.getAttribute(SHOW_ACCORDION_REQUEST_ATTRIBUTE);
        if (null == showRequestAttribute) {
            if (isRelatedContentHidden()) {
                LOGGER.debug("AccordionImpl init() : RCL content is hidden from ditamap property for page {}",
                        currentPage.getPath());
                show = false;
            } else {
                show = checkIfRelatedLinkExists();
                LOGGER.debug("AccordionImpl init() : Is Related Links for Page {} exists {}", show,
                        currentPage.getPath());

            }
            request.setAttribute(SHOW_ACCORDION_REQUEST_ATTRIBUTE, show);
            LOGGER.debug("AccordionImpl init() : Calculated if RCL content hidden for page {} : {}",
                    currentPage.getPath(), show);
        } else {
            show = showRequestAttribute;
            LOGGER.debug(
                    "AccordionImpl init() : Extracted from Request Attribute if RCL content hidden for page {} : {}",
                    currentPage.getPath(), show);
        }
        // Hide the RCL for joined page
        if(currentPage.getPath().contains(DITAConstants.JOINED)) {
        	show = false;
        	LOGGER.debug("AccordionImpl init() : RCL content is hidden as this is a joined page and the page path is {}",
                    currentPage.getPath());
        }
    }

    /**
     * Check if related links exist or not. if any of the related content link exists, it returns true otherwise false.
     * 
     * @return {@link Boolean}
     */
    private boolean checkIfRelatedLinkExists() {
        for (final Resource relatedLink : DITAUtils.getRelatedLinkResources(currentPage, resourceResolver)) {
            for (final Resource linkedlist : relatedLink.getChildren()) {
                if (linkedlist.isResourceType(DITAConstants.COMPONENT_DITA_LINKLIST)) {
                    for (final Resource link : linkedlist.getChildren()) {
                        if (link.isResourceType(DITAConstants.COMPONENT_DITA_TYPE)
                                && link.getValueMap().containsKey(DITAConstants.PROPERTY_LINK)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 
     * @return
     */
    private boolean isRelatedContentHidden() {
        final String basePath = MadisonUtil.getBasePath(currentPage.getPath(), resourceResolver);
        if (StringUtils.isNotBlank(basePath)) {
            final Resource basePageResource = resourceResolver.getResource(basePath.concat(DITAConstants.JCR_CONTENT));
            if (null != basePageResource) {
                return MadisonConstants.YES.equals(
                        basePageResource.getValueMap().get(DITAConstants.PN_IS_PUBLISHING_POINTS, MadisonConstants.NO))
                        && MadisonConstants.YES.equals(basePageResource.getValueMap()
                                .get(DITAConstants.META_HIDE_RELATED_CONTENT, MadisonConstants.NO));
            }
        }
        return false;
    }

    @Override
    public boolean getShow() {
        return show;
    }

}
