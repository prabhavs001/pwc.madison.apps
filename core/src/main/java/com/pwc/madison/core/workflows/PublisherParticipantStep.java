package com.pwc.madison.core.workflows;

import java.util.Arrays;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.ParticipantStepChooser;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.constants.WorkflowConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.services.MailSenderService;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.WorkFlowUtil;

@Component(service = ParticipantStepChooser.class, property = {
        "service.description"
                + "=PwC Simple WF Implementation of dynamic participant chooser.",
        "chooser.label" + "=PwC Publisher Participant Chooser"})
public class PublisherParticipantStep implements ParticipantStepChooser {
    private static final Logger logger = LoggerFactory
            .getLogger(PublisherParticipantStep.class);

    @Reference
	private ResourceResolverFactory resolverFactory;
    
    @Reference
    private MailSenderService mailSenderService;
    
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    @Override
    public String getParticipant(WorkItem workItem, WorkflowSession wfSession,
                                 MetaDataMap metaDataMap) throws WorkflowException {

        logger.debug("################ Inside the PwC Publisher ParticipantStepImpl GetParticipant ##########################");

        MetaDataMap meta = workItem.getWorkflowData().getMetaDataMap();
        if (meta == null) {
            logger.error("PwC ParticipantStepImpl Workflow metadata null. Cannot proceed with review.");
            return null;
        }
        final WorkflowData wfData = workItem.getWorkflowData();
        String[] participants = meta.get(DITAConstants.PUBLISHER, String.class).split(",");
        String participantGroup = DITAUtils.createParentGroup(participants,resolverFactory,DITAConstants.PUBLISHER, workItem.getWorkflow().getId(), madisonSystemUserNameProviderService.getFmditaServiceUsername());

        String wfStatus = WorkflowConstants.EMAIL_PUBLISHER_SUBJECT;

        String[] combinedReviewList = WorkFlowUtil.getReviewItems(meta);
        if (combinedReviewList.length > 0) {
/*            WorkFlowUtil.sendEmailNotification(mailSenderService, resolverFactory, participants, wfStatus, combinedReviewList,
                    WorkflowConstants.EMAIL_PUBLISH_BODY_TITLE);*/
        }
        // Reset the comment property in the workflow metadata, all Process steps in the workflow would eventually
        // have the same static comment
        WorkFlowUtil.updateWorkflowComment(wfData,wfSession, MadisonConstants.WORKFLOW_SYSTEM_USER_COMMENT,workItem);
        
        logger.debug("####### PwC Publisher Participant : " + Arrays.toString(participants) + " ##############");
        return participantGroup;
    }
}
