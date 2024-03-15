/**
 * 
 */
package com.pwc.madison.core.reports.columns;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.pwc.madison.core.reports.models.WorkflowReportRow;

/**
 * This CSV exporter reads the value from request attribute and based on the selection of dropdown value fetches the
 * results from request object and export to a column in an Spread sheet.
 */
@Model(adaptables = Resource.class)
public class WorkflowReportCSVExporter implements ReportCellCSVExporter {

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String value;

    @Override
    public String getValue(Object result) {
        String rowValue;
        WorkflowReportRow eachRow = (WorkflowReportRow) result;
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
        return rowValue;
    }
}
