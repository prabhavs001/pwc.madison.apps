package com.pwc.madison.core.workflows;

import com.day.cq.replication.AgentFilter;
import com.day.cq.replication.AgentManager;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.day.cq.search.QueryBuilder;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.beans.PostProcessing;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.List;
import java.util.UUID;

@Component(service = WorkflowProcess.class, property = { "process.label = PwC Madison - Replicate Pages" })
public class ReplicatePublishingPoint implements WorkflowProcess {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    // private static final String CONTENT_FMDITACUSTOM = "/content/fmditacustom";
    private static final String GENERATED_PATH = "generatedPath";
    private static final String OUTPUT_NAME = "outputName";
    private static final String IS_SUCCESS = "isSuccess";
    private static final String HTML_EXTENSION = ".html";
    private static final String REP_AGENT = "pwc-ditacontent-replication-agent-%s";
    private static final String DITAOT_FAILURE = "ditaotFaliure";
    public static final String POSTPROCESSING_LOG_FILENAME = "postprocessing-logs.txt";
    public static final String TEXT_PLAIN = "text/plain";

    @Reference
    private ResourceResolverFactory resolverFactory;
    @Reference
    private Replicator replicator;
    @Reference
    private QueryBuilder queryBuilder;
    @Reference
    private AgentManager agentMgr;
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    @Override
    public void execute(final WorkItem item, final WorkflowSession wfsession, final MetaDataMap args)
            throws WorkflowException {
        String outputHistoryPath = StringUtils.EMPTY;
        ResourceResolver pkgResolver = null;
        ResourceResolver resolver = null;
        Session pkgSession = null;
        Session session = null;
        JcrPackageManager pkgMgr = null;
        JcrPackage pkg = null;
        final long startTime = System.currentTimeMillis();
        PostProcessing postProcessing = new PostProcessing();
        log.debug("Start time in miliseconds before package creation {}", startTime);
        try {
            final MetaDataMap meta = item.getWorkflowData().getMetaDataMap();

            if (null == meta) {
                log.error("Workflow metadata null. Cannot proceed with Update.");
                return;
            }

            final String ditaOTGenerationSuccess = meta.get(IS_SUCCESS, String.class);
            final String outputPath = meta.get(GENERATED_PATH, String.class);
            final String outputName = meta.get(OUTPUT_NAME, String.class);
            Boolean failure = meta.get(MadisonConstants.FAILURE, Boolean.class);
            if(null == failure){
                return;
            }
            postProcessing.setFailure(failure);
            final String exceptionString = meta.get(MadisonConstants.EXCEPTIONS, String.class);
            if(null != exceptionString){
                postProcessing.appendException(exceptionString);
                log.error("Error in UpdateRelatedContent step, so stopping replication step");
            }

            outputHistoryPath = meta.get(DITAConstants.OUTPUT_HISTORY_PATH, String.class);
            resolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
            session = resolver.adaptTo(Session.class);

            log.debug("Update Page Properties Workflow ditaOTGenerationSuccess ::: {} ", ditaOTGenerationSuccess);
            log.debug("Update Page Properties Workflow outputPath ::: {} ", outputPath);
            log.debug("Update Page Properties Workflow outputName ::: {} ", outputName);

            if( postProcessing.isFailure()){
                return;
            }

            if (StringUtils.isBlank(outputName) || "workflowtopicregeneration".equalsIgnoreCase(outputName)) {
                log.error("Skipping Package Replication for {}", outputName);
                return;
            }

            if (StringUtils.isBlank(outputName) || !"AEMSITE".equalsIgnoreCase(outputName)) {
                log.error("Output Name is Empty or not AEMSITE {}", outputName);
                postProcessing.setFailure(true);
                postProcessing.appendException("Replication Failed - Output Name is Empty or not AEMSITE: "+ outputName);
                return;
            }

            if (StringUtils.isBlank(ditaOTGenerationSuccess) || ditaOTGenerationSuccess.equals("false") || StringUtils.isBlank(outputPath)) {
                log.error(MadisonConstants.EXCEPTION_OUTPUT_TYPE_NULL);
                postProcessing.setFailure(true);
                postProcessing.appendException("Replication Failed - "+ MadisonConstants.EXCEPTION_OUTPUT_TYPE_NULL);
                return;
            }

            if (!outputPath.contains(HTML_EXTENSION)) {
                log.error("output path does not have html extension {} ", outputPath);
                postProcessing.setFailure(true);
                postProcessing.appendException("Replication Failed - output path does not have html extension: "+ outputPath);
                return;
            }

            final String repAgent = String.format(REP_AGENT, MadisonUtil.getTerritoryCodeForPathUrl(outputPath));
            final List<String> agentsNameList = MadisonUtil.getReplicationAgents(repAgent, agentMgr);

            if (agentsNameList.isEmpty()) {
                log.error("Replication agent: {} not found , so skipping replicating to publish", repAgent);
                postProcessing.setFailure(true);
                postProcessing.appendException("Replication Failed - Replication agent not found, so skipping replicating to publish: "+ repAgent);
                return;
            }

            final DefaultWorkspaceFilter dwf = new DefaultWorkspaceFilter();
            pkgResolver = MadisonUtil.getResourceResolver(resolverFactory,
                    madisonSystemUserNameProviderService.getReplicationServiceUsername());

            if (null == pkgResolver) {
                log.error("pckResolver null. Cannot proceed with Replicate Pages.");
                postProcessing.setFailure(true);
                postProcessing.appendException("Replication Failed - pckResolver null. Cannot proceed with Replicate Pages.");
                return;
            }
            pkgSession = pkgResolver.adaptTo(Session.class);

            final String sourcePath = outputPath.substring(0, outputPath.indexOf(HTML_EXTENSION));
            final ImportMode mode = ImportMode.REPLACE;

            final String uuid = UUID.randomUUID().toString();
            final PathFilterSet pfs = new PathFilterSet(sourcePath);
            pfs.setImportMode(mode);
            dwf.add(pfs);

            // final PathFilterSet pfs1 = new PathFilterSet(CONTENT_FMDITACUSTOM);
            // pfs1.setImportMode(mode);
            // dwf.add(pfs1);

            pkgMgr = PackagingService.getPackageManager(pkgSession);
            pkg = pkgMgr.create(MadisonConstants.AUTO_REPLICATION_PACKAGE_GROUP, uuid);
            log.debug("Package created and assembled at {}", pkg.getNode().getPath());
            pkg.getDefinition().setFilter(dwf, true);
            pkgMgr.assemble(pkg, null);

            for (final String agentName : agentsNameList) {
                final ReplicationOptions opts = new ReplicationOptions();
                opts.setFilter(new AgentFilter() {
                    @Override
                    public boolean isIncluded(final com.day.cq.replication.Agent repAgent) {
                        return repAgent.getId().equalsIgnoreCase(agentName);
                    }
                });
                replicator.replicate(pkgSession, ReplicationActionType.ACTIVATE, pkg.getNode().getPath(), opts);
            }

            // pkgMgr.remove(pkg);
        } catch (final Exception e) {
            postProcessing.setFailure(true);
            postProcessing.appendException("Error in Replicate Process:");
            postProcessing.appendLog(e);
            log.error("Error in Replicate Process: {}", e);

        } finally {
            if( postProcessing.isFailure() && null != postProcessing.getException() && StringUtils.isNotBlank(postProcessing.getException().toString())){
                MadisonUtil.setFailure(true, outputHistoryPath, session);
                MadisonUtil.storeErrorLog(resolver, postProcessing, outputHistoryPath, session);
                closeObjects(pkg, pkgSession, session, pkgResolver, resolver);
                return;
            }
            closeObjects(pkg, pkgSession, session, pkgResolver, resolver);
            log.debug("### Total Time taken in miliseconds for creating package and inovking replication {}",
                    System.currentTimeMillis() - startTime);
        }

    }
    
    private void closeObjects(final JcrPackage pkg, final Session pkgSession, final Session session, final ResourceResolver pkgResolver, final ResourceResolver resolver) {

		if (null != pkg) {
			pkg.close();
		}
		if (null != pkgSession) {
			pkgSession.logout();
		}
		if (null != session) {
			session.logout();
		}
		if (null != pkgResolver && pkgResolver.isLive()) {
			pkgResolver.close();
		}
		if (null != resolver && resolver.isLive()) {
			resolver.close();
		}
    	
    }

}
