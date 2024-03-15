package com.pwc.madison.core.models.impl;

import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.granite.crypto.CryptoSupport;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.pwc.madison.core.authorization.models.AuthorizationInformation;
import com.pwc.madison.core.authorization.services.ContentAuthorizationService;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.MultimediaModal;
import com.pwc.madison.core.models.MultimediaWrapperPage;
import com.pwc.madison.core.models.MultimediaWrapperPageWithRelatedLinks;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.models.User;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Model(adaptables = SlingHttpServletRequest.class,
    adapters = MultimediaModal.class,
    resourceType = MultimediaModalImpl.RESOURCE_TYPE)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class MultimediaModalImpl implements MultimediaModal {

    protected static final String RESOURCE_TYPE = "pwc-madison/components/content/multimedia-modal";
    private static final Logger LOGGER = LoggerFactory.getLogger(MultimediaModal.class);

    private static final String PWC_BODY_PATH_FROM_PAGE = "/jcr:content/root/maincontainer/readerrow/bodycontainer/docreader/contentbody/topicbody/pwc-topic/pwc-body";
    private static final String MEDIA_WRAPPER_RESOURCE_TYPE = "pwc-madison/components/ditacontent/mediawrapper";
    private static final String MEDIA_WRAPPER_LINK_PROPERTY = "link";

    private static final String INLINE_LINKS_QUERY = "SELECT * from [nt:unstructured] where ISDESCENDANTNODE('%s') and [%s] = '%s'";

    @ScriptVariable
    private SlingHttpServletRequest request;

    @ScriptVariable
    private SlingHttpServletResponse response;

    @ScriptVariable
    private Page currentPage;

    @OSGiService
    private ResourceResolverFactory resourceResolverFactory;

    @OSGiService
    private UserRegRestService userRegRestService;

    @OSGiService
    private CryptoSupport cryptoSupport;

    @OSGiService
    private CountryTerritoryMapperService countryTerritoryMapperService;

    @OSGiService
    private UserPreferencesProviderService userPreferencesProviderService;

    @OSGiService(injectionStrategy = InjectionStrategy.OPTIONAL)
    private ContentAuthorizationService contentAuthorizationService;
    
    @OSGiService
    private transient XSSAPI xssapi;

    private ResourceResolver resolver;
    private PageManager pageManager;
    User currentUser;
    private List<List<MultimediaWrapperPageWithRelatedLinks>> multimediaModalsList = new ArrayList<>();

    @PostConstruct
    protected void init() {

        resolver = MadisonUtil.getResourceResolver(resourceResolverFactory, MadisonConstants.MADISON_READ_SUB_SERVICE);
        pageManager = resolver.adaptTo(PageManager.class);

        currentUser =
            UserInformationUtil.getUser(request, false, userRegRestService, cryptoSupport, response,
                true, countryTerritoryMapperService, userPreferencesProviderService, false, false, xssapi);

        MultimediaWrapperPageWithRelatedLinks multimediaWrapperPageWithRelatedLinks = getMultimediaWrapperPageWithRelatedLinksFromPagePath(currentPage.getPath());
        if (Objects.nonNull(multimediaWrapperPageWithRelatedLinks)) {

            //Add modal for the current page media (in case of multimedia wrapper page)
            List<MultimediaWrapperPageWithRelatedLinks> currentPageMediaModalList = getMediaWithRelatedMediaList(multimediaWrapperPageWithRelatedLinks);
            if (Objects.nonNull(currentPageMediaModalList)) {
                multimediaModalsList.add(currentPageMediaModalList);
            }

            //Add modals for RCL
            for (MultimediaWrapperPage multimediaWrapperPage : multimediaWrapperPageWithRelatedLinks.getRelatedContentList()) {
                List<MultimediaWrapperPageWithRelatedLinks> relatedContentMediaModalList = getMediaWithRelatedMediaList(multimediaWrapperPage.getPagePath());
                if (Objects.nonNull(relatedContentMediaModalList)) {
                    multimediaModalsList.add(relatedContentMediaModalList);
                }
            }

            //Add modals for inline media links
            List<String> inlineMediaLinks = getInlineMediaLinks(currentPage.getPath());
            for (String wrapperPagePath : inlineMediaLinks) {
                List<MultimediaWrapperPageWithRelatedLinks> inlineMediaModalList = getMediaWithRelatedMediaList(wrapperPagePath);
                if (Objects.nonNull(inlineMediaModalList)) {
                    multimediaModalsList.add(inlineMediaModalList);
                }
            }
        }

        resolver.close();
    }

    /**
     * @param pagePath path of the multimedia wrapper page
     * @return List of media with related media of same type, null if the current user doesn't have access to the given page.
     */
    @Nullable
    private List<MultimediaWrapperPageWithRelatedLinks> getMediaWithRelatedMediaList(String pagePath) {
        MultimediaWrapperPageWithRelatedLinks multimediaWrapperPageWithRelatedLinks = getMultimediaWrapperPageWithRelatedLinksFromPagePath(pagePath);
        return getMediaWithRelatedMediaList(multimediaWrapperPageWithRelatedLinks);
    }

    /**
     * @param multimediaWrapperPageWithRelatedLinks {@link MultimediaWrapperPageWithRelatedLinks}
     * @return List of media with related media of same type, null if the current user doesn't have access to the given page.
     */
    @Nullable
    private List<MultimediaWrapperPageWithRelatedLinks> getMediaWithRelatedMediaList(MultimediaWrapperPageWithRelatedLinks multimediaWrapperPageWithRelatedLinks) {
        List<MultimediaWrapperPageWithRelatedLinks> mediaWithRelatedMediaList = null;
        if (Objects.nonNull(multimediaWrapperPageWithRelatedLinks) && StringUtils.isNotEmpty(multimediaWrapperPageWithRelatedLinks.getDmComponentPath())) {
            mediaWithRelatedMediaList = new ArrayList<>();

            setAuthInformationInMultimediaWrapperPage(multimediaWrapperPageWithRelatedLinks);
            if (!multimediaWrapperPageWithRelatedLinks.getAuthorizationInformation().isAuthorized()) {
                return null;
            }

            mediaWithRelatedMediaList.add(multimediaWrapperPageWithRelatedLinks);
            for (MultimediaWrapperPage multimediaWrapperPage : multimediaWrapperPageWithRelatedLinks.getRelatedContentList()) {
                MultimediaWrapperPageWithRelatedLinks relatedContentMultimediaWrapperPageWithRelatedLinks = getMultimediaWrapperPageWithRelatedLinksFromPagePath(multimediaWrapperPage.getPagePath());
                if (Objects.nonNull(relatedContentMultimediaWrapperPageWithRelatedLinks) && multimediaWrapperPageWithRelatedLinks.getPageType().equals(relatedContentMultimediaWrapperPageWithRelatedLinks.getPageType())) {

                    setAuthInformationInMultimediaWrapperPage(relatedContentMultimediaWrapperPageWithRelatedLinks);
                    mediaWithRelatedMediaList.add(relatedContentMultimediaWrapperPageWithRelatedLinks);
                }
            }
        }
        LOGGER.debug("Media list for {}: {}", multimediaWrapperPageWithRelatedLinks, mediaWithRelatedMediaList);
        return mediaWithRelatedMediaList;
    }

    /**
     * @param pagePath String
     * @return List of inline media links for the given page
     */
    private List<String> getInlineMediaLinks(String pagePath) {
        List<String> inlineMediaLinks = new ArrayList<>();
        if (StringUtils.isNotBlank(pagePath)) {
            Resource pwcBodyResource = resolver.getResource(pagePath + PWC_BODY_PATH_FROM_PAGE);
            if (Objects.nonNull(pwcBodyResource)) {

                String inlineLinksQuery = String.format(INLINE_LINKS_QUERY, pwcBodyResource.getPath(), ResourceResolver.PROPERTY_RESOURCE_TYPE, MEDIA_WRAPPER_RESOURCE_TYPE);

                Iterator<Resource> inlineLinksResourceIterator = resolver.findResources(inlineLinksQuery, Query.JCR_SQL2);
                while (inlineLinksResourceIterator.hasNext()) {
                    Resource inlineLinksResource = inlineLinksResourceIterator.next();
                    String wrapperPageLink = inlineLinksResource.getValueMap().get(MEDIA_WRAPPER_LINK_PROPERTY, String.class);
                    if (StringUtils.isNotEmpty(wrapperPageLink)) {
                        inlineMediaLinks.add(wrapperPageLink);
                    }
                }
            }
        }

        LOGGER.debug("Inline media links for {}: {}", pagePath, inlineMediaLinks);
        return inlineMediaLinks;
    }


    @Nullable
    private MultimediaWrapperPageWithRelatedLinks getMultimediaWrapperPageWithRelatedLinksFromPagePath(String pagePath) {
        MultimediaWrapperPageWithRelatedLinks multimediaWrapperPageWithRelatedLinks = null;
        if (StringUtils.isNotBlank(pagePath)) {
            Page wrapperPage = pageManager.getPage(pagePath);
            if (Objects.nonNull(wrapperPage)) {
                Resource wrapperPageContentResource = wrapperPage.getContentResource();
                if (Objects.nonNull(wrapperPageContentResource)) {
                    multimediaWrapperPageWithRelatedLinks = wrapperPageContentResource.adaptTo(MultimediaWrapperPageWithRelatedLinks.class);
                }
            }
        }
        LOGGER.debug("multimediaWrapperPageWithRelatedLinks for page: {} is {}", multimediaWrapperPageWithRelatedLinks, pagePath);
        return multimediaWrapperPageWithRelatedLinks;
    }

    /**
     * Set {@link AuthorizationInformation} for the given {@link MultimediaWrapperPageWithRelatedLinks} to check whether
     * the video is accessible to current user or not. Also filter the related pages list for the current user.
     *
     * @param multimediaWrapperPageWithRelatedLinks {@link MultimediaWrapperPageWithRelatedLinks}
     */
    private void setAuthInformationInMultimediaWrapperPage(@Nonnull MultimediaWrapperPageWithRelatedLinks multimediaWrapperPageWithRelatedLinks) {
        if (Objects.isNull(contentAuthorizationService)) {
            multimediaWrapperPageWithRelatedLinks.setAuthorizationInformation(new AuthorizationInformation(true, false, false));
        } else {
            Page page = pageManager.getPage(multimediaWrapperPageWithRelatedLinks.getWrapperPageModel().getPagePath());
            if (Objects.nonNull(page)) {
                AuthorizationInformation pageAuthorizationInformation = contentAuthorizationService.getUserAuthorization(page, currentUser);
                multimediaWrapperPageWithRelatedLinks.setAuthorizationInformation(pageAuthorizationInformation);
            }

            //remove unauthorised related pages
            multimediaWrapperPageWithRelatedLinks.getRelatedContentList().removeIf(multimediaWrapperPage -> {
                String pagePath = multimediaWrapperPage.getPagePath();
                Page relatedPage = pageManager.getPage(pagePath);
                if (Objects.isNull(relatedPage)) {
                    return true;
                }
                AuthorizationInformation authorizationInformation = contentAuthorizationService.getUserAuthorization(relatedPage, currentUser);
                return !authorizationInformation.isAuthorized();
            });

        }
    }

    @Override
    public List<List<MultimediaWrapperPageWithRelatedLinks>> getMultimediaModalsList() {
        return multimediaModalsList;
    }

}
