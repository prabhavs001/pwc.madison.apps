package com.pwc.madison.core.workflows;

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
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.WorkFlowUtil;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.util.List;

@Component(service = ParticipantStepChooser.class, property = {
    "service.description"
        + "=PwC Simple WF Implementation of dynamic participant chooser.",
    "chooser.label" + "=PwC Approver Participant Chooser"})
public class ApproverParticipantStep implements ParticipantStepChooser {
    private static final Logger logger = LoggerFactory
        .getLogger(ApproverParticipantStep.class);


    @Reference
    private MailSenderService mailSenderService;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private CreateReviewService createReviewService;
    
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    @Override
    public String getParticipant(WorkItem workItem, WorkflowSession wfSession,
                                 MetaDataMap metaDataMap) throws WorkflowException {

        logger.debug("################ Inside the PwC Approver ParticipantStepImpl GetParticipant ##########################");

        MetaDataMap meta = workItem.getWorkflowData().getMetaDataMap();
        if (meta == null) {
            logger.error("PwC ParticipantStepImpl Workflow metadata null. Cannot proceed with review.");
            return null;
        }

        List<HistoryItem> historyItems = wfSession.getHistory(workItem.getWorkflow());
        //historyItems.size()<1 makes sure that this is the first step of the workflow
        if (historyItems.size() < 1) {
            //Preparing the metadata required for review page
            //last parameter is flag which determines whether the status needs to be set to In_Review
            createReviewService.createReview(workItem, wfSession, meta, false);

            //setting the comment given by author to the workflow item's comment tab
            if (meta.containsKey(DITAConstants.DESCRIPTION)) {
                String comment = meta.get(DITAConstants.DESCRIPTION).toString();
                workItem.getMetaDataMap().put(MadisonConstants.COMMENT, comment);
            }
        } else {
            setDocStateToReviewed(meta);
        }
        final WorkflowData wfData = workItem.getWorkflowData();
        String[] participants = meta.get(DITAConstants.APPROVER, String.class).split(",");
        String participantGroup = DITAUtils.createParentGroup(participants, resolverFactory, DITAConstants.APPROVER, workItem.getWorkflow().getId(), madisonSystemUserNameProviderService.getFmditaServiceUsername());
        String wfStatus = WorkflowConstants.EMAIL_APPROVER_SUBJECT;

        String[] combinedReviewList = WorkFlowUtil.getReviewItems(meta);
        if (combinedReviewList.length > 0) {
/*            WorkFlowUtil
                    .sendEmailNotification(mailSenderService, resolverFactory, participants, wfStatus, combinedReviewList,
                            WorkflowConstants.EMAIL_APPROVAL_BODY_TITLE);*/
        }

        // Reset the comment property in the workflow metadata, all Process steps in the workflow would eventually
        // have the same static comment
        WorkFlowUtil.updateWorkflowComment(wfData, wfSession, MadisonConstants.WORKFLOW_SYSTEM_USER_COMMENT, workItem);

        logger.debug("####### PwC Approver Participant : " + participants + " ##############");
        return participantGroup;
    }

    private void setDocStateToReviewed(MetaDataMap meta) {
        ResourceResolver resolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());

        try {
            Session session = resolver.adaptTo(Session.class);
            // Setting doc state to in-review only for full cycle review workflow
            String docState = DITAConstants.DITA_DOCUMENTSTATE_REVIEWED;

            String orgTopics = meta.get(DITAConstants.ORIGINAL_TOPICS, String.class);
            String[] orgTopicsList = orgTopics.split("\\|");

            DITAUtils.setDocStates(orgTopicsList, docState, session, true, true, null);


            Boolean isDitamap = meta.get(DITAConstants.IS_DITAMAP, Boolean.class);
            if (isDitamap != null && isDitamap) {
                String ditamap = meta.get(DITAConstants.DITAMAP, String.class);
                DITAUtils.setDocStates(new String[]{ditamap}, docState, session, true, true, null);
            }
        } catch (RepositoryException e) {
            if (null != resolver && resolver.isLive()) {
                resolver.close();
            }
        }
    }
}
