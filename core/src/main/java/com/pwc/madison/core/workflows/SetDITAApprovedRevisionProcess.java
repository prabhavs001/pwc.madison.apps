package com.pwc.madison.core.workflows;

import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Process to create auto version/revision for the DITA/DITAMAP with "Approved" label.
 *
 * @author kau
 */
@Component(service = WorkflowProcess.class,
           property = { "process.label= Madision - Create Approved revision" })
public class SetDITAApprovedRevisionProcess implements WorkflowProcess {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    @Reference
    private ResourceResolverFactory resolverFactory;
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    @Override
    public void execute(WorkItem item, WorkflowSession wfsession, MetaDataMap args) throws WorkflowException {

        ResourceResolver resolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
        Session session = resolver.adaptTo(Session.class);
        MetaDataMap meta = item.getWorkflowData().getMetaDataMap();
        if (meta == null || null == session) {
            log.error("Workflow metadata/session is null. Cannot proceed with setting revision.");
            return;
        }
        String orgTopics = meta.get(DITAConstants.ORIGINAL_TOPICS, String.class);
        String reviewMaps = meta.get(DITAConstants.REVIEW_DITAMAPS, String.class);
        // Create new revision and set label as "Approved"
        try {
            if (reviewMaps != null && !reviewMaps.isEmpty()) {
                String[] reviewMapsList = reviewMaps.split("\\|");
                setNewRevisionForTopic(reviewMapsList, resolver, session);
            }
            if (orgTopics != null && !orgTopics.isEmpty()) {
                String[] orgTopicsList = orgTopics.split("\\|");
                setNewRevisionForTopic(orgTopicsList, resolver, session);
            }
        } finally {
            if (resolver.isLive()) {
                session.logout();
                resolver.close();
            }
        }
    }

    /**
     * @param orgTopicsList
     * @param resolver
     * @param session
     */
    private void setNewRevisionForTopic(String[] orgTopicsList, ResourceResolver resolver, Session session) {

        for (String path : orgTopicsList) {
            Resource res = resolver.getResource(path);
            try {
                DITAUtils.createRevision(path, MadisonConstants.APPROVED, "Auto Increment from workflow", res, session);
                session.save();
            } catch (Exception ex) {
                log.error("Failed to create new version for DITA : " + path, ex);
                continue;
            }
        }
    }
}
