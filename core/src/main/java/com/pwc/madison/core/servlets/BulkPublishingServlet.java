package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.message.BasicHeader;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.asset.api.Asset;
import com.day.cq.commons.Externalizer;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.thread.HttpGetThread;

/**
 * Servlet for triggering bulk publishing under given DAM folder with publishing point status as yes. curl -u
 * <user>:<password> <host>/bin/pwc-madison/bulk-publishing?dam-path=<valid-dam-path>\&output-path=<aemsite|previewsite>
 *
 * @author vijendrasetty
 *
 */
@Component(
    service = Servlet.class,
    property = { Constants.SERVICE_DESCRIPTION + "=Bulk Publishing Servlet",
            "sling.servlet.methods=" + HttpConstants.METHOD_GET,
            "sling.servlet.paths=" + "/bin/pwc-madison/bulk-publishing" },
    configurationPolicy = ConfigurationPolicy.REQUIRE)
public class BulkPublishingServlet extends SlingSafeMethodsServlet {

    private static final String AEMSITE = "aemsite";
    private static final int DEFAULT_TIMEOUT = 5000;
    private static final long serialVersionUID = 1L;
    private static String PUBLISH_LISTENER_PATH = "/bin/publishlistener";
    private static final String OUTPUT_PRESET_TARGET_PATH = "fmdita-targetPath";
    private static final String OUTPUT_PRESET_SITE_NAME = "fmdita-siteName";

    @Reference
    private Externalizer externalizer;

    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    transient ResourceResolverFactory resourceResolverFactory;

    private final Logger LOG = LoggerFactory.getLogger(BulkPublishingServlet.class);

    @Override
    protected void doGet(@Nonnull final SlingHttpServletRequest request,
            @Nonnull final SlingHttpServletResponse response) throws ServletException, IOException {
        final String damPath = request.getParameter("dam-path");
        final String outputType = request.getParameter("output-type");
        final PrintWriter writer = response.getWriter();
        LOG.info(" Fetching publishing point DITAMAP's from the following path ::  " + damPath);

        if (StringUtils.isBlank(damPath)) {
            writer.println(" DAM path not provided. Please check the request");
            writer.close();
            return;
        }
        if (StringUtils.isBlank(outputType) || !outputType.equals(AEMSITE) && !outputType.equals("previewsite")) {
            writer.println(" Output Type (possible values are 'aemsite' and 'previewsite') not provided. "
                    + "Please provide the valid output type.");
            writer.close();
            return;
        }

        final ResourceResolver resourceResolver = request.getResourceResolver();
        if (null == resourceResolver) {
            writer.println(" Unable to get fmdita-serviceuser resolver to process data. Please check the permissions");
            writer.close();
            return;
        }

        final Resource sourceResource = resourceResolver.getResource(damPath);

        if (null == sourceResource) {
            writer.println(" DAM path provided is invalid. Please check/validate and try again");
            writer.close();
            return;
        }
        final String resourceType = (String) sourceResource.getValueMap().getOrDefault(JcrConstants.JCR_PRIMARYTYPE,
                org.apache.commons.lang3.StringUtils.EMPTY);

        if (!resourceType.contains(MadisonConstants.STR_FOLDER)) {
            writer.println(" DAM path provided is not a folder. Please provide proper path");
            writer.close();
            return;
        }
        writer.println(" Fetching publishing point DITAMAP's from the following path ::  " + damPath);
        writer.flush();

        final List<String> publishingPoints = fetchPublishingPoints(damPath, resourceResolver, outputType);
        publishDitaMaps(writer, publishingPoints, resourceResolver, request, outputType);
        writer.close();

    }

