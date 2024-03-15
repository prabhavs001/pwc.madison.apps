package com.pwc.madison.core.models.impl;

import java.util.Iterator;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.AccordionMobile;

/**
 * Sling model for Breadcrumb component.
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = AccordionMobile.class)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)

public class AccordionMobileImpl implements AccordionMobile {

    @ScriptVariable
    private Page currentPage;

    private static final String RIGHT_RAIL_PATH = DITAConstants.RIGHT_RAIL_NODE_PATH_V1;
    private static final String RIGHT_RAIL_PATH_MOBILE = MadisonConstants.FORWARD_SLASH + RIGHT_RAIL_PATH
            + MadisonConstants.FORWARD_SLASH + "responsivegrid";
    private static final String ACCORDION_RESOURCE_TYPE = "pwc-madison/components/content/accordion/accordion-container";

    @Override
    public String getAccordionContentPath() {
        return accordionContentPath;
    }

    private String accordionContentPath;

    /**
     * Init Method of Model.
     */
    @PostConstruct
    protected void init() {
        final String pagePath = currentPage.getPath();
        final Resource resource = currentPage.adaptTo(Resource.class);
        if (resource == null) {
            return;
        }
        final Resource childResource = resource.getChild(pagePath + RIGHT_RAIL_PATH_MOBILE);
        if (childResource == null) {
            return;
        }
        for (final Iterator<Resource> i = childResource.listChildren(); i.hasNext();) {
            final Resource child = i.next();
            if (null != child && child.isResourceType(ACCORDION_RESOURCE_TYPE)) {
                accordionContentPath = child.getPath();
                break;
            }
        }

    }

}
