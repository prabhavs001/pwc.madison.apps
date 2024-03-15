package com.pwc.madison.core.models.impl;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.DocumentTitle;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { DocumentTitle.class },
    resourceType = DocumentTitleImpl.RESOURCE_TYPE)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)

public class DocumentTitleImpl implements DocumentTitle {

    public static final Logger LOGGER = LoggerFactory.getLogger(DocumentTitleImpl.class);
    private static final String PN_VALUE = "value";
    public static final String RESOURCE_TYPE = "pwc-madison/components/ditacontent/documenttitle";
    private static final String PN_TOPIC_TITLES = "topicTitles";
    private static final String AICPA_REF_RESCOURCE = "/content/pwc-madison/global/reference-data/metadata/standard-setter/items/aicpa";
    private static final String FASB_REF_RESOURCE = "/content/pwc-madison/global/reference-data/metadata/standard-setter/items/fasb";
    private static final String AICPA_DEFAULT_LOGO = "/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/AICPA_logo.png";
    private static final String FASB_DEFAULT_LOGO = "/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/FASB_logo.png";
    private static final String AICPA = "AICPA";
    private static final String FASB = "FASB";
    private static final String SOURCE_PATH = "sourcePath";

    @SlingObject
    private Resource resource;

    @ScriptVariable
    private Page currentPage;

    @OSGiService
    private CountryTerritoryMapperService countryTerritoryMapperService;

    @SlingObject
    private ResourceResolver resourceResolver;
    
    @Inject
    SlingHttpServletRequest request;

    private String sectionTitle = StringUtils.EMPTY;
    private String subTitle = StringUtils.EMPTY;
    private String publishedDate = StringUtils.EMPTY;
    private String revisedDate = StringUtils.EMPTY;
    private String imgPath = StringUtils.EMPTY;
    private Boolean isInternalUser;
    private String pwcCountry = StringUtils.EMPTY;
    private String source = StringUtils.EMPTY;
    private String contentType = StringUtils.EMPTY;
    private List<String> privateGroupList = null;
    boolean isPrivateUser;
    boolean hidePublicationDate = false;
    private boolean isJoinedTopic = false;

