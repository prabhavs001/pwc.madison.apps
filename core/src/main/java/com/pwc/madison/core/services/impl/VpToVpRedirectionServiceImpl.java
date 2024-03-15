package com.pwc.madison.core.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.google.gson.Gson;
import com.pwc.madison.core.beans.VpRedirectionModifyEventInfo;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonDomainsService;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.services.VpToVpRedirectionService;
import com.pwc.madison.core.services.impl.VpToVpRedirectionServiceImpl.VpToVpRedirectionConfiguration;
import com.pwc.madison.core.userreg.models.request.AddRedirectionsRequest;
import com.pwc.madison.core.userreg.models.request.ViewpointRedirection;
import com.pwc.madison.core.userreg.models.response.GetVpRedirectPathResponse;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.SecurityUtils;
import com.pwc.madison.core.util.BTree;
import com.pwc.madison.core.util.MadisonUtil;
import com.sun.jersey.api.client.ClientResponse;

@Component(
        service = VpToVpRedirectionService.class,
        property = { Constants.SERVICE_DESCRIPTION + "= VP To VP Redirection Service" })
@Designate(ocd = VpToVpRedirectionConfiguration.class)
public class VpToVpRedirectionServiceImpl implements VpToVpRedirectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VpToVpRedirectionServiceImpl.class);

    @Reference
    private UserRegRestService userregRestService;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    private MadisonDomainsService domainService;

    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    private boolean isEventCapturingEnabled;
    private boolean isVpToVpRedirectionEnabled;
    private String[] pathsDisabledForEventCapturing;
    private static final String INSERT_TO_REDIRECT = "INSERT_TO_REDIRECT";
    private String token;

    @Activate
    @Modified
    protected void activate(VpToVpRedirectionConfiguration vpRedirectionConfiguration) {
        isVpToVpRedirectionEnabled = vpRedirectionConfiguration.vp_redirection_enabled();
        isEventCapturingEnabled = vpRedirectionConfiguration.vp_redirection_event_capture_enabled();
        pathsDisabledForEventCapturing = vpRedirectionConfiguration.vp_redirection_paths_disabled();
        token = SecurityUtils.encrypt(INSERT_TO_REDIRECT, userregRestService.getfdKey());
        LOGGER.debug("VpToVpRedirectionServiceImpl Activate : Is Vp Redirection Enabled: {}",
                isVpToVpRedirectionEnabled);
        LOGGER.debug("VpToVpRedirectionServiceImpl Activate : Is Vp Redirection Event Capturing Enabled: {}",
                isEventCapturingEnabled);
        LOGGER.debug("VpToVpRedirectionServiceImpl Activate : Paths Disabled  EFor Event Capturing: {}",
                pathsDisabledForEventCapturing);
    }

    @ObjectClassDefinition(name = "PwC Viewpoint To Viewpoint Redirection Configuration")
    public @interface VpToVpRedirectionConfiguration {

        @AttributeDefinition(
                name = "Enable Viewpoint To Viewpoint Redirection",
                description = "Check if requested should be redirected to new path on 404 response of old page")
        boolean vp_redirection_enabled();

        @AttributeDefinition(
                name = "Enable Viewpoint To Viewpoint Redirection Event Capturing",
                description = "Check if event capturing should be enabled to capture redirection from old path to new path")
        boolean vp_redirection_event_capture_enabled();

        @AttributeDefinition(
                name = "Content Paths Disabled For Viewpoint To Viewpoint Redirection Event Capturing",
                description = "List of new content paths under which viewpoint redirection event capturing should be disabled",
                cardinality = Integer.MAX_VALUE)
        String[] vp_redirection_paths_disabled();

    }

    @Override
    public String getRedirectPath(String oldPath) {
        String redirectPath = null;
        if (isVpToVpRedirectionEnabled) {
            LOGGER.debug("VpToVpRedirectionServiceImpl getRedirectPath : called for path {}", oldPath);
            if (StringUtils.isNoneEmpty(oldPath)) {
                ClientResponse clientResponse = userregRestService.getRedirectPath(oldPath);
                LOGGER.debug("VpToVpRedirectionServiceImpl getRedirectPath : Redirect path Status for path {} : {}",
                        oldPath, clientResponse.getStatus());
                if (clientResponse.getStatus() == SlingHttpServletResponse.SC_OK) {
                    final String responseString = clientResponse.getEntity(String.class);
                    final GetVpRedirectPathResponse getVpRedirectPathResponse = new Gson().fromJson(responseString,
                            GetVpRedirectPathResponse.class);
                    redirectPath = domainService.getPublishedPageUrl(getVpRedirectPathResponse.getRedirectionPath(),
                            false);
                }
            }
        }
        return redirectPath;
    }

    @Override
    public void captureModifiedUrls(final List<VpRedirectionModifyEventInfo> modifiedUrlsInfoList) {
        if (isEventCapturingEnabled) {
            LOGGER.info("VpToVpRedirectionServiceImpl captureModifiedPaths : called for Modified urls {}",
                    modifiedUrlsInfoList);
            if (null != modifiedUrlsInfoList) {
                ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resolverFactory,
                        madisonSystemUserNameProviderService.getFmditaServiceUsername());
                if (resourceResolver != null) {
                    captureModifiedUrls(resourceResolver, modifiedUrlsInfoList);
                } else {
                    LOGGER.warn(
                            "VpToVpRedirectionServiceImpl captureModifiedUrls() : Getting Resource Resolver {} as null",
                            madisonSystemUserNameProviderService.getFmditaServiceUsername());
                }
            }
        }
    }

    /**
     * It adds valid {@link VpRedirectionModifyEventInfo} in {@link List} for VP redirection.
     * 
     * @param resourceResolver
     *            {@link ResourceResolver}
     * @param modifiedUrlsInfoList
     *            {@link List} of {@link VpRedirectionModifyEventInfo}
     */
    private void captureModifiedUrls(final ResourceResolver resourceResolver,
            final List<VpRedirectionModifyEventInfo> modifiedUrlsInfoList) {
        try {
            final Session session = resourceResolver.adaptTo(Session.class);
            final AddRedirectionsRequest addRedirectionsRequest = new AddRedirectionsRequest(
                    new ArrayList<ViewpointRedirection>());
            final List<ViewpointRedirection> viewpointRedirections = addRedirectionsRequest.getRedirects();
            BTree guidbtree = new BTree(session.getNode(DITAConstants.GUID_TO_PATH));
            modifiedUrlsInfoList.forEach((vpRedirectionModifyEventInfo) -> {
                if (checkRedirectionValidityForModifiedUrl(resourceResolver, vpRedirectionModifyEventInfo, guidbtree,
                        session)) {
                    LOGGER.debug(
                            "VpToVpRedirectionServiceImpl captureModifiedUrls() : Adding Event to VP redirection {}",
                            vpRedirectionModifyEventInfo);
                    viewpointRedirections.add(new ViewpointRedirection(vpRedirectionModifyEventInfo.getOldUrl(),
                            vpRedirectionModifyEventInfo.getNewUrl(), false, true));
                } else {
                    LOGGER.debug(
                            "VpToVpRedirectionServiceImpl captureModifiedUrls() : Not Adding Event to VP redirection {}",
                            vpRedirectionModifyEventInfo);
                }
            });
            LOGGER.info("VpToVpRedirectionServiceImpl captureModifiedUrls() : Adding Final events to VP redirection {}",
                    viewpointRedirections);
            if (viewpointRedirections.size() > 0) {
                ClientResponse clientResponse = userregRestService.addRedirects(addRedirectionsRequest, token);
                LOGGER.info(
                        "VpToVpRedirectionServiceImpl captureModifiedUrls() : Response from API for VP redirections {} : {}",
                        viewpointRedirections, clientResponse.getStatus());
            }
        } catch (RepositoryException repositoryException) {
            LOGGER.error(
                    "VpToVpRedirectionServiceImpl captureModifiedUrls() : Exception occured while get node of path {} : {}",
                    DITAConstants.GUID_TO_PATH, repositoryException);
        } finally {
            if (null != resourceResolver && resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }
    }

    /**
     * Returns true if redirect in {@link VpRedirectionModifyEventInfo} is valid to be added for VP redirection.
     * 
     * @param resourceResolver
     *            {@link ResourceResolver}
     * @param vpRedirectionModifyEventInfo
     *            {@link VpRedirectionModifyEventInfo} contains modified URL information
     * @param guidbtree
     *            {@link Btree}
     * @param session
     *            {@link Session}
     * @return {@link Boolean}
     */
    private boolean checkRedirectionValidityForModifiedUrl(final ResourceResolver resourceResolver,
            final VpRedirectionModifyEventInfo vpRedirectionModifyEventInfo, final BTree guidbtree,
            final Session session) {
        if (!vpRedirectionModifyEventInfo.getOldUrl().contains(MadisonConstants.HASH)
                && isNewPathEnabledForVpRedirection(getPathFromPageUrl(vpRedirectionModifyEventInfo.getNewUrl()))) {
            return checkPageTopicFmguidSingleUsability(vpRedirectionModifyEventInfo, guidbtree, session,
                    resourceResolver);
        } else {
            LOGGER.debug(
                    "VpToVpRedirectionServiceImpl checkRedirectionValidityForModifiedUrl : Either old URL contains Hash '#' or new URL is disabled for redirection capturing, Not adding redirection to new URL {}",
                    vpRedirectionModifyEventInfo);
        }
        return false;
    }

    /**
     * It returns true if given fmguid's topic is used in single ditamap otherwise false. Topic path is extracted from
     * given fmguid by searching in guidbtree {@link BTree}.
     * 
     * @param vpRedirectionModifyEventInfo
     *            {@link VpRedirectionModifyEventInfo} contains modified URL information
     * @param guidbtree
     *            {@link BTree}
     * @param session
     *            {@link Session}
     * @param resourceResolver
     *            {@link ResourceResolver}
     * @return {@link Boolean}
     */
    private boolean checkPageTopicFmguidSingleUsability(final VpRedirectionModifyEventInfo vpRedirectionModifyEventInfo,
            final BTree guidbtree, final Session session, final ResourceResolver resourceResolver) {
        try {
            Node node = guidbtree.getBTreeNodeForKey(vpRedirectionModifyEventInfo.getFmguid());
            if (null != node) {
                Property property = node.getProperty(vpRedirectionModifyEventInfo.getFmguid());
                String topic = property.getString();
                boolean isTopicSingleReferenced = checkSingleTopicReference(topic, session);
                if (!isTopicSingleReferenced) {
                    LOGGER.info(
                            "VpToVpRedirectionServiceImpl checkPageTopicFmguidSingleUsability() : There are multiple references for topic {}",
                            topic);
                }
                return isTopicSingleReferenced;
            }
        } catch (RepositoryException repositoryException) {
            LOGGER.error(
                    "VpToVpRedirectionServiceImpl checkPageTopicSingleUsability() : Exception occured while finding topic of fmguid {} : {}",
                    vpRedirectionModifyEventInfo.getFmguid(), repositoryException);
            return checkPageTopicFmguidSingleUsabilityUsingSourcePath(vpRedirectionModifyEventInfo, resourceResolver,
                    session);
        }
        return false;
    }

    /**
     * It returns true if given fmguid's topic is used in single ditamap otherwise false. Topic path is extracted from
     * {@value DITAConstants#SOURCE_PATH} property on new URL page's content node.
     * 
     * @param vpRedirectionModifyEventInfo
     *            {@link VpRedirectionModifyEventInfo} contains modified URL information
     * @param resourceResolver
     *            {@link ResourceResolver}
     * @param session
     *            {@link Session}
     * @return {@link Boolean}
     */
    private boolean checkPageTopicFmguidSingleUsabilityUsingSourcePath(
            final VpRedirectionModifyEventInfo vpRedirectionModifyEventInfo, final ResourceResolver resourceResolver,
            final Session session) {
        LOGGER.info(
                "VpToVpRedirectionServiceImpl checkPageTopicFmguidSingleUsabilityUsingSourcePath() : Using Source Path property to get Topic of new URL {}",
                vpRedirectionModifyEventInfo.getNewUrl());
        if (!vpRedirectionModifyEventInfo.getNewUrl().contains(MadisonConstants.HASH)) {
            final String newPath = getPathFromPageUrl(vpRedirectionModifyEventInfo.getNewUrl());
            final Resource resource = resourceResolver
                    .getResource(newPath + MadisonConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
            final String topic = resource.getValueMap().get(DITAConstants.SOURCE_PATH, String.class);
            return checkSingleTopicReference(topic, session);
        } else {
            LOGGER.info(
                    "VpToVpRedirectionServiceImpl checkPageTopicFmguidSingleUsabilityUsingSourcePath() : New URL contains Hash hence we can not find source path {}",
                    vpRedirectionModifyEventInfo.getNewUrl());
            return checkPageTopicFmguidSingleUsabilityUsingAnchorId(vpRedirectionModifyEventInfo, session);
        }
    }

    /**
     * It returns true if given fmguid's topic is used in single ditamap otherwise false. Topic path is extracted from
     * Anchor ID of new URL.
     * 
     * @param vpRedirectionModifyEventInfo
     *            {@link VpRedirectionModifyEventInfo} contains modified URL information
     * @param session
     *            {@link Session}
     * @return {@link Boolean}
     */
    private boolean checkPageTopicFmguidSingleUsabilityUsingAnchorId(
            final VpRedirectionModifyEventInfo vpRedirectionModifyEventInfo, final Session session) {
        LOGGER.info(
                "VpToVpRedirectionServiceImpl checkPageTopicFmguidSingleUsabilityUsingAnchorId() : Using Anchor ID to get Topic of new URL {}",
                vpRedirectionModifyEventInfo.getNewUrl());
        final int hashIndex = vpRedirectionModifyEventInfo.getNewUrl().indexOf(MadisonConstants.HASH);
        final String anchor = vpRedirectionModifyEventInfo.getNewUrl().substring(hashIndex + 1,
                vpRedirectionModifyEventInfo.getNewUrl().length());
        String topicPath = null;
        try {
            final Map<String, String> map = new HashMap<String, String>();
            map.put("path", MadisonConstants.PWC_MADISON_DITAROOT_DAM_PATH);
            map.put("type", DamConstants.NT_DAM_ASSET);
            map.put("fulltext.relPath",
                    JcrConstants.JCR_CONTENT + MadisonConstants.FORWARD_SLASH + "@" + DITAConstants.FMDITA_IDS);
            map.put("fulltext", anchor);
            map.put("p.limit", MadisonConstants.P_LIMIT);
            Query query = queryBuilder.createQuery(PredicateGroup.create(map), session);
            SearchResult result = query.getResult();
            List<Hit> hits = result.getHits();
            if (null != hits) {
                for (Hit hit : hits) {
                    final ValueMap valueMap = hit.getProperties();
                    final String fmditaIds = valueMap.get(DITAConstants.FMDITA_IDS, StringUtils.EMPTY);
                    if (fmditaIds.startsWith(anchor)) {
                        topicPath = hit.getPath();
                    }
                }
            } else {
                LOGGER.info(
                        "VpToVpRedirectionServiceImpl checkPageTopicFmguidSingleUsabilityUsingAnchorId() : Got no topics for Anchor ID {}",
                        anchor);
                return false;
            }
        } catch (Exception exception) {
            LOGGER.error(
                    "VpToVpRedirectionServiceImpl checkSingleTopicReference() : Exception occured while finding topic with anhor ID {} : {}",
                    anchor, exception);
        }
        if (null != topicPath) {
            return checkSingleTopicReference(topicPath, session);
        } else {
            LOGGER.info(
                    "VpToVpRedirectionServiceImpl checkPageTopicFmguidSingleUsabilityUsingAnchorId() : Got no topic for anchor ID from property fmditaIds {}",
                    anchor);
            return false;
        }
    }

    /**
     * Returns true if given path is not disabled for vp redirection event capturing otherwise false.
     * 
     * @param newPath
     *            {@link String} new path that is checked
     * @return {@link Boolean}
     */
    private boolean isNewPathEnabledForVpRedirection(final String newPath) {
        for (String pathDisabledForEventCapturing : pathsDisabledForEventCapturing) {
            if (newPath.startsWith(pathDisabledForEventCapturing)) {
                return false;
            }
        }
        return true;
    }

    /**
     * It returns true if topic is used in single ditamap otherwise false. It uses
     * {@value DITAConstants#PN_FMDITATOPICREFS} property on ditamaps to check the number of references using query.
     * 
     * @param topic
     *            {@link String} dita topic
     * @param session
     *            {@link Session} session
     * @return {@link Boolean}
     */
    private boolean checkSingleTopicReference(final String topic, final Session session) {
        try {
            final Map<String, String> map = new HashMap<String, String>();
            map.put("path", MadisonConstants.PWC_MADISON_DITAROOT_DAM_PATH);
            map.put("type", DamConstants.NT_DAM_ASSETCONTENT);
            map.put("property", DITAConstants.PN_FMDITATOPICREFS);
            map.put("property.value", MadisonConstants.COMMA_SEPARATOR + topic);
            map.put("p.limit", MadisonConstants.P_LIMIT);
            Query query = queryBuilder.createQuery(PredicateGroup.create(map), session);
            SearchResult result = query.getResult();
            List<Hit> hits = result.getHits();
            if (hits != null && hits.size() == 1) {
                return true;
            } else {
                LOGGER.info(
                        "VpToVpRedirectionServiceImpl checkSingleTopicReference() : There are multiple references for topic {}",
                        topic);
            }
        } catch (Exception exception) {
            LOGGER.error(
                    "VpToVpRedirectionServiceImpl checkSingleTopicReference() : Exception occured while finding references of topic {} : {}",
                    topic, exception);
        }
        return false;

    }

    /**
     * It returns page path from given page URL.
     * 
     * @param path
     *            {@link String}
     * @return {@link String}
     */
    private String getPathFromPageUrl(final String path) {
        int dotIndex = path.indexOf(".");
        if (dotIndex > -1) {
            return path.substring(0, dotIndex);
        }
        return path;
    }

}
