package com.pwc.madison.core.workflows;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.Replicator;
import com.day.cq.search.QueryBuilder;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;

@Component(service = WorkflowProcess.class, property = { "process.label= PwC Madision - Purge Duplicate Page Paths" })
public class PurgeDuplicatePagePaths implements WorkflowProcess {

    private static final String PREVIEWSITE = "previewsite";
    private static final String AEMSITE = "AEMSITE";
    private static final String FMDITA_SITE_NAME = "fmdita-siteName";
    private static final String PN_FMDITA_TARGET_PATH = "fmdita-targetPath";

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private Replicator replicator;

    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    private static final String GENERATED_PATH = "generatedPath";
    private static final String OUTPUT_NAME = "outputName";
    private static final String IS_SUCCESS = "isSuccess";

    @Override
    public void execute(final WorkItem item, final WorkflowSession wfsession, final MetaDataMap args)
            throws WorkflowException {

        ResourceResolver resolver = null;
        Session session = null;
        final long startTime = System.currentTimeMillis();

        try {

            final MetaDataMap meta = item.getWorkflowData().getMetaDataMap();

            if (null == meta) {
                log.error("Error in PurgeDuplicatePagePaths metadata empty {}", meta);
                return;
            }

            log.info("PurgeDuplicatePagePaths metadata info ::: {} ", meta);
            final String outputType = meta.get(IS_SUCCESS, String.class);
            final String outputPath = meta.get(GENERATED_PATH, String.class);
            final String outputName = meta.get(OUTPUT_NAME, String.class);

            if (StringUtils.isBlank(outputType) || StringUtils.isBlank(outputPath)
                    || !outputPath.contains(DITAConstants.HTML_EXT)
                    || !(AEMSITE.equalsIgnoreCase(outputName) || PREVIEWSITE.equalsIgnoreCase(outputName))) {
                log.error("Error for generated Page PATH {} for outputName {} which is {}",
                        new Object[] { outputPath, outputName, outputType });
                return;
            }

            final String publishMapPath = (String) item.getWorkflowData().getPayload();
            final String generatedPagePath = outputPath.substring(0, outputPath.indexOf(DITAConstants.HTML_EXT));

            log.debug("generatedPagePath :: {}", generatedPagePath);

            resolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
            if (null == resolver) {
                log.error(" ResourceResolver null in SyndicationPeerLinkFixProcess for user {}",
                        madisonSystemUserNameProviderService.getFmditaServiceUsername());
                return;
            }
            session = resolver.adaptTo(Session.class);

            if (StringUtils.isBlank(publishMapPath)) {
                log.error("Error As No publishMapPath found {} for path {}", publishMapPath, generatedPagePath);
                return;
            }

            if (session.itemExists(publishMapPath)) {

                String mapNodePath = publishMapPath;
                if (AEMSITE.equalsIgnoreCase(outputName)) {
                    mapNodePath = publishMapPath + DITAConstants.AEMSITE_PRESETS_NODE;
                } else if (PREVIEWSITE.equalsIgnoreCase(outputName)) {
                    mapNodePath = publishMapPath + "/jcr:content/metadata/namedoutputs/previewsite";
                }

                final Node mapNode = session.getNode(mapNodePath);
                String configuredPath = StringUtils.EMPTY;

                if (null != mapNode && mapNode.hasProperty(PN_FMDITA_TARGET_PATH)) {
                    configuredPath = mapNode.getProperty(PN_FMDITA_TARGET_PATH).getString();
                    if (mapNode.hasProperty(FMDITA_SITE_NAME)
                            && StringUtils.isNotBlank(mapNode.getProperty(FMDITA_SITE_NAME).getString())) {
                        configuredPath = configuredPath + DITAConstants.FORWARD_SLASH
                                + mapNode.getProperty(FMDITA_SITE_NAME).getString();

                    } else {
                        configuredPath = configuredPath + generatedPagePath
                                .substring(generatedPagePath.lastIndexOf(DITAConstants.FORWARD_SLASH));
                    }

                    configuredPath = configuredPath.replace("//", DITAConstants.FORWARD_SLASH).replace(" ", "_");
                    log.debug("Configured Path: {}", configuredPath);

                    if (checkDupPagePath(generatedPagePath, configuredPath) && session.itemExists(configuredPath)) {
                        log.error("The page Path:: {} has duplicate path:: {} which will be deleted", generatedPagePath,
                                configuredPath);
                        replicator.replicate(session, ReplicationActionType.DEACTIVATE, configuredPath);
                        session.getNode(configuredPath).remove();
                        session.save();
                    }
                }
            }

        } catch (final Exception e) {
            log.error("Error in PurgeDuplicatePagePaths {} ", e);
        } finally {
            if (null != resolver && resolver.isLive()) {
                resolver.close();
            }
            log.error("Total Time taken(in Milisecond) for PurgeDuplicatePagePaths :::: {}",
                    System.currentTimeMillis() - startTime);
        }

    }

    private boolean checkDupPagePath(final String generatedPagePath, final String configuredPagePath) {

        final Pattern pattern = Pattern.compile("[^a-zA-Z0-9\\/_-]");
        final Matcher matcher = pattern.matcher(configuredPagePath);
        if (matcher.find() || !generatedPagePath.equals(configuredPagePath)) {
            return true;
        } else {
            return false;
        }

    }

}
