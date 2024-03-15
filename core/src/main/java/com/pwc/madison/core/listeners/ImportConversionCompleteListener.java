/*
 * Copyright 2015 Adobe Systems Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.pwc.madison.core.listeners;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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
import com.adobe.granite.asset.api.AssetManager;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.DamConstants;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.XMLFormatter;

/**
 * Event handler to post process dita topics from XHTML/DOCX conversion
 */
@Component(service = EventHandler.class,
           immediate = true,
           property = {
                   Constants.SERVICE_DESCRIPTION + "=PwC Viewpoint XHTML/DOCX conversion post processing event handler",
                   EventConstants.EVENT_TOPIC + "=com/adobe/fmdita/conversion/complete" })
public class ImportConversionCompleteListener implements EventHandler {

    private final String STATUS = "status";
    private final String SUCCESS = "Success";
    private final String FILE_PATH = "filePath";
    private final String OUTPUT_PATH = "outputPath";

    private final String NEW_LINE = "\\n";
    private final String TOPICS_NODE = "/topics";
    private final String BACKWARD_SLASH_PATTERN = "\\\\";
    private final String OBSOLETE_START_TAG = "<obsolete>";
    private final String OBSOLETE_END_TAG = "</obsolete>";
    private final String PWC_ID_PATTERN = "(pwc)(-)(topic)( )(id)(=).*?(\".*?\")";
    private final String OBSOLETE_START_TAG_PATTERN = "(<)(l)(i)(n)(e)(-)(t)(h)(r)(o)(u)(g)(h)(>)";
    private final String OBSOLETE_END_TAG_PATTERN = "(<)(/)(l)(i)(n)(e)(-)(t)(h)(r)(o)(u)(g)(h)(>)";

    private final String HTML_ZIP_EXTENSION = "zip";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ResourceResolverFactory resolverFactory;
    
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    @Override
    public void handleEvent(final Event event) {

        final String status = event.getProperty(STATUS).toString();
        final String outputPath = event.getProperty(OUTPUT_PATH).toString();
        final String filePath = event.getProperty(FILE_PATH).toString();

        if (SUCCESS.equals(status)) {
            final ResourceResolver resourceResolver = MadisonUtil
                    .getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());

            if (null == resourceResolver) {
                log.error("Unable to get fmdita service user resource resolver. Check the permissions");
            }

            final Resource inputResource = resourceResolver.getResource(filePath);
            final String extension = filePath.substring(filePath.lastIndexOf('.') + 1);

            try {
                if (HTML_ZIP_EXTENSION.equals(extension)) {
                    // Remove the source file (.zip) post conversion
                    resourceResolver.delete(inputResource);
                } else {
                    // Perform post processing of DITA directly to DAM.
                    writeToDam(outputPath, resourceResolver);
                    // Remove the source file (.docx) post conversion
                    resourceResolver.delete(inputResource);
                }

            } catch (final RepositoryException | IOException e) {
                log.error("Error during post processing of docx/xhtml conversion ", e);
            } finally {
                // close the service user resolver
                if (resourceResolver.isLive()) {
                    try {
                        resourceResolver.refresh();
                        resourceResolver.commit();
                        resourceResolver.close();
                    } catch (final PersistenceException e) {
                        log.error("Error while saving post processing changes for docx/xhtml conversion", e);
                    }
                }
            }
        }

    }

    /**
     * Pay-load is validated against folder type and process only the DITA files for formatting as per the PwC DTD.
     *
     * @param path
     * @param resolver
     * @throws PersistenceException
     */
    private void writeToDam(final String path, final ResourceResolver resolver)
            throws IOException, RepositoryException {

        final Session session = resolver.adaptTo(Session.class);
        final String newPath = path.replaceAll(BACKWARD_SLASH_PATTERN, DITAConstants.FORWARD_SLASH);
        // Remove the first '\' char - JCR API does not respect that
        final String correctedPath = newPath.replaceFirst(DITAConstants.FORWARD_SLASH, StringUtils.EMPTY);

        // Use JCR API to read property to check folder type.
        final Node root = session.getRootNode();
        final Node dataPathNode = root.getNode(correctedPath + TOPICS_NODE);

        final String property = dataPathNode.getProperty(JcrConstants.JCR_PRIMARYTYPE).getString();
        // only perform if the asset is folder.
        if (property.isEmpty() || !dataPathNode.hasNodes()) {
            return;
        }
        // Iterate each DITA file and do processing
        final NodeIterator linksIterator = dataPathNode.getNodes();
        if (null != linksIterator) {
            while (linksIterator.hasNext()) {
                processDitaFile(path, linksIterator.nextNode(), resolver);
            }
        }
    }

    /**
     * Perform post processing for each DITA file, ID formatting and pretty XML to make author more authorable and fix
     * the compiler issues.
     *
     * @param path
     * @param child
     * @param resolver
     * @throws RepositoryException
     * @throws IOException
     */
    private void processDitaFile(final String path, final Node child, final ResourceResolver resolver)
            throws RepositoryException, IOException {

        // Process only for DITA files.
        if (!child.getName().endsWith(DITAConstants.DITA_EXTENSION)) {
            return;
        } else if (child.getProperty(JcrConstants.JCR_PRIMARYTYPE).getString().equals(DamConstants.NT_DAM_ASSET)) {
            // Generate the unique ID as per PwC Topic template DTD.
            final String id = "pwc-topic id=\"pwc-topic.dita_" + child.getUUID() + "\"";

            // Use AssetManager to get the DITA file from the provided folder.
            final AssetManager assetMgr = resolver.adaptTo(AssetManager.class);

            final Asset dita = assetMgr.getAsset(child.getPath());
            final InputStream inputStream = dita.getRendition(DamConstants.ORIGINAL_FILE).getStream();
            final StringWriter writer = new StringWriter();
            IOUtils.copy(inputStream, writer, Charset.forName(MadisonConstants.UTF_8));

            final String theString = writer.toString();
            final String pwcDtd = theString.split(NEW_LINE)[0];
            final String pwcBody = theString.split(NEW_LINE)[1];
            final String replacedString = pwcBody.replaceAll(PWC_ID_PATTERN, id)
                    .replaceAll(OBSOLETE_START_TAG_PATTERN, OBSOLETE_START_TAG)
                    .replaceAll(OBSOLETE_END_TAG_PATTERN, OBSOLETE_END_TAG);
            // Format the XML so that author can see the errors at multiple lines.
            final XMLFormatter formatter = new XMLFormatter();
            String prettyXML = formatter.format(replacedString);
            prettyXML = prettyXML.substring(prettyXML.indexOf('\n') + 1);
            final String processedDita = pwcDtd + "\n" + prettyXML;

            // use ByteArrayInputStream to get the bytes of the String and convert them to InputStream.
            final InputStream assetStream = new ByteArrayInputStream(
                    processedDita.getBytes(Charset.forName(MadisonConstants.UTF_8)));
            // Set the modified stream back to DITA file.
            dita.setRendition(DamConstants.ORIGINAL_FILE, assetStream, dita.getValueMap());

            resolver.refresh();
            resolver.commit();
        }
    }
}
