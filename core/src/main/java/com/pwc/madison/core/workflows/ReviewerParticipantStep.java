package com.pwc.madison.core.workflows;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.HistoryItem;
import com.adobe.granite.workflow.exec.ParticipantStepChooser;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.constants.WorkflowConstants;
import com.pwc.madison.core.services.CreateReviewService;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.services.MailSenderService;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.WorkFlowUtil;

@Component(service = ParticipantStepChooser.class, property = {
    "service.description"
        + "=PwC Simple WF Implementation of dynamic participant chooser.",
    "chooser.label" + "=PwC Reviewer Participant Chooser"})
public class ReviewerParticipantStep implements ParticipantStepChooser {
    private static final Logger logger = LoggerFactory
        .getLogger(ReviewerParticipantStep.class);
    public static final String PWC_SIMPLIFIED_WF = "PwC Simplified WF";

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private MailSenderService mailSenderService;

    @Reference
    private CreateReviewService createReviewService;

    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    @Override
    public String getParticipant(WorkItem workItem, WorkflowSession wfSession,
                                 MetaDataMap metaDataMap) throws WorkflowException {

        logger.debug("################ Inside the PwC Reviewer ParticipantStepImpl GetParticipant ##########################");

        MetaDataMap meta = workItem.getWorkflowData().getMetaDataMap();
        String wfStatus = StringUtils.EMPTY;
        String wfBodyTitle = StringUtils.EMPTY;
        if (meta == null) {
            logger.error("PwC ParticipantStepImpl Workflow metadata null. Cannot proceed with review.");
            return null;
        }


        List<HistoryItem> historyItems = wfSession.getHistory(workItem.getWorkflow());
        //historyItems.size()<1 makes sure that this is the first step of the Full-cycle or Collab workflow or if it is just Simplified workflow we allow it
        String modelTitle = workItem.getWorkflow().getWorkflowModel().getTitle();
        if (modelTitle.equals(PWC_SIMPLIFIED_WF) || historyItems.size() < 1) {
            //Preparing the metadata required for review page
            //last parameter is flag which determines whether the status needs to be set to In_Review
            createReviewService.createReview(workItem, wfSession, meta, true);

            //setting the comment given by author to the workflow item's comment tab
            if (meta.containsKey(DITAConstants.DESCRIPTION)) {
                String comment = meta.get(DITAConstants.DESCRIPTION).toString();
                workItem.getMetaDataMap().put(MadisonConstants.COMMENT, comment);
            }
            wfStatus = WorkflowConstants.EMAIL_REVIEW_SUBJECT;
            wfBodyTitle = WorkflowConstants.EMAIL_REVIEW_BODY_TITLE;
        }

        if(workItem.getNode().getTitle().equals(WorkflowConstants.GOTO_REVIEWER_STEP)){
            wfStatus = WorkflowConstants.EMAIL_BACK_REVIEWER_SUBJECT;
            wfBodyTitle = WorkflowConstants.EMAIL_BACK_AUTHOR_BODY_TITLE;
        }

        final WorkflowData wfData = workItem.getWorkflowData();

        String[] participants = meta.get(DITAConstants.REVIEWER, String.class).split(",");
        String assigneeName = "";
        if (!modelTitle.equals(PWC_SIMPLIFIED_WF)) {
            assigneeName = DITAUtils.createParentGroup(participants, resolverFactory, DITAConstants.REVIEWER, workItem.getWorkflow().getId(), madisonSystemUserNameProviderService.getFmditaServiceUsername());
        }else{
            if (participants!=null && participants.length>0){
                assigneeName = participants[0];
            }
        }
        String[] combinedReviewList = WorkFlowUtil.getReviewItems(meta);
        if (combinedReviewList.length > 0) {
            WorkFlowUtil.sendEmailNotification(mailSenderService, resolverFactory, participants, wfStatus, combinedReviewList,
                    wfBodyTitle, madisonSystemUserNameProviderService.getFmditaServiceUsername());
        }
        // Reset the comment property in the workflow metadata, all Process steps in the workflow would eventually
        // have the same static comment
        WorkFlowUtil.updateWorkflowComment(wfData,wfSession,MadisonConstants.WORKFLOW_SYSTEM_USER_COMMENT,workItem);

        logger.debug("####### PwC Reviewer Participant : {} ##############", participants);
        return assigneeName;
    }
}
