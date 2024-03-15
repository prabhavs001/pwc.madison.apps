package com.pwc.madison.core.models;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.Objects;

/**
 * Represents multimedia wrapper page metadata with dynamic media component path and asset id.
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class MultimediaWrapperPage {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultimediaWrapperPage.class);

    private static final String REVISED_DATE_FORMAT = "MMM dd, yyyy";

    @SlingObject
    private Resource currentResource;

    @ValueMapValue(name = DITAConstants.META_CONTENT_ID)
    private String contentId;

    @ValueMapValue(name = DITAConstants.META_CONTENT_TYPE)
    private String contentType;

    @ValueMapValue(name = JcrConstants.JCR_CREATED)
    private Date createdDate;

    @ValueMapValue(name = JcrConstants.JCR_CREATED)
    private String createdDateString;

    @ValueMapValue(name = DITAConstants.META_PUBLICATION_DATE)
    private Date publicationDate;

    @ValueMapValue(name = DITAConstants.META_PUBLICATION_DATE)
    private String publicationDateString;

    @ValueMapValue(name = DITAConstants.META_REVISION_DATE)
    private Date revisedDate;

    @ValueMapValue(name = DITAConstants.META_REVISION_DATE)
    private String revisedDateString;

    private String pageTitle = StringUtils.EMPTY;
    private String pageDesc = StringUtils.EMPTY;
    private String pagePath;
    private String wrapperPageLink;
    private Page multimediaWrapperPage;
    private String countryLabel;
    private String formattedRevisedDate;
    private String viewDate;
    private boolean hidePublicationDate;

    @OSGiService
    private CountryTerritoryMapperService countryTerritoryMapperService;

    @PostConstruct
    protected void init() {

        multimediaWrapperPage = currentResource.getParent().adaptTo(Page.class);
        if (Objects.isNull(multimediaWrapperPage)) {
            LOGGER.error("Authored path {} is not a page", currentResource.getPath());
            return;
        }

        pagePath = multimediaWrapperPage.getPath();

        pageTitle = MadisonUtil.getPageTitle(multimediaWrapperPage);

        pageDesc = multimediaWrapperPage.getDescription();

        if (Objects.isNull(publicationDate)) {
            publicationDate = createdDate;
            publicationDateString = createdDateString;
        }

        String territoryCode = MadisonUtil.getTerritoryCodeFromPagePath(pagePath);
        Territory territory = countryTerritoryMapperService.getTerritoryByTerritoryCode(territoryCode);
        if (Objects.nonNull(territory)) {
            countryLabel = territory.getTerritoryCode();
            if(StringUtils.isNotEmpty(countryLabel)){
                countryLabel = countryLabel.toUpperCase();
            }
        }

        wrapperPageLink = pagePath + MadisonConstants.HTML_EXTN;
        
        final String territoryLowerCase = territory != null ? territory.getTerritoryCode().toLowerCase() : StringUtils.EMPTY;

        String viewDateString;
        if (Objects.isNull(revisedDate)) {
            viewDateString = publicationDateString;
        } else {
            viewDateString = revisedDateString;
            final String revisedDateFormat = MadisonUtil.fetchDateFormat(territoryLowerCase, countryTerritoryMapperService, REVISED_DATE_FORMAT);
            formattedRevisedDate = DITAUtils.formatDate(revisedDateString, revisedDateFormat);
        }
        hidePublicationDate = DITAUtils.isHidePublicationDate(multimediaWrapperPage.getContentResource()).equals(MadisonConstants.YES) ? true : false;
        if (!hidePublicationDate) {
            final String viewDateFormat = MadisonUtil.fetchDateFormat(territoryLowerCase, countryTerritoryMapperService, MadisonConstants.COMPONENTS_DATE_FORMAT);
            viewDate = DITAUtils.formatDate(viewDateString, viewDateFormat);
		}

    }

    public String getPageTitle() {
        return pageTitle;
    }

    public String getPageDesc() {
        return pageDesc;
    }

    public String getContentId() {
        return contentId;
    }

    public String getContentType() {
        return contentType;
    }

    public String getCountryLabel() {
        return countryLabel;
    }

    public Date getRevisedDate() {
        return revisedDate;
    }
    
    public boolean getHidePublicationDate() {
        return hidePublicationDate;
    }

    public Date getPublicationDate() {
        return publicationDate;
    }

    /**
     * @return Revision date if exists, or publication date of the multimedia content.
     */
    public String getViewDate() {
        return viewDate;
    }

    public String getPagePath() {
        return pagePath;
    }

    public String getWrapperPageLink() {
        return wrapperPageLink;
    }

    public String getFormattedRevisedDate() {
        return formattedRevisedDate;
    }

    @Override
    public String toString() {
        return "MultimediaWrapperPage{" +
            "pagePath='" + pagePath + '\'' +
            '}';
    }
}
