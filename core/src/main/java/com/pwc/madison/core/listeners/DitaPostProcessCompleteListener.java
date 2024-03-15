package com.pwc.madison.core.listeners;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.asset.api.Rendition;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.DamConstants;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.XMLFormatter;

/**
 * DITA post process completion event handler - to update bodydiv id for pwc templates
 */
@Component(
    service = EventHandler.class,
    immediate = true,
    property = { Constants.SERVICE_DESCRIPTION + "=PwC Viewpoint dita post process completion event handler",
            EventConstants.EVENT_TOPIC + "=" + DitaPostProcessCompleteListener.EVENT_TOPIC })
public class DitaPostProcessCompleteListener implements EventHandler {

    private static final String PN_DC_FORMAT = "dc:format";
    private static final String PN_FMDITA_TARGET_PATH = "fmdita-targetPath";
    private static final String JCR_CONTENT_METADATA = "/jcr:content/metadata";
    private static final String IS_PROCESSED = "isProcessed";
    private static final String DAM_BASE = "/dam/";
    private static final String PN_OUTPUT_PRESET_SITE_NAME = "fmdita-siteName";
    public static final String EVENT_TOPIC = "com/adobe/fmdita/postprocess/complete";

    private final String PAYLOAD_PATH = "path";
    private final String ORIGINAL_CONTENT_PATH = "/jcr:content/renditions/original/jcr:content";

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    @Override
    public void handleEvent(final Event event) {

        final String payloadPath = event.getProperty(PAYLOAD_PATH).toString();
        final String ditaPath = payloadPath.replace(ORIGINAL_CONTENT_PATH, "");

        final ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resolverFactory,
                madisonSystemUserNameProviderService.getFmditaServiceUsername());

        if (null == resourceResolver) {
            return;
        }

        if (!ditaPath.endsWith(".dita")) {
            if (StringUtils.isNotBlank(ditaPath) && ditaPath.endsWith(DITAConstants.DITAMAP_EXT)) {
                updateOutputDestinationPath(ditaPath, resourceResolver);
            }
            return;
        }

        final Resource ditaResource = resourceResolver.getResource(ditaPath);

        if (null == ditaResource) {
            if (resourceResolver.isLive()) {
                resourceResolver.close();
            }
            return;
        }

        final Asset ditaAsset = ditaResource.adaptTo(Asset.class);

        if (null == ditaAsset) {
            if (resourceResolver.isLive()) {
                resourceResolver.close();
            }
            return;
        }

        final Rendition original = ditaAsset.getRendition(DamConstants.ORIGINAL_FILE);

        final InputStream inputStream = original.getStream();
        final StringWriter writer = new StringWriter();

        try {
            IOUtils.copy(inputStream, writer, Charset.forName(MadisonConstants.UTF_8));
        } catch (final IOException e) {
            LOG.error("Error while modifying the dita to pre include body div ", e);
            if (resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }

        String ditaContent = writer.toString();

        if (!(ditaContent.contains("<bodydiv id=\"body_div\">") || ditaContent.contains("<faq-bodydiv id=\"body_div\">")
                || ditaContent.contains("<example-bodydiv id=\"body_div\">"))) {
            // bodydiv or faq-bodydiv of example-bodydiv not present or updated. don't do anything

            try {
                final String jcrContentNodePath = original.getPath() + MadisonConstants.FORWARD_SLASH
                        + JcrConstants.JCR_CONTENT;
                LOG.debug("trying to Updating MIMETYPE for asset rendition{}", jcrContentNodePath);
                final Resource jcrContentResource = resourceResolver.getResource(jcrContentNodePath);
                if (null != jcrContentResource) {
                    final ModifiableValueMap modifiableValueMap = jcrContentResource.adaptTo(ModifiableValueMap.class);
                    if (null != modifiableValueMap && !modifiableValueMap.containsKey(JcrConstants.JCR_MIMETYPE)) {
                        modifiableValueMap.put(JcrConstants.JCR_MIMETYPE, DITAConstants.APPLICATION_XML);
                        LOG.debug("Updating MIMETYPE for asset rendition{}", jcrContentNodePath);

                        resourceResolver.commit();
                    }
                }
            } catch (final PersistenceException e) {
                LOG.error("Error while Updating MIMETYPE {}", e);
            } finally {
                if (resourceResolver.isLive()) {
                    resourceResolver.close();
                }
            }

            return;
        }

        // construct a unique id using asset uuid
        final String ditaIdentifier = ditaAsset.getIdentifier();
        final String bodydivId = "bodydiv_" + ditaIdentifier;

        final String oldId = "id=\"body_div\"";
        final String newId = "id=\"" + bodydivId + "\"";
        ditaContent = ditaContent.replace(oldId, newId);

        final XMLFormatter formatter = new XMLFormatter();

        ditaContent = formatter.format(ditaContent);

        final Rendition modifiedRendition = ditaAsset.setRendition(DamConstants.ORIGINAL_FILE,
                IOUtils.toInputStream(ditaContent, StandardCharsets.UTF_8), original.getValueMap());

        addMimeTypeProperty(resourceResolver, modifiedRendition);

        try {
            if (resourceResolver.hasChanges()) {
                resourceResolver.refresh();
                resourceResolver.commit();
            }
        } catch (final PersistenceException e) {
            LOG.error("Unable to commit pre including body div changes ", e);
        } finally {
            resourceResolver.close();
        }
    }