    @PostConstruct
    private void initModel() {
        hidePublicationDate = DITAUtils.isHidePublicationDate(currentPage.getContentResource()).equals(MadisonConstants.YES) ? true : false;
        sectionTitle = DITAUtils.getDITASectionTitle(currentPage, resourceResolver);
        if (request != null) {
            List<String> selectors = Arrays.asList(request.getRequestPathInfo().getSelectors());
            isJoinedTopic = selectors.contains("joinedsection");
        }
        final ValueMap metaMap = currentPage.getProperties();
        // THere's an Element mapping created which will inject every topic title within the map to a page level
        // property called 'topicTitles'
        // which is a multi field, the value at Index '0' will be the section title, which's the the title of first
        // topic.
        String[] topicTitles = null;
        if (currentPage.getProperties().get(PN_TOPIC_TITLES) != null) {
            topicTitles = currentPage.getProperties().get(PN_TOPIC_TITLES, String[].class);
            if (topicTitles != null && topicTitles.length > 0) {
                if(currentPage.getPath().matches(MadisonConstants.FASB_CONTENT_REGEX)){
                    if(!currentPage.getPath().contains(DITAConstants.JOINED)) {
                        subTitle = StringUtils.isNotBlank(DITAUtils.getDitaAncestryValue(currentPage, resourceResolver)) ? DITAUtils.getDitaAncestryValue(currentPage, resourceResolver) :
                                (StringUtils.isNotBlank(DITAUtils.getAncestryValueFromHeadNode(currentPage, resourceResolver)) ? DITAUtils.getAncestryValueFromHeadNode(currentPage, resourceResolver) : StringUtils.EMPTY);
                    }
                    if(StringUtils.isNoneBlank(DITAUtils.getLongTitle(currentPage, resourceResolver))) {
                        subTitle += " " + DITAUtils.getLongTitle(currentPage, resourceResolver);
                    }else{
                        subTitle += " " + currentPage.getProperties().get(DITAConstants.PN_PAGE_TITLE, String.class);
                    }
                }else {
                    subTitle = topicTitles[0];
                }
            }
        }

        if (currentPage.getProperties().get(SOURCE_PATH) != null) {
            final String ditaPath = currentPage.getProperties().get(SOURCE_PATH, String.class);
            pwcCountry = MadisonUtil.getTerritoryCodeForPath(ditaPath);
        }

        final String dateFormat = countryTerritoryMapperService.getTerritoryByTerritoryCode(pwcCountry).getDateFormat();
        LOGGER.debug("date format for the territory/country {} is  {}", pwcCountry, dateFormat);

        if (currentPage.getProperties().get(DITAConstants.META_STANDARD_SETTERS) != null) {
            source = currentPage.getProperties().get(DITAConstants.META_STANDARD_SETTERS, String.class);
        }

        if (currentPage.getProperties().get(DITAConstants.META_CONTENT_TYPE) != null) {
            contentType = currentPage.getProperties().get(DITAConstants.META_CONTENT_TYPE, String.class);
        }

        if (currentPage.getProperties().get(DITAConstants.META_AUDIENCE) != null) {
            final String audience = currentPage.getProperties().get(DITAConstants.META_AUDIENCE, String.class);
            if (StringUtils.isNotBlank(audience) && DITAConstants.AUDIENCE_PRIVATE.equals(audience)) {
                isPrivateUser = true;
                if (currentPage.getProperties().get(DITAConstants.META_PRIVATE_GROUP) != null) {
                    final String[] privateUserGroup = currentPage.getProperties().get(DITAConstants.META_PRIVATE_GROUP,
                            String[].class);
                    privateGroupList = Arrays.asList(privateUserGroup);
                }
            }
        }

        if (currentPage.getPath() != null && resourceResolver != null) {
            isInternalUser = DITAUtils.isShareWithMail(currentPage.getPath(), resourceResolver);
        }

        // if 'sectionTitle' is Null/Empty, you are on a normal topic page with no content chunking, so just use the
        // 'pageTitle' as the title.
        // so reset the 'subTitle' value with pagetitle PN_PAGE_TITLE
        if (StringUtils.isEmpty(sectionTitle)) {
            if(currentPage.getPath().matches(MadisonConstants.FASB_CONTENT_REGEX)){
                if(StringUtils.isEmpty(subTitle)) {
                    if (!currentPage.getPath().contains(DITAConstants.JOINED)) {
                        subTitle = StringUtils.isNotBlank(DITAUtils.getDitaAncestryValue(currentPage, resourceResolver)) ? DITAUtils.getDitaAncestryValue(currentPage, resourceResolver) :
                                (StringUtils.isNotBlank(DITAUtils.getAncestryValueFromHeadNode(currentPage, resourceResolver)) ? DITAUtils.getAncestryValueFromHeadNode(currentPage, resourceResolver) : StringUtils.EMPTY);
                    }
                    if (StringUtils.isNoneBlank(DITAUtils.getLongTitle(currentPage, resourceResolver))) {
                        subTitle += " " + DITAUtils.getLongTitle(currentPage, resourceResolver);
                    } else {
                        subTitle += " " + currentPage.getProperties().get(DITAConstants.PN_PAGE_TITLE, String.class);
                    }
                }
            }else {
                subTitle = currentPage.getProperties().get(DITAConstants.PN_PAGE_TITLE, String.class);
            }
            sectionTitle = metaMap.containsKey(DITAConstants.META_CONTENT_ID)
                    ? metaMap.get(DITAConstants.META_CONTENT_ID, String.class)
                    : StringUtils.EMPTY;
        }

        // Fetch the published date from the created date of the DITA MAP
        publishedDate = DITAUtils.getDITAMAPPublishedDate(currentPage, resourceResolver);

        if (StringUtils.isBlank(publishedDate) && StringUtils.isNotBlank(dateFormat)) {
            final String revisionDate = metaMap.containsKey(DITAConstants.META_REVISION_DATE)
                    ? metaMap.get(DITAConstants.META_REVISION_DATE, String.class)
                    : StringUtils.EMPTY;
            final String publicationDate = metaMap.containsKey(DITAConstants.META_PUBLICATION_DATE)
                    ? metaMap.get(DITAConstants.META_PUBLICATION_DATE, String.class)
                    : StringUtils.EMPTY;
            publishedDate = DITAUtils.formatDate(publicationDate, dateFormat);
            if (StringUtils.isNotBlank(revisionDate) && !revisionDate.equals(publicationDate)) {
                revisedDate = DITAUtils.formatDate(revisionDate, dateFormat);
            }
        }

        imgPath = getImagePath();
    }

