package com.pwc.madison.core.services.impl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.timeline.Timeline;
import com.adobe.granite.timeline.TimelineAggregator;
import com.adobe.granite.timeline.TimelineEvent;
import com.adobe.granite.timeline.TimelineEventType;
import com.day.cq.commons.Externalizer;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.reports.models.WorkflowReportRow;
import com.pwc.madison.core.services.WorkflowReportService;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * @author sevenkat
 * 
 *         The Class WorkflowReportService is the OSGi service which will create a report of the workflows ran on a
 *         ditamap. Based on the selection criteria it return the workflow instances.
 */
@Component(service = WorkflowReportService.class, immediate = true)
public class WorkflowReportServiceImpl implements WorkflowReportService {

    private static final String START_TIME = "startTime";
    private static final String END_TIME = "endTime";
    private static final String DATE_FORMAT = "MM-dd-yyyy hh:mm a";
    private static final String PATH = "path";
    private static final String TRUE = "true";
    private static final String P_GUESS_TOTAL = "p.guessTotal";
    private static final String TYPE = "type";
    private static final String EVENT_TYPE = "com.adobe.granite.timeline.types.WorkflowTimelineEventType";
    private static final String[] wfModels = { "/var/workflow/models/pwc-collaboration-workflow",
            "/var/workflow/models/pwc-simplified-wf", "/var/workflow/models/pwc-fullcyle-workflow" };

    @Reference
    private QueryBuilder queryBuilder;
    
    @Reference
    ResourceResolverFactory resolverFactory;

    @Reference
    SlingSettingsService slingService;

    @Reference
    TimelineAggregator aggregator;

