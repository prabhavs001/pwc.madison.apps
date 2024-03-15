/**
 * 
 */
package com.pwc.madison.core.reports.models.impl;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.pwc.madison.core.beans.AuditReportRow;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.reports.models.ActivitiesAuditReportModel;

@Model(adaptables = SlingHttpServletRequest.class, adapters = ActivitiesAuditReportModel.class)
public class ActivitiesAuditReportModelImpl implements ActivitiesAuditReportModel {

    @Self
    private SlingHttpServletRequest request;

    @ValueMapValue
    private String value;

    private String activityFieldValue;

    /**
     * Init Method
     * 
     */
    @PostConstruct
    private void init() {
        AuditReportRow eachRow = (AuditReportRow) request.getAttribute("result");
        switch (value) {
            case MadisonConstants.TITLE_NODE:
                activityFieldValue = eachRow.getTitle();
                break;
            case DITAConstants.PATH_PROP_NAME:
                activityFieldValue = eachRow.getPath();
                break;
            case MadisonConstants.PN_USERS:
                activityFieldValue = eachRow.getCreatedBy();
                break;
            case MadisonConstants.INPUT_DATE:
                activityFieldValue = eachRow.getModifiedDate();
                break;
            case MadisonConstants.ACTIVITY:
                activityFieldValue = eachRow.getActivity();
                break;
            default:
                activityFieldValue = null;
                break;
        }

    }

    /**
     * @return the activityFieldValue
     */
    @Override
    public String getActivityFieldValue() {
        return activityFieldValue;
    }
}
