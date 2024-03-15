package com.pwc.madison.core.workflows;

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.MadisonConstants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


//Workflow service which sets the flag to take the flow back to reviewer
@Component(service = WorkflowProcess.class,
    property = {"process.label= Madision - Set go back flag process"})
public class SetGoBackFlagProcess implements WorkflowProcess {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void execute(WorkItem workItem, WorkflowSession wfsession, MetaDataMap args) throws WorkflowException {

        MetaDataMap meta = workItem.getWorkflowData().getMetaDataMap();
        meta.put("goBackFlag", "true");

        workItem.getMetaDataMap().put(MadisonConstants.COMMENT, MadisonConstants.WORKFLOW_SYSTEM_USER_COMMENT);
    }
}