    /**
     * Publish all the DITAMAP's from the provided publishing point.
     *
     * @param publishingPoints
     * @param resourceResolver
     * @param request
     */
    private void publishDitaMaps(final PrintWriter writer, final List<String> publishingPoints,
            final ResourceResolver resourceResolver, final SlingHttpServletRequest request, final String outputType) {

        if (null == publishingPoints || publishingPoints.size() < 1) {
            LOG.error(" No DITAMAP's found for bulk publish");
            writer.println(" No DITAMAP's found for bulk publishing");
            return;
        }
        writer.println(" Number of DITAMAP's found for bulk publishing -> " + publishingPoints.size());
        // Get URL, header before triggering GET request for DITA publishing.
        final List<Header> header = getHeader(request);
        final String publishingURL = getGenerateOutputAPIUrl(resourceResolver, outputType);
        final RequestConfig requestConfig = setConnectionData();

        if (StringUtils.isBlank(publishingURL)) {
            writer.println(" No DITAMAPS found for bulk publishing");
            return;
        }
        // Get each publishing point and publish.
        final Session session = resourceResolver.adaptTo(Session.class);
        // Set 2 thread for now, later to move it to configurable by OPS team.
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        for (final String ditaMap : publishingPoints) {

            final Runnable worker = new HttpGetThread(resourceResolver, publishingURL, ditaMap, requestConfig, header,
                    outputType, session.getUserID());
            executor.execute(worker);
        }
        writer.print("Waiting for the site generation to complete .");
        writer.flush();
        executor.shutdown();
        while (!executor.isTerminated()) {
            writer.print(".");
            writer.flush();
            try {
                TimeUnit.SECONDS.sleep(4);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
        writer.println("Finished executing all the threads");
        writer.close();
        LOG.info("Finished all threads");
    }

    /**
     * Get the authorization from the request and set to HTTP call.
     *
     * @param request
     * @return
     */
    private List<Header> getHeader(final SlingHttpServletRequest request) {
        final Header header = new BasicHeader(HttpHeaders.AUTHORIZATION, request.getHeader("Authorization"));
        return Arrays.asList(header);
    }

    /**
     * Fetches all the ditamaps having publishing points "yes".
     *
     * @param damPath
     * @return
     */
    private List<String> fetchPublishingPoints(final String damPath, final ResourceResolver resourceResolver,
            final String outputType) {

        final Resource ditamapRes = resourceResolver.getResource(damPath);
        if (null == ditamapRes) {
            return null;
        }
        final Map<String, Object> map = createDitaMapQuery(damPath);
        final Query query = queryBuilder.createQuery(PredicateGroup.create(map),
                resourceResolver.adaptTo(Session.class));
        final SearchResult ditaMapPaths = query.getResult();
        return populateDitamaps(ditaMapPaths, resourceResolver, outputType);
    }

    /**
     * @param ditaMapPaths
     * @param resourceResolver
     * @return
     */
    private List<String> populateDitamaps(final SearchResult ditaMapPaths, final ResourceResolver resourceResolver,
            final String outputType) {

        final List<String> ditaMaps = new ArrayList<>();
        if (null == ditaMapPaths) {
            return ditaMaps;
        }

        final Iterator<Resource> pathResources = ditaMapPaths.getResources();
        while (pathResources.hasNext()) {
            final Resource resource = pathResources.next();
            // Check for only ditamaps
            if (StringUtils.isNoneBlank(resource.getPath()) && resource.getPath().endsWith(DITAConstants.DITAMAP_EXT)
                    && updateDitaMapDestinationPath(resource, resourceResolver, outputType)) {
                ditaMaps.add(resource.getPath());
            }
        }
        return ditaMaps;
    }

    /**
     * @param ditaMap
     * @param resourceResolver
     * @return true if updated, false if not
     */
    private boolean updateDitaMapDestinationPath(final Resource ditaMap, final ResourceResolver resourceResolver,
            final String outputType) {

        // Output Preset for AEM Site
        final Resource aemSiteOutputPreset = ditaMap.getChild("jcr:content/metadata/namedoutputs/" + outputType);
        boolean updated = Boolean.FALSE;

        if (null == aemSiteOutputPreset) {
            return updated;
        }
        try {
            // Update the destination path to relative content DAM path and also the site name as the asset name.
            final Asset ditaAsset = ditaMap.adaptTo(Asset.class);
            final ModifiableValueMap presetProperties = aemSiteOutputPreset.adaptTo(ModifiableValueMap.class);
            final String destinationPath = getDestinationPathForDitamap(ditaMap, outputType);
            presetProperties.put(OUTPUT_PRESET_TARGET_PATH, destinationPath);
            presetProperties.put(OUTPUT_PRESET_SITE_NAME,
                    ditaAsset.getName().replaceFirst(DITAConstants.DITAMAP_EXT, StringUtils.EMPTY));
            if (outputType.equals(AEMSITE) && null != ditaAsset && null != resourceResolver) {
                updateWFPresetPath(ditaAsset, ditaMap, destinationPath, resourceResolver);
            }
            if (resourceResolver.hasChanges()) {
                resourceResolver.refresh();
                resourceResolver.commit();
            }
            updated = Boolean.TRUE;
        } catch (final PersistenceException e) {
            LOG.error("Unable to update the output preset target path for ditamap in bulk publishing {} ",
                    ditaMap.getPath());
            return false;
        }
        return updated;
    }

    /**
     * This Method is for setting the Workflow Preset Destination path
     *
     * @param ditaAsset
     *            ditaAsset
     * @param ditaMap
     *            ditaMap
     * @param destinationPath
     *            destinationPath
     * @param resourceResolver
     */
    private void updateWFPresetPath(final Asset ditaAsset, final Resource ditaMap, final String destinationPath,
            final ResourceResolver resourceResolver) {
        final Resource wfOutputPreset = ditaMap.getChild(DITAConstants.WORKFLOW_PRESETS_NODE);
        try {
            if (null != wfOutputPreset) {
                final ModifiableValueMap presetProperties = wfOutputPreset.adaptTo(ModifiableValueMap.class);
                if (null != presetProperties) {
                    presetProperties.put(OUTPUT_PRESET_TARGET_PATH, destinationPath);
                    presetProperties.put(OUTPUT_PRESET_SITE_NAME,
                            ditaAsset.getName().replaceFirst(DITAConstants.DITAMAP_EXT, StringUtils.EMPTY));
                }
                if (resourceResolver.hasChanges()) {
                    resourceResolver.refresh();
                    resourceResolver.commit();
                }
            }
        } catch (final PersistenceException e) {
            LOG.error("Unable to update the Workflow preset target path for ditamap in bulk publishing {} ",
                    ditaMap.getPath());
        }

    }

    private String getDestinationPathForDitamap(final Resource ditaMap, final String outputType) {
        // Strip ditamap and /dam/ from the source path ditamap.
        final String ditaMapPath = ditaMap.getPath();
        final int last = ditaMapPath.lastIndexOf("/");
        return outputType.equals(AEMSITE) ? ditaMapPath.substring(0, last).replaceFirst("/content/dam/", "/content/")
                : ditaMapPath.substring(0, last).replaceFirst("/content/dam/pwc-madison",
                        MadisonConstants.PWC_MADISON_PREVIEW_BASEPATH);
    }

    /**
     * Creates query to fetch DitaMap path from dam path based on publication point.
     *
     * @param damPath
     * @return
     */
    private Map<String, Object> createDitaMapQuery(final String damPath) {
        final Map<String, Object> predicateMap = new HashMap<>();
        predicateMap.put("path", damPath);
        predicateMap.put("type", "dam:Asset");
        predicateMap.put("property", "@jcr:content/" + "metadata/pwc:isPublishingPoint");
        predicateMap.put("property.value", "yes");
        predicateMap.put("p.limit", "-1");
        return predicateMap;
    }

    /**
     *
     * @param resourceResolver
     * @return
     */
    private String getGenerateOutputAPIUrl(final ResourceResolver resourceResolver, final String outputType) {
        final StringBuilder parameters = new StringBuilder();
        String getRequestApiEndPoint = StringUtils.EMPTY;
        if (externalizer != null) {
            parameters.append("http://localhost:4502");
            parameters.append(PUBLISH_LISTENER_PATH);
            parameters.append("?");
            parameters.append("operation=GENERATEOUTPUT");
            parameters.append("&");
            parameters.append("outputName=");
            parameters.append(outputType);
            parameters.append("&");
            parameters.append("source=");
            getRequestApiEndPoint = parameters.toString();
            LOG.info("Get request api {}", getRequestApiEndPoint);
        }
        return getRequestApiEndPoint;
    }

    /**
     *
     * @return
     */
    private static RequestConfig setConnectionData() {
        final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(DEFAULT_TIMEOUT)
                .setConnectionRequestTimeout(DEFAULT_TIMEOUT).setSocketTimeout(DEFAULT_TIMEOUT).build();
        return requestConfig;
    }
}
