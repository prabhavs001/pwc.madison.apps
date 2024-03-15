package com.pwc.madison.core.workflows;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.day.cq.workflow.exec.WorkflowData;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;


/**
 * Process to create auto version/revision for the DITA/DITAMAP and set the last published date for the same.
 * 
 * @author vhs
 *
 */
@Component(service = WorkflowProcess.class, property = {"process.label= Madision - Set Publish Status on Topics"})
public class SetDITARevisionProcess implements WorkflowProcess {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    @Reference
    private ResourceResolverFactory resolverFactory;
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    @Override
    public void execute(WorkItem item, WorkflowSession wfsession, MetaDataMap args) throws WorkflowException {

    	ResourceResolver resolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
        Session session = resolver.adaptTo(Session.class);
        MetaDataMap meta = item.getWorkflowData().getMetaDataMap();
        if (meta == null || null == session) {
            log.error("Workflow metadata/session is null. Cannot proceed with setting revision.");
            return;
        }
        String orgTopics = meta.get(DITAConstants.ORIGINAL_TOPICS, String.class);
        if(orgTopics.isEmpty()) {
        	log.error("Workflow topics are null. Cannot proceed with setting revision.");
            return;
        }
        // Setting revision creation and setting the last published date for the DITA/DITAMAP
        try {
            boolean isFullSiteGeneration = false;
            String strChangesDitamaps = meta.get(DITAConstants.REVIEW_DITAMAPS,String.class);
            if(null!=strChangesDitamaps && !strChangesDitamaps.isEmpty()){
                String[] ditamapArray = strChangesDitamaps.split("|");
                if(ditamapArray.length>0){
                    isFullSiteGeneration = true;
                }
            }
            String topics = meta.get(DITAConstants.SELECTED_TOPICS, String.class);
            if(isFullSiteGeneration){
                topics = meta.get(DITAConstants.ORIGINAL_TOPICS, String.class);
            }

            String currentDate = currentDate();
            String[] topicsArr = topics.split("\\|");
            DITAUtils.setLastPublishedDate(topicsArr, currentDate, session);
            WorkflowData wfData = item.getWorkflowData();
            setNewRevisionForTopic(topicsArr, resolver, session, wfData.getMetaDataMap().get(DITAConstants.DESCRIPTION, String.class));
            // If it's DITAMAP just ignore.           
        } finally {
        	if (resolver.isLive()) {
        		session.logout();
        		resolver.close();
            }
        }
    }
    
	/**
	 * @param orgTopicsList
	 * @param resolver
	 * @param session
	 */
	private void setNewRevisionForTopic(String[] orgTopicsList,	ResourceResolver resolver, Session session, String comment) {

		for (String path : orgTopicsList) {
			Resource res = resolver.getResource(path);
			try {
				DITAUtils.createRevision(path, "Published", comment, res, session);
				session.save();
			} catch (Exception ex) {
				log.error( "Failed to create new version for DITA : " + path, ex);
			}
		}
	}
    
	/**
	 * Method to get current Date
	 * 
	 * @return
	 */
	private String currentDate() {
		final DateFormat df = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
		return df.format(new Date(System.currentTimeMillis()));
	}
}
