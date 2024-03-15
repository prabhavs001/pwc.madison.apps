package com.pwc.madison.core.workflows;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.ParticipantStepChooser;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.WorkFlowUtil;

@Component(
    service = ParticipantStepChooser.class,
    property = { "service.description" + "=Dynamic Participant Chooser for Content Approver.",
            "chooser.label" + "=PwC Content Approver Participant Chooser" })
public class ContentApproverParticipantStep implements ParticipantStepChooser {

    private static final String PN_ROOT = "root";
    private static final String JCR_CONTENT_VLT_DEFINITION_FILTER_RESOURCE = "/jcr:content/vlt:definition/filter/resource";
    private static final String ETC_WORKFLOW_PACKAGES = "/etc/workflow/packages";
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentApproverParticipantStep.class);

    @Override
    public String getParticipant(final WorkItem workItem, final WorkflowSession workflowSession,
            final MetaDataMap metaDataMap) throws WorkflowException {

        final WorkflowData wfData = workItem.getWorkflowData();

        // Content approvers are relative to the Territory Hierarchy, so based on the payload, identify the territory
        // and return the approver participant accordingly.
        // If you follow the User groups, only the 'Territory Owners' (Territory Site managers) has the permission to
        // activate the content.
        // The territory Owners/Site manager group names in Madison has the following pattern 'us-madison-owner' ,
        // 'uk-madison-owner' etc
        // so based on the territory , the owner group will be automatically picked.
        // When there are resources which are not part of the territory hierarchy, 'administrators' will be added as the
        // approver for such requests.

        final String thPayload = (String) wfData.getPayload();
        String payloadPath = StringUtils.EMPTY;

        // Check if the Payload Path starts with Path: /etc/workflow/packages/generated-package
        if (thPayload.startsWith(ETC_WORKFLOW_PACKAGES)) {
            final Session session = workflowSession.adaptTo(Session.class);
            try {
                if (null != session && session.nodeExists(thPayload + JCR_CONTENT_VLT_DEFINITION_FILTER_RESOURCE)
                        && session.getNode(thPayload + JCR_CONTENT_VLT_DEFINITION_FILTER_RESOURCE)
                                .hasProperty(PN_ROOT)) {

                    payloadPath = session.getNode(thPayload + JCR_CONTENT_VLT_DEFINITION_FILTER_RESOURCE)
                            .getProperty(PN_ROOT).getString();
                }
            } catch (final RepositoryException e) {
                LOGGER.error("Error in Getting path for getParticipant :: {}", e);
            }

        } else {
            payloadPath = thPayload;
        }
        // get the territory code
        final String territoryCode = MadisonUtil.getTerritoryCodeFromPagePath(payloadPath);

        String ownerGroup;

        if (StringUtils.isNotEmpty(territoryCode)) {
            // based on the territory code , return the group name who is the territory Site Owner
            ownerGroup = territoryCode + "-territory-site-manager";
        } else {
            ownerGroup = MadisonConstants.USER_GROUPS_ADMINISTRATORS;
        }

        // Reset the comment property in the workflow metadata, all Process steps in the workflow would eventually
        // have the same static comment
        WorkFlowUtil.updateWorkflowComment(wfData, workflowSession, MadisonConstants.WORKFLOW_SYSTEM_USER_COMMENT,
                workItem);

        return ownerGroup;
    }
}
