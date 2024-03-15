package com.pwc.madison.core.workflows;

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;


//This is a component so it can provide or consume services
@Component(service = WorkflowProcess.class,
    property = {"process.label= Madision - Set Document State"})
public class SetDocumentStateProcess implements WorkflowProcess {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    @Reference
    private ResourceResolverFactory resolverFactory;
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    @Override
    public void execute(WorkItem item, WorkflowSession wfsession, MetaDataMap args) throws WorkflowException {

        if (args.containsKey("PROCESS_ARGS")) {

            String docState = args.get("PROCESS_ARGS", "string").toString();

            ResourceResolver resolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());

            Session session = resolver.adaptTo(Session.class);

            MetaDataMap meta = item.getWorkflowData().getMetaDataMap();
            if (meta == null) {
                log.error("Workflow metadata null. Cannot proceed with review.");
                return;
            }
            // Setting doc state
            try {
                boolean isFullSiteGeneration = false;
                String strChangesDitamaps = meta.get(DITAConstants.REVIEW_DITAMAPS,String.class);
                if(null!=strChangesDitamaps && !strChangesDitamaps.isEmpty()){
                    String[] ditamapArray = strChangesDitamaps.split("|");
                    if(ditamapArray.length>0){
                        isFullSiteGeneration = true;
                    }
                }
                String topics = meta.get(DITAConstants.SELECTED_TOPICS, String.class);;
                if(isFullSiteGeneration){
                    topics = meta.get(DITAConstants.ORIGINAL_TOPICS, String.class);
                }

                String[] selectedTopicsList = topics.split("\\|");
                DITAUtils.setDocStates(selectedTopicsList, docState, session, true, true, null);
                Boolean isDitamap = meta.get(DITAConstants.IS_DITAMAP, Boolean.class);
                if (isDitamap != null && isDitamap) {
                    String ditamap = meta.get(DITAConstants.DITAMAP, String.class);
                    DITAUtils.setDocStates(new String[]{ditamap}, docState, session, true, true, null);
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
            } finally {
                session.logout();
            }
        }
    }
}
