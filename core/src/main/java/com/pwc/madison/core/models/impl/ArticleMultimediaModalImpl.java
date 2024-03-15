package com.pwc.madison.core.models.impl;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.scene7.api.constants.Scene7Constants;
import com.day.cq.wcm.api.Page;
import com.drew.lang.StringUtil;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.ArticleMultimediaModal;
import com.pwc.madison.core.models.Territory;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Model(adaptables = SlingHttpServletRequest.class,
        adapters = ArticleMultimediaModal.class,
        resourceType = ArticleMultimediaModalImpl.RESOURCE_TYPE)
public class ArticleMultimediaModalImpl implements ArticleMultimediaModal {

    protected static final String RESOURCE_TYPE = "pwc-madison/components/inloop/multimedia-modal";

    private static final Logger LOGGER = LoggerFactory.getLogger(ArticleMultimediaModalImpl.class);

    private static final String DYNAMIC_MEDIA_RESOURCE_TYPE = "pwc-madison/components/ditacontent/dynamicmedia";

    private static final String QUERY = "SELECT * from [nt:unstructured] where ISDESCENDANTNODE('%s') and [%s] = '%s'";

    private static final String REVISED_DATE_FORMAT = "MMM dd, yyyy";

    private static final String ASSET_ID_PN = "assetID";

    private static final String FILE_REFERENCE = "fileReference";

    private static final String DAM_LENGTH = "dam:Length";

    private static final String DURATION = "duration";

    private static final String AUDIO = "audio";

    private static final String VIDEO = "video";

    @OSGiService
    private ResourceResolverFactory resourceResolverFactory;

    @OSGiService
    private CountryTerritoryMapperService countryTerritoryMapperService;

    @ScriptVariable
    private Page currentPage;

    private String countryLabel;

    private String contentId;

    private String contentType;

    private Boolean hidePublicationDate;

    private String formattedRevisedDate;

    private String viewDate;

    private final Map<String, List<String>> multimediaMap = new HashMap<>();

    @PostConstruct
    protected void init() {
        try (final ResourceResolver resolver = MadisonUtil.getResourceResolver(resourceResolverFactory, MadisonConstants.MADISON_READ_SUB_SERVICE)) {

            final String inlineLinksQuery = String.format(QUERY, currentPage.getPath(), ResourceResolver.PROPERTY_RESOURCE_TYPE, DYNAMIC_MEDIA_RESOURCE_TYPE);
            Iterator<Resource> dynamicMediaComponentIterator = resolver.findResources(inlineLinksQuery, Query.JCR_SQL2);
            boolean pageHasDynamicMediaComponent = false;
            while (dynamicMediaComponentIterator.hasNext()) {
                pageHasDynamicMediaComponent = true;
                List<String> multimediaMetadata = new ArrayList<>();
                Resource dynamicMediaResource = dynamicMediaComponentIterator.next();
                String nodePath = dynamicMediaResource.getPath();
                multimediaMetadata.add(nodePath);

                String fileReference = dynamicMediaResource.getValueMap().get(FILE_REFERENCE, String.class);
                Asset asset = MadisonUtil.getAssetFromPath(fileReference, resolver);
                String mimeType = asset != null ? asset.getMetadataValueFromJcr(DamConstants.DC_FORMAT) : null;
                String pageType = mimeType != null && mimeType.startsWith(AUDIO) ? AUDIO : VIDEO;
                multimediaMetadata.add(pageType);

                String assetId = dynamicMediaResource.getValueMap().get(ASSET_ID_PN, String.class);
                assetId = StringUtils.isBlank(assetId) && pageType.equals(AUDIO) ? asset.getMetadataValueFromJcr(Scene7Constants.PN_S7_FILE) : assetId;
                multimediaMetadata.add(assetId);

                String mediaLength = pageType.equals(AUDIO) ? asset.getMetadataValueFromJcr(DAM_LENGTH)
                        : resolver.getResource(fileReference).getChild(JcrConstants.JCR_CONTENT + MadisonConstants.FORWARD_SLASH + DamConstants.ACTIVITY_TYPE_METADATA)
                        .getValueMap().get(DURATION, "");
                long timeInSeconds = (long) Float.parseFloat(mediaLength);
                mediaLength = MadisonUtil.getTime(timeInSeconds);
                multimediaMetadata.add(mediaLength);

                multimediaMap.put(nodePath, multimediaMetadata);
            }
            if (pageHasDynamicMediaComponent) {

                final ValueMap properties = currentPage.getProperties();

                Date publicationDate = properties.get(DITAConstants.META_PUBLICATION_DATE, Date.class);
                String publicationDateString = properties.get(DITAConstants.META_PUBLICATION_DATE, String.class);

                String createdDateString = properties.get(JcrConstants.JCR_CREATED, String.class);

                Date revisedDate = properties.get(DITAConstants.META_REVISION_DATE, Date.class);
                String revisedDateString = properties.get(DITAConstants.META_REVISION_DATE, String.class);

                contentType = properties.get(DITAConstants.META_CONTENT_TYPE, String.class);

                contentId = properties.get(DITAConstants.META_CONTENT_ID, String.class);

                String territoryCode = MadisonUtil.getTerritoryCodeFromPagePath(currentPage.getPath());
                Territory territory = countryTerritoryMapperService.getTerritoryByTerritoryCode(territoryCode);
                countryLabel = territory.getTerritoryCode().toUpperCase();

                hidePublicationDate = DITAUtils.isHidePublicationDate(currentPage.getContentResource()).equals(MadisonConstants.YES);

                final String viewDateFormat = MadisonUtil.fetchDateFormat(countryLabel.toLowerCase(), countryTerritoryMapperService, REVISED_DATE_FORMAT);

                if (Objects.isNull(publicationDate)) {
                    publicationDateString = createdDateString;
                }

                String viewDateString;
                if (Objects.isNull(revisedDate)) {
                    viewDateString = publicationDateString;
                } else {
                    viewDateString = revisedDateString;
                    formattedRevisedDate = DITAUtils.formatDate(revisedDateString, viewDateFormat);
                }

                if (!hidePublicationDate) {
                    viewDate = DITAUtils.formatDate(viewDateString, viewDateFormat);
                }
            }

        }
    }

    @Override
    public Map<String, List<String>> getMultimediaMap() {
        return multimediaMap;
    }

    @Override
    public String getPageTitle() {
        return currentPage.getTitle();
    }

    @Override
    public String getCountryLabel() {
        return countryLabel;
    }

    @Override
    public String getContentId() {
        return contentId;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public Boolean getHidePublicationDate() {
        return hidePublicationDate;
    }

    @Override
    public String getFormattedRevisedDate() {
        return formattedRevisedDate;
    }

    @Override
    public String getViewDate() {
        return viewDate;
    }
}
