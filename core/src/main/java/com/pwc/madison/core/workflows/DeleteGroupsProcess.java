package com.pwc.madison.core.workflows;

import javax.jcr.Session;

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
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.WorkFlowUtil;

//This is a component so it can provide or consume services
@Component(service = WorkflowProcess.class,
           property = { "process.label= Madision - Delete groups process" })
public class DeleteGroupsProcess implements WorkflowProcess {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    private Session session = null;
    @Reference
    private ResourceResolverFactory resolverFactory;
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    @Override
    public void execute(WorkItem item, WorkflowSession wfsession, MetaDataMap args) throws WorkflowException {

        String workflowId = WorkFlowUtil.getUniqueWorkId(item.getWorkflow().getId());

        String reviewerGroup = DITAConstants.MAC_DEFAULT.concat("-").concat(DITAConstants.REVIEWER).concat("-")
                .concat(workflowId);
        String approverGroup = DITAConstants.MAC_DEFAULT.concat("-").concat(DITAConstants.APPROVER).concat("-")
                .concat(workflowId);
        String publisherGroup = DITAConstants.MAC_DEFAULT.concat("-").concat(DITAConstants.PUBLISHER).concat("-")
                .concat(workflowId);
        String rejectionGroup = DITAConstants.MAC_DEFAULT.concat("-").concat(DITAConstants.REJECTION_LIST).concat("-")
                .concat(workflowId);

        DITAUtils.deleteGroup(reviewerGroup, resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
        DITAUtils.deleteGroup(approverGroup, resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
        DITAUtils.deleteGroup(publisherGroup, resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
        DITAUtils.deleteGroup(rejectionGroup, resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());

        //Set status to completed for simple/full cycle workflows
        MetaDataMap meta = item.getWorkflowData().getMetaDataMap();
        ResourceResolver resourceResolver = MadisonUtil
                .getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
        try {
            if (null != resourceResolver) {
                session = resourceResolver.adaptTo(Session.class);
                Object orgNode = meta.get(DITAConstants.ORIGINAL_TOPICS);
                String[] orgTopics = {};
                if (orgNode != null) {
                    orgTopics = orgNode.toString().split("\\" + DITAConstants.ITEM_SEPARATOR);
                    for (int i = 0; i < orgTopics.length; i++) {
                        if (!orgTopics[i].isEmpty()) {
                            WorkFlowUtil.setStatus(orgTopics[i], session, DITAConstants.COMPLETE_STATUS);
                        }
                    }
                }
                Boolean isDitamap = meta.get(DITAConstants.IS_DITAMAP, Boolean.class);
                if (isDitamap == null) {
                    isDitamap = false;
                }
                String ditamap = null;
                if (isDitamap) {
                    ditamap = meta.get(DITAConstants.DITAMAP, String.class);
                    WorkFlowUtil.setStatus(ditamap, session, DITAConstants.COMPLETE_STATUS);
                }
            }
        } finally {
            if (resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }
    }
}
