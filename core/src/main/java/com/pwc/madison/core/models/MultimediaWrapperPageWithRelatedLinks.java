package com.pwc.madison.core.models;

import com.day.cq.commons.DownloadResource;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.authorization.enums.AudienceType;
import com.pwc.madison.core.authorization.models.AuthorizationInformation;
import com.pwc.madison.core.authorization.models.ContentAuthorization;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents multimedia wrapper page with related links.
 *
 * @see MultimediaWrapperPage
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class MultimediaWrapperPageWithRelatedLinks {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultimediaWrapperPageWithRelatedLinks.class);

    private static final String RELATED_CONTENT_LINK_LIST_TYPE = "inline-links";
    private static final String COMPONENT_DITA_LINKLIST = "fmdita/components/dita/linklist";
    private static final String COMPONENT_DITA_TYPE = "fmdita/components/dita/link";
    private static final String TYPE_PROPERTY_NAME = "type";
    private static final String LINK_PROPERTY_NAME = "link";
    private static final String RELATED_LINKS_RELATIVE_PATH_FROM_PAGE = "root/maincontainer/readerrow/bodycontainer/docreader/contentbody/topicbody/pwc-topic/related-links";

    private static final String DM_COMPONENT_RELATIVE_PATH_FROM_PAGE = "root/maincontainer/readerrow/bodycontainer/docreader/contentbody/topicbody/pwc-topic/pwc-body/bodydiv/object";
    private static final String ASSET_ID_PN = "assetID";
    private static final String DAM_LENGTH_PN = "dam:Length";
    private static final String DURATION_PN = "duration";
    private static final String AUDIO_TYPE = "audio";
    private static final String VIDEO_TYPE = "video";

    @SlingObject
    private Resource currentResource;

    @Self
    private MultimediaWrapperPage wrapperPageModel;

    @OSGiService
    private ResourceResolverFactory resourceResolverFactory;

    private ResourceResolver resolver;

    private Page multimediaWrapperPage;

    private List<MultimediaWrapperPage> relatedContentList;

    private String dmComponentPath;
    private String assetId;
    private String mediaLength;
    private String pageType = "text";

    private AuthorizationInformation authorizationInformation;

    private boolean internalUseOnly = false;

    @PostConstruct
    protected void init() {

        resolver = MadisonUtil.getResourceResolver(resourceResolverFactory, MadisonConstants.MADISON_READ_SUB_SERVICE);

        multimediaWrapperPage = currentResource.getParent().adaptTo(Page.class);
        if (Objects.isNull(multimediaWrapperPage)) {
            LOGGER.error("Authored path {} is not a page", currentResource.getPath());
            return;
        }

        ContentAuthorization contentAuthorization = MadisonUtil.getPageContentAuthorization(multimediaWrapperPage);
        String multimediaWrapperPageAudienceType = contentAuthorization.getAudienceType();
        if (AudienceType.INTERNAL_ONLY.getValue().equals(multimediaWrapperPageAudienceType) || AudienceType.PRIVATE_GROUP.getValue().equals(multimediaWrapperPageAudienceType)) {
            internalUseOnly = true;
        }

        String pagePath = multimediaWrapperPage.getPath();

        Resource dynamicMediaResource = multimediaWrapperPage.getContentResource(DM_COMPONENT_RELATIVE_PATH_FROM_PAGE);
        if (Objects.nonNull(dynamicMediaResource)) {
            dmComponentPath = dynamicMediaResource.getPath();
            ValueMap dynamicMediaValueMap = dynamicMediaResource.getValueMap();
            assetId = dynamicMediaValueMap.get(ASSET_ID_PN, String.class);

            String assetPath = dynamicMediaValueMap.get(DownloadResource.PN_REFERENCE, String.class);
            Asset asset = MadisonUtil.getAssetFromPath(assetPath, resolver);
            if (Objects.nonNull(asset)) {
                String mimeType = asset.getMetadataValueFromJcr(DamConstants.DC_FORMAT);
                if (mimeType.startsWith(AUDIO_TYPE)) {
                    mediaLength = asset.getMetadataValueFromJcr(DAM_LENGTH_PN);
                    setMediaLengthInFormat();
                    pageType = AUDIO_TYPE;
                } else if (mimeType.startsWith(VIDEO_TYPE)) {
                    //duration property is not part of Asset API metadata
                    ValueMap assetMetadataValueMap = resolver.getResource(assetPath).getChild(JcrConstants.JCR_CONTENT + MadisonConstants.FORWARD_SLASH + DamConstants.ACTIVITY_TYPE_METADATA).adaptTo(ValueMap.class);
                    mediaLength = assetMetadataValueMap.get(DURATION_PN, "");
                    setMediaLengthInFormat();
                    pageType = VIDEO_TYPE;
                }
            }
        }
        LOGGER.debug("dmComponent Path: {}, assetId: {} for {}", new Object[]{dmComponentPath, assetId, pagePath});

        setRelatedContentList();

        resolver.close();
    }

    private void setMediaLengthInFormat() {
        try {
            long timeInSeconds = (long) Float.parseFloat(mediaLength);
            mediaLength = MadisonUtil.getTime(timeInSeconds);
        } catch (NumberFormatException e) {
            LOGGER.error("Error in parsing float from mediaLength property, DM component path: {}, {}", dmComponentPath, e);
        }
    }

    /**
     * Populate related content list
     */
    private void setRelatedContentList() {
        relatedContentList = new ArrayList<>();
        Resource relatedLinksResource = multimediaWrapperPage.getContentResource(RELATED_LINKS_RELATIVE_PATH_FROM_PAGE);
        if (Objects.nonNull(relatedLinksResource)) {
            for (Resource linkListResource : relatedLinksResource.getChildren()) {
                if (COMPONENT_DITA_LINKLIST.equals(linkListResource.getResourceType())) {
                    String linkListType = linkListResource.getValueMap().get(TYPE_PROPERTY_NAME, String.class);
                    if (RELATED_CONTENT_LINK_LIST_TYPE.equals(linkListType)) {
                        for (Resource linkResource : linkListResource.getChildren()) {
                            setRelatedContent(linkResource);
                        }
                    }
                }
            }
        }
        LOGGER.debug("Related content list for {} is: {}", wrapperPageModel.getPagePath(), relatedContentList);
    }

    private void setRelatedContent(Resource linkResource) {
        if (COMPONENT_DITA_TYPE.equals(linkResource.getResourceType())) {
            String link = linkResource.getValueMap().get(LINK_PROPERTY_NAME, String.class);
            if (StringUtils.isNotBlank(link)) {
                Resource relatedContentResource = resolver.getResource(link + MadisonConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
                if (Objects.nonNull(relatedContentResource)) {
                    relatedContentList.add(relatedContentResource.adaptTo(MultimediaWrapperPage.class));
                }
            }
        }

    }

    public MultimediaWrapperPage getWrapperPageModel() {
        return wrapperPageModel;
    }

    public List<MultimediaWrapperPage> getRelatedContentList() {
        return relatedContentList;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getDmComponentPath() {
        return dmComponentPath;
    }

    public String getMediaLength() {
        return mediaLength;
    }

    public String getPageType() {
        return pageType;
    }

    public AuthorizationInformation getAuthorizationInformation() {
        return authorizationInformation;
    }

    public void setAuthorizationInformation(AuthorizationInformation authorizationInformation) {
        this.authorizationInformation = authorizationInformation;
    }

    /**
     * @return true for page with audience type {@link AudienceType#INTERNAL_ONLY} or {@link AudienceType#PRIVATE_GROUP}
     */
    public boolean isInternalUseOnly() {
        return internalUseOnly;
    }

    @Override
    public String toString() {
        return "MultimediaWrapperPageWithRelatedLinks{" +
            "wrapperPageModel=" + wrapperPageModel +
            ", relatedContentList=" + relatedContentList +
            ", pageType='" + pageType + '\'' +
            '}';
    }
}
