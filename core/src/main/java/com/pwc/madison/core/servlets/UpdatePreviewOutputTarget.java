package com.pwc.madison.core.servlets;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Call Generate output ootb servlet for preview
 */
@Component(service = Servlet.class,
           property = { Constants.SERVICE_DESCRIPTION + "=Update preview output target path",
                   "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                   "sling.servlet.paths=" + "/bin/pwc-madison/update-preview-output-target" })
public class UpdatePreviewOutputTarget extends SlingSafeMethodsServlet {

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private XSSAPI xssAPI;

    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    private final String OUTPUT_PRESET_TARGET_PATH = "fmdita-targetPath";

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Override
    protected void doGet(@Nonnull final SlingHttpServletRequest request,
            @Nonnull final SlingHttpServletResponse response) throws ServletException, IOException {

        final String outputName = request.getParameter("outputName");
        final String source = request.getParameter("source");

        final ResourceResolver resourceResolver = MadisonUtil
                .getResourceResolver(resourceResolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());

        if (null == resourceResolver) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to get service resource resolver");
            return;
        }

        final Resource ditaMap = resourceResolver.getResource(source);

        if (null == ditaMap) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Source not present");
            return;
        }

        final Resource previewOutputPreset = ditaMap.getChild("jcr:content/metadata/namedoutputs/" + outputName);

        if (null == previewOutputPreset) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Preview output preset not present. Apply the preset");
            return;
        }

        final String territory = MadisonUtil.getTerritoryCodeForPath(source);
        final String language = MadisonUtil.getLanguageCodeForPath(source);

        if (null == language || null == territory) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Source not present under a territory");
            return;
        }

        final String previewTargetPath =
                MadisonConstants.PWC_MADISON_PREVIEW_BASEPATH + territory + "/" + language + "/" + "preview";

        final ModifiableValueMap presetProperties = previewOutputPreset.adaptTo(ModifiableValueMap.class);
        presetProperties.put(OUTPUT_PRESET_TARGET_PATH, previewTargetPath);

        try {
            if (resourceResolver.hasChanges()) {
                resourceResolver.commit();
                response.setStatus(HttpServletResponse.SC_OK);
            }
        } catch (final PersistenceException e) {
            LOG.error("Unable to update the output preset target path for {} ", xssAPI.encodeForHTML(source), e);
        } finally {
            if (resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }
    }
}
