package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Servlet;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.xss.XSSAPI;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.PageManager;
import com.google.gson.Gson;
import com.pwc.madison.core.authorization.services.ContentAuthorizationService;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.models.User;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.RelatedContentUtils;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.util.MadisonUtil;

@Component(
    service = Servlet.class,
    property = {
            Constants.SERVICE_DESCRIPTION + "=This servlet is called for getting related links based on user access",
            "sling.servlet.methods=" + "POST", "sling.servlet.paths=" + "/bin/pwc-madison/validRelatedLinks",
            "sling.servlet.extensions=" + "json" })
public class RelatedLinksPermissionServlet extends SlingAllMethodsServlet {
    private static final long serialVersionUID = 1L;
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    @Reference(cardinality=ReferenceCardinality.OPTIONAL)
    private ContentAuthorizationService contentAuthorizationService;

    @Reference
    private UserRegRestService userRegRestService;

    @Reference
    private CryptoSupport cryptoSupport;

    @Reference
    private CountryTerritoryMapperService countryTerritoryMapperService;

    @Reference
    private UserPreferencesProviderService userPreferencesProviderService;
    
    @Reference
    private transient XSSAPI xssapi;

    User currentUser;
    
    private static final String PATH = "path";
    
    @Reference
    private SlingSettingsService slingSettingsService;

    private Set<String> noAccessPaths = Collections.emptySet();

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {
        RequestParameterMap requestParameterMap = request.getRequestParameterMap();
        String path = StringUtils.EMPTY;
        Gson gson = new Gson();
        currentUser = null;
        noAccessPaths = new HashSet<String>();
        if (requestParameterMap.containsKey(PATH) && isPublish()) {
            path = requestParameterMap.getValue(PATH).getString();
            LOG.debug("Paths to check {}", xssapi.encodeForHTML(path));
            String[] paths = StringUtils.split(path, ",");
            currentUser = UserInformationUtil.getUser(request, false, userRegRestService, cryptoSupport, response, true,
                    countryTerritoryMapperService, userPreferencesProviderService, false, false, xssapi);
            if (ArrayUtils.isNotEmpty(paths)) {
                checkAccess(request, paths);
            }
        }
        gson.toJson(noAccessPaths, response.getWriter());
    }

    private boolean isPublish() {
        String runMode = MadisonUtil.getCurrentRunmode(slingSettingsService);
        LOG.debug("runmode {}", xssapi.encodeForHTML(runMode));
        return StringUtils.equalsIgnoreCase(Externalizer.PUBLISH, runMode);
    }

    /**
     * 
     * @param request
     * @param paths
     */
    private void checkAccess(final SlingHttpServletRequest request, String[] paths) {
        ResourceResolver resourceResolver = request.getResourceResolver();
        for (String resourcePath : paths) {
            if (resourceResolver != null) {
                Resource resource = resourceResolver.resolve(resourcePath);
                if (resource != null && !StringUtils.equalsIgnoreCase(Resource.RESOURCE_TYPE_NON_EXISTING,
                        resource.getResourceType())) {
                    if (!RelatedContentUtils.isUserHasAccessToRelatedItem(contentAuthorizationService,
                            resourceResolver.adaptTo(PageManager.class), resource.getPath(), currentUser)) {
                        noAccessPaths.add(resourcePath);
                    }
                }
            }
        }
    }

}
