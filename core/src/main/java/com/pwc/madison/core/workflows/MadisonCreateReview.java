package com.pwc.madison.core.workflows;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.CreateReviewService;

//This custom workflow step will be used to prepare selected workflow content for collaboration
@Component(service = WorkflowProcess.class,
           property = { "process.label=Madison - Create review" })

public class MadisonCreateReview implements WorkflowProcess {

    private static final Logger logger = LoggerFactory.getLogger(MadisonCreateReview.class);

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private CreateReviewService createReviewService;

    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap args) throws WorkflowException {

        MetaDataMap meta = workItem.getWorkflowData().getMetaDataMap();
        if (meta == null) {
            logger.error("PwC Collaboration Workflow metadata null. Cannot proceed with review.");
        }

        createReviewService.createReview(workItem, workflowSession, meta, false);

        //setting the comment given by author to the workflow item's comment tab
        if (meta.containsKey(DITAConstants.DESCRIPTION)) {
            String comment = meta.get(DITAConstants.DESCRIPTION).toString();
            workItem.getMetaDataMap().put(MadisonConstants.COMMENT, comment);
        }
    }

}
