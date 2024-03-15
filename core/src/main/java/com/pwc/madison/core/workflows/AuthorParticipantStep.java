package com.pwc.madison.core.workflows;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.ParticipantStepChooser;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.WorkflowConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.services.MailSenderService;
import com.pwc.madison.core.util.WorkFlowUtil;

@Component(service = ParticipantStepChooser.class, property = {
        "service.description" + "=PwC Approval workflow dynamic participant chooser.",
        "chooser.label" + "=PwC Initiator Participant Chooser" }) public class AuthorParticipantStep
        implements ParticipantStepChooser {
    private static final Logger logger = LoggerFactory.getLogger(AuthorParticipantStep.class);

    @Reference private MailSenderService mailSenderService;

    @Reference private ResourceResolverFactory resolverFactory;
    
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    @Override public String getParticipant(WorkItem workItem, WorkflowSession wfSession, MetaDataMap metaDataMap)
            throws WorkflowException {

        logger.debug(
                "################ Inside the PwC Initiator ParticipantStepImpl GetParticipant ##########################");

        MetaDataMap meta = workItem.getWorkflowData().getMetaDataMap();
        if (meta == null) {
            logger.error("PwC Initiator ParticipantStepImpl Workflow metadata null. Cannot proceed with review.");
            return null;
        }
        String participant = meta.get(DITAConstants.INITIATOR, String.class);
        String[] participants = { participant };
        String wfStatus = StringUtils.EMPTY;
        String wfBodyTitle = StringUtils.EMPTY;
        if (workItem.getNode().getTitle().equals(WorkflowConstants.SEND_BACK_TO_AUTHOR_STEP)) {
            wfStatus = WorkflowConstants.EMAIL_BACK_AUTHOR_SUBJECT;
            wfBodyTitle = WorkflowConstants.EMAIL_BACK_AUTHOR_BODY_TITLE;
        } else {
            wfStatus = metaDataMap.get("PROCESS_ARGS", String.class);
        }
        String[] combinedReviewList = WorkFlowUtil.getReviewItems(meta);
        if (combinedReviewList.length > 0) {
            WorkFlowUtil.sendEmailNotification(mailSenderService, resolverFactory, participants, wfStatus, combinedReviewList,
                    wfBodyTitle, madisonSystemUserNameProviderService.getFmditaServiceUsername());
        }

        logger.debug("####### PwC Initiator Participant : " + participant + " ##############");
        return participant;
    }

}
