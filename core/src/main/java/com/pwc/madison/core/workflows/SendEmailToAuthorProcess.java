package com.pwc.madison.core.workflows;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.WorkflowConstants;
import com.pwc.madison.core.services.MailSenderService;
import com.pwc.madison.core.util.WorkFlowUtil;

//This is a component so it can provide or consume services
@Component(service = WorkflowProcess.class,
           property = { "process.label= Madison - Send mail to author" })
public class SendEmailToAuthorProcess implements WorkflowProcess {

    @Reference
    private MailSenderService mailSenderService;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public void execute(WorkItem workItem, WorkflowSession wfsession, MetaDataMap metaDataMap)
            throws WorkflowException {
        MetaDataMap meta = workItem.getWorkflowData().getMetaDataMap();
        String participant = meta.get(DITAConstants.INITIATOR, String.class);
        String[] participants = { participant };
        String wfStatus = WorkflowConstants.EMAIL_ENGAGE_SUBJECT;
        String wfBody = WorkflowConstants.EMAIL_ENGAGE_BODY_TITLE;
        if (workItem.getNode().getTitle().equals(WorkflowConstants.SEND_AUTHOR_EMAIL_STEP)) {
            wfStatus = WorkflowConstants.EMAIL_AUHTOR_FINAL_SUBJECT;
            wfBody = WorkflowConstants.EMAIL_AUHTOR_FINAL_BODY_TITLE;

        }
        String[] combinedReviewList = WorkFlowUtil.getReviewItems(meta);
        if (combinedReviewList.length > 0) {
            /*WorkFlowUtil.sendEmailNotification(mailSenderService, resolverFactory, participants, wfStatus,
                    combinedReviewList, wfBody);*/
        }
    }
}
