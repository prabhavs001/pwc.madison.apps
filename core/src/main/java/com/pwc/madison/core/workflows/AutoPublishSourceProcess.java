package com.pwc.madison.core.workflows;

import com.day.cq.commons.Externalizer;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonDomainsService;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.services.PublishDitamapService;
import com.pwc.madison.core.thread.HttpGetThread;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.PublishingUtil;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component(service = WorkflowProcess.class, property = { "process.label=Madison - Auto-Publish Source Process" })
public class AutoPublishSourceProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(AutoPublishProcess.class);

    @Reference
    private ResourceResolverFactory resolverFactory;
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;
    @Reference
    private PublishDitamapService publishDitamapService;
    @Reference
    private Externalizer externalizer;
    @Reference
    private MadisonDomainsService madisonDomainsService;
    /**
     * @param workItem
     * @param workflowSession
     * @param metaDataMap
     * @throws WorkflowException
     */
    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        LOG.debug("Starting with Auto-Publish Source Step");

        ResourceResolver resolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
        Session session = resolver.adaptTo(Session.class);
        MetaDataMap meta = workItem.getWorkflowData().getMetaDataMap();
        if (meta == null || null == session) {
            LOG.error("Workflow metadata/session is null. Cannot proceed with setting revision.");
            return;
        }
        String orgTopics = meta.get(DITAConstants.ORIGINAL_TOPICS, String.class);
        String reviewMaps = meta.get(DITAConstants.REVIEW_DITAMAPS, String.class);
        if(Objects.nonNull(reviewMaps) && !reviewMaps.isEmpty()){
            LOG.debug("There is/are map(s) for review : "+ reviewMaps + ". Hence starting the AutoPublish Process for Source Publishing Point.");
            final ExecutorService executor = Executors.newFixedThreadPool(1);
            final String publishingURL = PublishingUtil.getAPIUrl(madisonDomainsService.getDefaultDomain(), MadisonConstants.OUTPUT_TYPE_AEMSITE, externalizer, MadisonConstants.GENERATE_OUTPUT);
            String sourcePublishingPoint = workItem.getWorkflowData().getPayload().toString();
            if(publishDitamapService.isPublishingPoint(resolver, resolver.getResource(sourcePublishingPoint))) {
                RequestConfig requestConfig = publishDitamapService.setConnectionData();
                final List<Header> header = publishDitamapService.getHeaders();
                final Runnable worker = new HttpGetThread(resolver, publishingURL, sourcePublishingPoint, requestConfig, header,
                        MadisonConstants.OUTPUT_TYPE_AEMSITE, "auto-publish-user");
                executor.execute(worker);
                executor.shutdown();
                while (!executor.isTerminated()) {
                    try {
                        TimeUnit.SECONDS.sleep(4);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }else {
            LOG.debug("There are no review maps, hence Author has to follow the Regenerate Topic process.");
        }
    }
}
