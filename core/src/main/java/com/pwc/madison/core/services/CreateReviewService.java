package com.pwc.madison.core.services;

import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.metadata.MetaDataMap;

public interface CreateReviewService {
    void createReview(WorkItem item, WorkflowSession wfsession, MetaDataMap meta, Boolean isReview);
}