    private void updateOutputDestinationPath(final String ditamapPath, final ResourceResolver resourceResolver) {
        LOG.debug("Ditamap Path {}", ditamapPath);
        final Resource assetRes = resourceResolver.getResource(ditamapPath + JCR_CONTENT_METADATA);
        final Resource ditaMapRes = resourceResolver.getResource(ditamapPath);

        if (null == ditaMapRes || null == assetRes) {
            return;
        }
        final String ditamapName = ditaMapRes.getName().replaceFirst(DITAConstants.DITAMAP_EXT, StringUtils.EMPTY);
        final Node assetMetaNode = assetRes.adaptTo(Node.class);
        try {
            if (null != assetMetaNode && !assetMetaNode.hasProperty(IS_PROCESSED)) {
                final String outputPath = StringUtils.substringBeforeLast(ditamapPath, DITAConstants.FORWARD_SLASH)
                        .replace(DAM_BASE, DITAConstants.FORWARD_SLASH);

                final Resource aemSiteRes = resourceResolver
                        .getResource(ditamapPath + DITAConstants.AEMSITE_PRESETS_NODE);
                final Resource workFlowRes = resourceResolver
                        .getResource(ditamapPath + DITAConstants.FORWARD_SLASH + DITAConstants.WORKFLOW_PRESETS_NODE);
                if (null == aemSiteRes || null == workFlowRes) {
                    return;
                }

                final Node aemSiteNode = aemSiteRes.adaptTo(Node.class);
                final Node workFlowNode = workFlowRes.adaptTo(Node.class);

                aemSiteNode.setProperty(PN_FMDITA_TARGET_PATH, outputPath);
                aemSiteNode.setProperty(PN_OUTPUT_PRESET_SITE_NAME, ditamapName);
                workFlowNode.setProperty(PN_FMDITA_TARGET_PATH, outputPath);
                workFlowNode.setProperty(PN_OUTPUT_PRESET_SITE_NAME, ditamapName);

                assetMetaNode.setProperty(IS_PROCESSED, true);
                if (resourceResolver.hasChanges()) {
                    resourceResolver.refresh();
                    resourceResolver.commit();
                }
            }

        } catch (final RepositoryException | PersistenceException e) {
            LOG.error("updateOutputDestinationPath {}", e);
        } finally {
            if (resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }

    }

    /**
     * Method to add jcr:mimeType property to jcr:content Node
     *
     * @param resourceResolver
     *            Resource Resolver reference
     * @param modifiedRendition
     *            Modified Rendition
     */
    private void addMimeTypeProperty(final ResourceResolver resourceResolver, final Rendition modifiedRendition) {
        final String jcrContentNodePath = modifiedRendition.getPath() + MadisonConstants.FORWARD_SLASH
                + JcrConstants.JCR_CONTENT;
        final Resource jcrContentResource = resourceResolver.getResource(jcrContentNodePath);
        if (null != jcrContentResource) {
            final ModifiableValueMap modifiableValueMap = jcrContentResource.adaptTo(ModifiableValueMap.class);
            if (null != modifiableValueMap) {
                modifiableValueMap.put(JcrConstants.JCR_MIMETYPE, DITAConstants.APPLICATION_XML);
            }
        }
    }
}