    // TODO :: Remove This after Creating Homepage template by adding the img path to homepage page properties.
    private String getImagePath() {
        String imgUrl = StringUtils.EMPTY;
        String source;
        if (null != currentPage && null != currentPage.getProperties()) {
            imgUrl = currentPage.getProperties().get(DITAConstants.META_TOPIC_IMAGE, String.class);
            source = currentPage.getProperties().get(DITAConstants.META_STANDARD_SETTERS, String.class);
            if (StringUtils.isNoneBlank(imgUrl)) {
                return imgUrl;
            }
            if (StringUtils.isNotBlank(source)) {
                final Resource aicpaRefRes = resourceResolver.getResource(AICPA_REF_RESCOURCE);
                final Resource fasbRefRes = resourceResolver.getResource(FASB_REF_RESOURCE);
                if (null == aicpaRefRes || null == fasbRefRes) {
                    return imgUrl;
                }
                final ValueMap aicpaValueMap = aicpaRefRes.getValueMap();
                final ValueMap fasbValueMap = fasbRefRes.getValueMap();
                if (source.equalsIgnoreCase(aicpaValueMap.get(PN_VALUE, String.class))) {
                    imgUrl = AICPA_DEFAULT_LOGO;
                } else if (source.equalsIgnoreCase(fasbValueMap.get(PN_VALUE, String.class))) {
                    imgUrl = FASB_DEFAULT_LOGO;
                }
            } else {
                /* If there is no source value then path will be used */
                final String sourcePath = currentPage.getProperties().get("sourcePath", String.class);
                if (StringUtils.isBlank(sourcePath)) {
                    return imgUrl;
                }
                if (sourcePath.matches(MadisonConstants.AICPA_REGEX)) {
                    imgUrl = AICPA_DEFAULT_LOGO;
                } else if (sourcePath.matches(MadisonConstants.FASB_REGEX)) {
                    imgUrl = FASB_DEFAULT_LOGO;
                }
            }
        }
        return imgUrl;
    }

    /**
     * Convert the given date format to PwC standard format (example 01 Jan 2000).
     *
     * @param inputeDate
     * @return
     * @throws ParseException
     */
    public static String getMadisonRevisedDateString(final String inputeDate) {

        if (StringUtils.isBlank(inputeDate)) {
            return inputeDate;
        }
        try {
            final java.util.Date outputDate = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sssX")
                    .parse(inputeDate);
            return new java.text.SimpleDateFormat("d/M/yyyy").format(outputDate);
        } catch (final Exception e) {
            LOGGER.error("Error formatting the given date " + inputeDate + e.getMessage());
        }
        return inputeDate;
    }

    @Override
    public String getSectionTitle() {
        return sectionTitle;
    }

    @Override
    public String getSubTitle() {
        return subTitle;
    }

    @Override
    public String getPubhlishedDate() {
        return publishedDate;
    }

    @Override
    public String getRevisedDate() {
        return revisedDate;
    }

    @Override
    public String getImgPath() {
        return imgPath;
    }

    @Override
    public Boolean getIsInternalUser() {
        return isInternalUser;
    }

    @Override
    public String getPwcCountry() {
        return pwcCountry;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getPwcSourceValue() {
        return DITAConstants.PWC_SOURCE_VALUE;
    }

    @Override
    public List<String> getPrivateGroupList() {
        return privateGroupList;
    }

    @Override
    public boolean isPrivateUser() {
        return isPrivateUser;
    }

    @Override
    public boolean isHidePublicationDate() {
        return hidePublicationDate;
    }
    
    @Override
    public boolean isJoinedTopic() {
        return isJoinedTopic;
    }
}
