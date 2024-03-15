package com.pwc.madison.core.workflows;

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
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.asset.api.AssetManager;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.DamConstants;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.XMLFormatter;

/**
 * Workflow to post process the DITA (converted from WORD).
 * 
 * @author vhs
 *
 */
@Component(service = WorkflowProcess.class, property = { "process.label= Madision - Word 2 Dita post processor" })
public class Word2DitaPostProcessor implements WorkflowProcess {

    private String NEW_LINE = "\\n";
    private String OBSOLETE_START_TAG = "<obsolete>";
    private String OBSOLETE_END_TAG = "</obsolete>";
    private String PWC_ID_PATTERN = "(pwc)(-)(topic)( )(id)(=).*?(\".*?\")";
    private String OBSOLETE_START_TAG_PATTERN = "(<)(l)(i)(n)(e)(-)(t)(h)(r)(o)(u)(g)(h)(>)";
    private String OBSOLETE_END_TAG_PATTERN = "(<)(/)(l)(i)(n)(e)(-)(t)(h)(r)(o)(u)(g)(h)(>)";

    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    @Override
    public void execute(WorkItem item, WorkflowSession wfsession, MetaDataMap args) throws WorkflowException {

        log.debug(":::Here in execute method fo Word2DitaPostProcessor:::");

        WorkflowData workflowData = item.getWorkflowData();
        String path = workflowData.getPayload().toString();

        // Perform post processing of DITA directly to DAM.
        try {
            writeToDam(path, wfsession);
        } catch (PersistenceException e) {
            log.error("Error saving the changes to the DITA " + e.getMessage());
        }

    }

    /**
     * Payload is validated against folder type and process only the DITA files for formatting as per the PwC DTD.
     * 
     * @param path
     * @param wfsession
     * @throws PersistenceException
     */
    private void writeToDam(String path, WorkflowSession wfsession) throws PersistenceException {

        ResourceResolver resolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
        try {

            Session session = resolver.adaptTo(Session.class);
            // Remove the first / char - JCR API does not respect that
            String newPath = path.replaceFirst(DITAConstants.FORWARD_SLASH, StringUtils.EMPTY);

            // Use JCR API to read property to check folder type.
            Node root = session.getRootNode();
            Node dataPathNode = root.getNode(newPath);

            String property = dataPathNode.getProperty(JcrConstants.JCR_PRIMARYTYPE).getString();
            // only perform if the asset is folder.
            if (property.isEmpty() || !dataPathNode.hasNodes()) {
                return;
            }
            // Iterate each DITA file and do processing
            NodeIterator linksIterator = dataPathNode.getNodes();
            if (null != linksIterator) {
                while (linksIterator.hasNext()) {
                    processDitaFile(path, linksIterator.nextNode(), resolver);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            // close the service user resolver
            if (resolver.isLive()) {
                resolver.commit();
                resolver.close();
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
    private void processDitaFile(String path, Node child, ResourceResolver resolver) throws RepositoryException,
            IOException {

        // Process only for DITA files.
        if (!child.getName().endsWith(DITAConstants.DITA_EXTENSION)) {
            return;
        } else if (child.getProperty(JcrConstants.JCR_PRIMARYTYPE).getString().equals(DamConstants.NT_DAM_ASSET)) {
            // Generate the unique ID as per PwC Topic template DTD.
            String id = "pwc-topic id=\"pwc-topic.dita_" + child.getUUID() + "\"";

            // Use AssetManager to get the DITA file from the provided folder.
            AssetManager assetMgr = resolver.adaptTo(AssetManager.class);

            Asset dita = assetMgr.getAsset(child.getPath());
            InputStream inputStream = dita.getRendition(DamConstants.ORIGINAL_FILE).getStream();
            StringWriter writer = new StringWriter();
            IOUtils.copy(inputStream, writer, Charset.forName(MadisonConstants.UTF_8));

            String theString = writer.toString();
            String pwcDtd = theString.split(NEW_LINE)[0];
            String pwcBody = theString.split(NEW_LINE)[1];
            String replacedString = pwcBody.replaceAll(PWC_ID_PATTERN, id)
                    .replaceAll(OBSOLETE_START_TAG_PATTERN, OBSOLETE_START_TAG)
                    .replaceAll(OBSOLETE_END_TAG_PATTERN, OBSOLETE_END_TAG);
            // Format the XML so that author can see the errors at multiple lines.
            XMLFormatter formatter = new XMLFormatter();
            String prettyXML = formatter.format(replacedString);
            prettyXML = prettyXML.substring(prettyXML.indexOf('\n') + 1);
            String processedDita = pwcDtd + "\n" + prettyXML;

            // use ByteArrayInputStream to get the bytes of the String and convert them to InputStream.
            InputStream assetStream = new ByteArrayInputStream(processedDita.getBytes(Charset
                    .forName(MadisonConstants.UTF_8)));
            // Set the modified stream back to DITA file.
            dita.setRendition(DamConstants.ORIGINAL_FILE, assetStream, dita.getValueMap());
        }
    }
}