    @Reference
    Externalizer externalizer;

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowReportServiceImpl.class);

    @Override
    public List<Object> populateQueryResults(String assetPath, SlingHttpServletRequest request, int limit, int offset,
            List<Object> results) throws RepositoryException {
        if (null != request) {
            try {
                ResourceResolver resolver = MadisonUtil.getResourceResolver(resolverFactory, MadisonConstants.MADISON_GENERIC_SUB_SERVICE);
                final Session session = resolver.adaptTo(Session.class);
                final Map<String, String> map = new HashMap<>();
                map.put(PATH, assetPath);
                map.put(TYPE, DamConstants.NT_DAM_ASSET);
                map.put("orderby", "@jcr:content/metadata/dc:title");
                map.put(P_GUESS_TOTAL, TRUE);
                map.put("nodename", "*.ditamap");
                final Query query = queryBuilder.createQuery(PredicateGroup.create(map), session);
                query.setStart(offset);
                query.setHitsPerPage(limit);
                SearchResult searchResult = query.getResult();
                for (final Hit hit : searchResult.getHits()) {
                    String path = hit.getPath();
                    Resource res = resolver.getResource(path);
                    if (null != res) {
                        createReportForPath(res, results, resolver, request);
                    }
                }
            } catch (ParseException e) {
                LOGGER.error("Error occurred while formatting dates{}", e);
            }
        }
        return results;
    }

    /**
     * This method gets the Workflow Events only from time line and fetches the required information in case workflow
     * events empty,it populates the basic rows and return.
     * 
     * @param res
     * @param results
     * @param resolver
     * @param request
     * @throws ParseException
     */
    private void createReportForPath(Resource res, List<Object> results, ResourceResolver resolver,
            SlingHttpServletRequest request) throws ParseException {
        Timeline timeline = res.adaptTo(Timeline.class);
        if (null != timeline) {
            final TimelineEventType filterType = aggregator.getTypes().get(EVENT_TYPE);
            List<TimelineEvent> workflowEvents = timeline.getEvents(filterType);
            if (!workflowEvents.isEmpty()) {
                populateWorflowData(res, workflowEvents, results, resolver, request);
            } else {
                WorkflowReportRow row = new WorkflowReportRow();
                populateMetaFields(row, res, resolver);
                results.add(row);
            }
        }
    }

    /**
     * This method populates the basic Asset details.
     * 
     * @param row
     * @param res
     * @param resolver
     */
    private void populateMetaFields(WorkflowReportRow row, Resource res, ResourceResolver resolver) {
        row.setPath(res.getPath());
        Resource metaRes = resolver.getResource(res.getPath() + MadisonConstants.METADATA_PATH);
        Resource aemSiteRes = resolver.getResource(res.getPath() + DITAConstants.AEMSITE_PRESETS_NODE);
        if (null != metaRes) {
            row.setTitle(metaRes.getValueMap().get(DamConstants.DC_TITLE, String.class));
            row.setDocumentState(metaRes.getValueMap().get(DITAConstants.PN_METADATA_DOCSTATE, StringUtils.EMPTY));
            row.setContentStatus(metaRes.getValueMap().get(DITAConstants.PWC_CONTENT_STATUS, StringUtils.EMPTY));
        }
        if (null != aemSiteRes) {
            row.setPublishedDate(getDateTimeValue(DITAConstants.FMDITA_LAST_PUBLISHED, aemSiteRes));
        }
    }

    /**
     * Reverse Iterate the events based on date or row count selection as the recent workflow events will be added last
     * in the list.
     * 
     * @param res
     * @param events
     * @param results
     * @param resolver
     * @param request
     * @throws ParseException
     */
    private void populateWorflowData(Resource res, List<TimelineEvent> events, List<Object> results,
            ResourceResolver resolver, SlingHttpServletRequest request) throws ParseException {
        List<String> wfInstances = new ArrayList<>();
        Long fromTime = getTimeInMillis("fromDate", request);
        Long toTime = getTimeInMillis("toDate", request);
        String rows = request.getParameter("rows");
        int rowCount = StringUtils.isNotBlank(rows) ? Integer.parseInt(rows) : 1;
        if (null != fromTime && null != toTime) {
            for (int j = events.size() - 1; j >= 0; j--) {
                TimelineEvent event = events.get(j);
                if (event.getTime() >= fromTime && event.getTime() <= toTime) {
                    populateWorkflowEvents(res, event, resolver, results, wfInstances, rowCount);
                }
            }
        } else {
            for (int j = events.size() - 1; j >= 0; j--) {
                TimelineEvent event = events.get(j);
                populateWorkflowEvents(res, event, resolver, results, wfInstances, rowCount);
            }
        }
    }

    /**
     * Get all the required info needed for Workflow Report generation
     * 
     * @param res
     * @param event
     * @param resolver
     * @param results
     * @param wfInstances
     * @param rowCount
     */
    private void populateWorkflowEvents(Resource res, TimelineEvent event, ResourceResolver resolver,
            List<Object> results, List<String> wfInstances, int rowCount) {
        if (!wfInstances.contains(event.getOrigin()) && wfInstances.size() < rowCount) {
            WorkflowReportRow reportRow = new WorkflowReportRow();
            populateMetaFields(reportRow, res, resolver);
            Resource workflowRes = resolver.getResource(event.getOrigin());
            if (null != workflowRes) {
                String modelId = getValue("modelId", workflowRes);
                if (StringUtils.isNotBlank(modelId) && ArrayUtils.contains(wfModels, modelId)) {
                    Resource wfModel = resolver.getResource(modelId);
                    reportRow.setModelName(getValue("title", wfModel));
                    Resource metaDataRes = workflowRes.getChild("data/metaData");
                    reportRow.setApprovers(getValue("approver", metaDataRes));
                    reportRow.setCollaborators(getValue("collaborators", metaDataRes));
                    reportRow.setReviewers(getValue("reviewer", metaDataRes));
                    reportRow.setPublishers(getValue("publisher", metaDataRes));
                    reportRow.setInitiator(getValue("initiator", metaDataRes));
                    reportRow.setStartTime(getDateTimeValue(START_TIME, workflowRes));
                    populateEndTime(reportRow, workflowRes, metaDataRes, resolver);
                    Resource wfHistoryRes = workflowRes.getChild("history");
                    populateOtherTimes(wfHistoryRes, reportRow);
                }
            }
            results.add(reportRow);
            wfInstances.add(event.getOrigin());
        }
    }

    /**
     * As end time for Collaboration Workflow would be different and hence reading from Project task Other Workflows end
     * time is stored in the workflow node itself.
     * 
     * @param reportRow
     * @param workflowRes
     * @param metaDataRes
     * @param resolver
     */
    private void populateEndTime(WorkflowReportRow reportRow, Resource workflowRes, Resource metaDataRes,
            ResourceResolver resolver) {
        String task = getValue("projectTask", metaDataRes);
        if (StringUtils.isNotBlank(task)) {
            Resource taskRes = resolver.getResource(task);
            reportRow.setEndTime(getDateTimeValue(END_TIME, taskRes));
        } else {
            reportRow.setEndTime(getDateTimeValue(END_TIME, workflowRes));
        }

    }

    /**
     * Gets the Times based on the workflow steps transitions
     * 
     * @param wfHistoryRes
     * @param reportRow
     */
    private void populateOtherTimes(Resource wfHistoryRes, WorkflowReportRow reportRow) {
        if (null != wfHistoryRes) {
            Iterator<Resource> resIt = wfHistoryRes.listChildren();
            while (resIt.hasNext()) {
                Resource child = resIt.next().getChild("workItem");
                String title = getValue("_title", child);
                if (null != title) {
                    if (StringUtils.equalsIgnoreCase("send content for approval", title.trim())) {
                        reportRow.setLastReviewDate(getDateTimeValue(START_TIME, child));
                        reportRow.setLastReviewedDate(getDateTimeValue(END_TIME, child));
                    }
                    if (StringUtils.equalsIgnoreCase("Approve content", title.trim())) {
                        reportRow.setLastApprovedDate(getDateTimeValue(END_TIME, child));
                    }
                }
            }
        }
    }

    /**
     * @param key
     * @param workflowRes
     * @return
     */
    private String getDateTimeValue(String key, Resource workflowRes) {
        if (null != workflowRes && workflowRes.getValueMap().containsKey(key)) {
            Calendar cal = workflowRes.getValueMap().get(key, Calendar.class);
            if (null != cal) {
                return MadisonUtil.getDate(cal.getTime(), DATE_FORMAT);
            }
        }
        return null;
    }

    /**
     * @param key
     * @param resource
     * @return
     */
    private String getValue(String key, Resource resource) {
        if (null != resource && resource.getValueMap().containsKey(key)) {
            return resource.getValueMap().get(key, String.class);
        }
        return StringUtils.EMPTY;
    }

    /**
     * @param prop
     * @param request
     * @return
     * @throws ParseException
     */
    private Long getTimeInMillis(String prop, SlingHttpServletRequest request) throws ParseException {
        Long millis = null;
        String dateVal = request.getParameter(prop);
        if (null != dateVal) {
            Date date = MadisonUtil.FormatDeadline(dateVal);
            if (null != date) {
                return date.getTime();
            }
        }
        return millis;
    }
}
