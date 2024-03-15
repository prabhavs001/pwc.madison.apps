/**
 * 
 */
package com.pwc.madison.core.reports.models.impl;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.pwc.madison.core.reports.models.WorkflowReportModel;
import com.pwc.madison.core.reports.models.WorkflowReportRow;

@Model(adaptables = SlingHttpServletRequest.class, adapters = WorkflowReportModel.class)
public class WorkflowReportModelImpl implements WorkflowReportModel {

    @Self
    private SlingHttpServletRequest request;

    @ValueMapValue
    private String value;

    private String rowValue;

    @PostConstruct
    private void init() {
        WorkflowReportRow eachRow = (WorkflowReportRow) request.getAttribute("result");
        switch (value) {
            case "title":
                rowValue = eachRow.getTitle();
                break;
            case "path":
                rowValue = eachRow.getPath();
                break;
            case "docState":
                rowValue = eachRow.getDocumentState();
                break;
            case "contentStatus":
                rowValue = eachRow.getContentStatus();
                break;
            case "wfModelName":
                rowValue = eachRow.getModelName();
                break;
            case "initiator":
                rowValue = eachRow.getInitiator();
                break;
            case "collaborators":
                rowValue = eachRow.getCollaborators();
                break;
            case "wfStartTime":
                rowValue = eachRow.getStartTime();
                break;
            case "wfEndTime":
                rowValue = eachRow.getEndTime();
                break;
            case "lastReviewDate":
                rowValue = eachRow.getLastReviewDate();
                break;
            case "lastReviewedDate":
                rowValue = eachRow.getLastReviewedDate();
                break;
            case "reviewers":
                rowValue = eachRow.getReviewers();
                break;
            case "lastApprovedDate":
                rowValue = eachRow.getLastApprovedDate();
                break;
            case "approvers":
                rowValue = eachRow.getApprovers();
                break;
            case "publishedDate":
                rowValue = eachRow.getPublishedDate();
                break;
            case "publishers":
                rowValue = eachRow.getPublishers();
                break;
            default:
                rowValue = StringUtils.EMPTY;
                break;
        }
    }

    @Override
    public String getRowValue() {
        return rowValue;
    }
}
