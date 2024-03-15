package com.pwc.madison.core.models.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.jcr.Session;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.constants.WorkflowConstants;
import com.pwc.madison.core.models.DitamapVersionDiffModel;
import com.pwc.madison.core.util.MadisonUtil;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = DitamapVersionDiffModel.class,
       resourceType = BasePageModelImpl.RESOURCE_TYPE)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
          extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class DitamapVersionDiffModelImpl implements DitamapVersionDiffModel {
    private static final Logger LOG = LoggerFactory.getLogger(DitamapVersionDiffModel.class);
    List<String> referencedDitamapsList = Collections.EMPTY_LIST;

    @OSGiService
    private ResourceResolverFactory resolverFactory;

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private WorkflowService workflowService;

    @Override
    public List<String> referencedDitamapList() {
        return referencedDitamapsList;
    }

    /**
     * Init method for the model
     */
    @PostConstruct
    protected void init() {
        ResourceResolver resolver = MadisonUtil
                .getResourceResolver(resolverFactory, MadisonConstants.MADISON_GENERIC_SUB_SERVICE);

        Session session = resolver.adaptTo(Session.class);
        String workId = request.getRequestParameter(WorkflowConstants.WORKFLOW_ID).getString();
        WorkflowSession wfSession = workflowService.getWorkflowSession(session);
        try {
            Workflow workflow = wfSession.getWorkflow(workId);
            if (null != workflow) {
                MetaDataMap workflowData = workflow.getWorkflowData().getMetaDataMap();
                String refDitaMaps = workflowData.get(DITAConstants.REVIEW_DITAMAPS, String.class);
                if (null != refDitaMaps && !refDitaMaps.isEmpty()) {
                    String[] refDitaMapsArray = refDitaMaps.split("\\|");
                    referencedDitamapsList = Arrays.asList(refDitaMapsArray);
                }
            }
        } catch (WorkflowException e) {
            LOG.error(e.getMessage());
        } finally {
            if (null != resolver && resolver.isLive()) {
                resolver.close();
            }
        }
    }
}
