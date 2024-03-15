/*
 * Model class for populating the authorable global navigation component fields.
 */
package com.pwc.madison.core.models.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.UriBuilder;

import com.pwc.madison.core.services.MadisonDomainsService;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.GlobalNavigation;
import com.pwc.madison.core.models.GlobalNavigationGroup;
import com.pwc.madison.core.models.GlobalNavigationLink;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.models.User;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegPagesPathProvidesService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.util.MadisonUtil;

@Model(
        adaptables = { SlingHttpServletRequest.class },
        adapters = { GlobalNavigation.class },
        resourceType = { GlobalNavigationImpl.RESOURCE_TYPE },
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class GlobalNavigationImpl implements GlobalNavigation {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalNavigation.class);

    protected static final String RESOURCE_TYPE = "pwc-madison/components/structure/global-navigation";

    private static final String LINKS_NODE = "links";

    private static final String GROUPS_NODE = "groups";

    private static final String EXTERNAL_SELECTOR = "external";

    private static final String INTERNAL_SELECTOR = "internal";

    private static final String SEARCH_TYPE_QUERY_PARAM = "s";

    private static final String SEARCH_TYPE_QUERY_PARAM_VALUE = "c";
    
    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private ResourceResolver resolver;

    @Self
    private SlingHttpServletRequest slingRequest;

    @OSGiService
    private UserRegRestService userRegRestService;

    @SlingObject
    private SlingHttpServletResponse slingResponse;

    @OSGiService
    private CryptoSupport cryptoSupport;

    @OSGiService
    private CountryTerritoryMapperService countryTerritoryMapperService;

    @OSGiService
    private MadisonDomainsService madisonDomainsService;

    @OSGiService
    private UserPreferencesProviderService userPreferencesProviderService;

    @OSGiService
    private UserRegPagesPathProvidesService userRegPagesPathProvidesService;

    @OSGiService
    private XSSAPI xssapi;

    private List<GlobalNavigationGroup> globalNavigationItems = new ArrayList<>();

    private boolean isAuthorMode;

    private Resource groups;

    private Resource hamburgerResource;

    private boolean isInternalUser;

    private String madisonDefaultDomain;

    private String madisonPublishDomain;

    @PostConstruct
    protected void init() {
        final List<String> selectors = Arrays.asList(slingRequest.getRequestPathInfo().getSelectors());
        if (selectors.contains(EXTERNAL_SELECTOR)) {
            isInternalUser = false;
        } else if (selectors.contains(INTERNAL_SELECTOR)) {
            isInternalUser = true;
        } else {
            final User user = UserInformationUtil.getUser(slingRequest, false, userRegRestService, cryptoSupport,
                    slingResponse, true, countryTerritoryMapperService, userPreferencesProviderService, false, false, xssapi);
            isInternalUser = user.isUserLoggedIn() ? user.getUserProfile().getIsInternalUser() : Boolean.FALSE;
        }
        LOGGER.debug("GlobalNavigationImpl : init() : Selectors are {} and Is Internal user : {}", selectors,
                isInternalUser);
        setHamburgerResource();
        if (Objects.nonNull(hamburgerResource)) {
            groups = hamburgerResource.getChild(GROUPS_NODE);
        }
        madisonDefaultDomain = MadisonConstants.ALL_SELECTOR_REGEX.concat(madisonDomainsService.getDefaultDomain().split(MadisonConstants.DOUBLE_FORWARD_SLASH)[1]);
        madisonPublishDomain = MadisonConstants.ALL_SELECTOR_REGEX.concat(madisonDomainsService.getPublishDomain().split(MadisonConstants.DOUBLE_FORWARD_SLASH)[1]);

        generateGlobalNavigation();
    }

    private void generateGlobalNavigation() {
        if (groups != null) {
            for (Resource group : groups.getChildren()) {
                generateGlobalNavigationGroups(group);
            }
        }
    }

    /**
     * @param value generates group for global navigation.
     */
    private void generateGlobalNavigationGroups(Resource value) {
        GlobalNavigationGroup group = Objects.requireNonNull(value.adaptTo(GlobalNavigationGroup.class));
        String groupPath = group.getGroupPath();
        isAuthorMode = !MadisonUtil.isPublishMode(slingRequest);

        if (isAuthorMode || MadisonUtil.isGroupsOrGroupLinksVisible(group.getUserType(), isInternalUser)) {
            if (StringUtils.isNotBlank(groupPath)) {
                String processedlinkPath = StringUtils.EMPTY;
                if(groupPath.startsWith(MadisonConstants.SLASH_CONTENT)) {
                     processedlinkPath = MadisonUtil.getUrlForPageResource(groupPath);
                     group.setGroupPath(processedlinkPath);
                }
                 else if(groupPath.matches(madisonDefaultDomain.concat(MadisonConstants.PWC_MADISON_CONTENT_SEARCH_BASEPATH_REGEX))
                    || groupPath.matches(madisonPublishDomain.concat(MadisonConstants.PWC_MADISON_SEARCH_PATH_REGEX))){
                     processedlinkPath = appendQueryParamInSearchPageUrl(groupPath);
                     group.setGroupPath(processedlinkPath);

                }

            }

            Resource links = value.getChild(LINKS_NODE);
            group.setMultiFieldGlobalNavigationLinksBeanList(generateGlobalNavigationLinks(links));
            globalNavigationItems.add(group);
        }
    }


    private List<GlobalNavigationLink> generateGlobalNavigationLinks(Resource links) {
        List<GlobalNavigationLink> globalNavigationLink = new ArrayList<>();
        if (links != null) {
            for (Resource link : links.getChildren()) {
                GlobalNavigationLink groupLinksItem = Objects.requireNonNull(link.adaptTo(GlobalNavigationLink.class));
                String navigationURL = groupLinksItem.getNavigationURL();
                if (isAuthorMode || MadisonUtil.isGroupsOrGroupLinksVisible(groupLinksItem.getUserType(), isInternalUser)) {
                    if (StringUtils.isNotBlank(navigationURL)){
                        String processedlinkPath = StringUtils.EMPTY;
                        if(navigationURL.startsWith(MadisonConstants.SLASH_CONTENT)) {
                            processedlinkPath = MadisonUtil.getUrlForPageResource(navigationURL);
                            groupLinksItem.setNavigationURL(processedlinkPath);

                        }
                        else if(navigationURL.matches(madisonDefaultDomain.concat(MadisonConstants.PWC_MADISON_CONTENT_SEARCH_BASEPATH_REGEX))
                                || navigationURL.matches(madisonPublishDomain.concat(MadisonConstants.PWC_MADISON_SEARCH_PATH_REGEX))){
                            processedlinkPath = appendQueryParamInSearchPageUrl(navigationURL);
                            groupLinksItem.setNavigationURL(processedlinkPath);
                        }
                    }

                    Resource subLinks = link.getChild(LINKS_NODE);
                    groupLinksItem.setMultiFieldGlobalNavigationLinksBeanList(generateGlobalNavigationLinks(subLinks));
                    if(groupLinksItem.getMultiFieldGlobalNavigationLinksBeanList().size()>0) {
                        groupLinksItem.setOpenLinkNewWindow("");
                    }
                    globalNavigationLink.add(groupLinksItem);
                }
            }
        }
        return globalNavigationLink;
    }

    /**
     * method to append query parameter in Search Url
     */

    private String appendQueryParamInSearchPageUrl(String searchPath){
       return UriBuilder.fromUri(searchPath).queryParam(SEARCH_TYPE_QUERY_PARAM,SEARCH_TYPE_QUERY_PARAM_VALUE).build().toString();
    }

    /**
     * method to implement inheritance for global navigation
     */

    private void setHamburgerResource() {
        String pageLocale = MadisonUtil.getLocaleOfCurrentPage(userRegPagesPathProvidesService, countryTerritoryMapperService, slingRequest);
        hamburgerResource = MadisonUtil.getConfiguredResource(currentPage, MadisonConstants.GLOBAL_NAVIGATION_RELATIVE_PATH_FROM_PAGE, pageLocale, resolver, xssapi);
    }

    @Override
    public List<GlobalNavigationGroup> getGlobalNavigationItems() {
        return globalNavigationItems;
    }

    @Override
    public String getComponentName() {
        return MadisonConstants.ANALYTICS_HEADER_COMPONENT_NAME;
    }
}
