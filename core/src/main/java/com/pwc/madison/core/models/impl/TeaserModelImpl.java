package com.pwc.madison.core.models.impl;

import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.TeaserModel;
import com.pwc.madison.core.util.DITAUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;

import javax.annotation.PostConstruct;

/**
 * Inloop Teaser Component Model Implementation
 */
@Model(
        adaptables = SlingHttpServletRequest.class,
        adapters = TeaserModel.class,
        resourceType = TeaserModelImpl.RESOURCE_TYPE,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class TeaserModelImpl implements TeaserModel {

     public static final String RESOURCE_TYPE = "pwc-madison/components/inloop/teaser";
    public static final String INTERNAL_USE_ONLY = "internal";


    @ScriptVariable
    Page currentPage;

     private String publicationDate;
     private String revisionDate;

     private String contentType = StringUtils.EMPTY;

     private String[] privateGroups = new String[0];
     private Boolean isInternal = false;

    /**
     * postconstruct method
     */
    @PostConstruct
    protected void init() {
        Resource currentPageContentResource = currentPage.getContentResource();
        final ValueMap valueMap = currentPageContentResource.getValueMap();
        contentType = valueMap.get(DITAConstants.META_CONTENT_TYPE, String.class);
        publicationDate = DITAUtils.formatDate(valueMap.get(DITAConstants.META_PUBLICATION_DATE, String.class), MadisonConstants.COMPONENTS_DATE_FORMAT);
        revisionDate = DITAUtils.formatDate(valueMap.get(DITAConstants.META_REVISION_DATE, String.class), MadisonConstants.COMPONENTS_DATE_FORMAT);
        if(StringUtils.equals(valueMap.get(DITAConstants.META_AUDIENCE, String.class), DITAConstants.AUDIENCE_PRIVATE)) {
            privateGroups = valueMap.get(DITAConstants.META_PRIVATE_GROUP, String[].class);
        } else if(StringUtils.equals(valueMap.get(DITAConstants.META_AUDIENCE, String.class), DITAConstants.AUDIENCE_INTERNAL_ONLY))
            isInternal = true;
    }

    /**
     * fetch Page publication date
     * @return publicationDate
     */
    @Override
    public String getPagePublicationDate() {
        return publicationDate;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public Boolean isInternal() {
        return isInternal;
    }

    @Override
    public String[] getPrivateGroups() {
        return privateGroups;
    }

    @Override
    public String getPageRevisionDate() {
        return revisionDate;
    }

}
