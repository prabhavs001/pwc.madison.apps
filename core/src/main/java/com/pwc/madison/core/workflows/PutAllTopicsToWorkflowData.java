package com.pwc.madison.core.workflows;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Workflow process step to get all the topics from the ditamap at all levels and add it to workflow data for further processing.
 * 
 * @author vijendrasetty
 *
 */
@Component(service = WorkflowProcess.class, property = { "process.label= Madision - Retrieve all topics and add to workflow data" })
public class PutAllTopicsToWorkflowData implements WorkflowProcess {

	
	private static final String JCR_PATH = "/jcr:content";
    private static final String GENERATED_PATH = "generatedPath";
    private static final String IS_SUCCESS = "isSuccess";
    private static final String HTML_EXTENSION = ".html";

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceResolverFactory resolverFactory;
    
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;


    @Override
    public void execute(final WorkItem item, final WorkflowSession wfsession, final MetaDataMap args)
            throws WorkflowException {

        ResourceResolver resolver = null;
        Session session = null;
        try {
        	// Check if generation success/failed, else return.
        	final MetaDataMap meta = item.getWorkflowData().getMetaDataMap();
        	String payload = item.getWorkflowData().getPayload().toString();
        	if(isSiteGenerationFailed(meta, payload)) {
        		return;
        	}

            resolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
            if (null == resolver) {
                log.error("Workflow ResourceResolver null. Cannot proceed with dita status update.");
                return;
            }
            // Check if workflow meta data already has topics, then return.
            String orgTopics = meta.get(DITAConstants.ORIGINAL_TOPICS, String.class);
            if(null != orgTopics && !orgTopics.isEmpty()) {
                return;
            }
            session = resolver.adaptTo(Session.class);
            Value[] fmditaTopicrefs = getTopicRefs(payload, session);
            if(null == fmditaTopicrefs || fmditaTopicrefs.length == 0) {
            	log.error("DITAMAP does not have any topics or sub maps to publish. Cannot proceed with dita status update.");
                return;
            }
            
            putAllTopicsToWorkflow(fmditaTopicrefs, session, item, wfsession);
            
            if (resolver.hasChanges()) {
                resolver.commit();
            }
        } catch (final Exception e) {
            log.error("Failed to update topics status ", e);
        } finally {
            if (resolver != null) {
                resolver.close();
            }
        }
    }
    
    private boolean isSiteGenerationFailed(final MetaDataMap meta, final String payload) {
    	
    	boolean generationFailed = Boolean.FALSE;
        if (null == meta || StringUtils.isEmpty(payload)) {
            log.error("Workflow metadata/payload is null. Cannot proceed with updating the dita status.");
            generationFailed = Boolean.TRUE;
        }
        final String outputType = meta.get(IS_SUCCESS, String.class);
        final String outputPath = meta.get(GENERATED_PATH, String.class);
        if (StringUtils.isBlank(outputType) || StringUtils.isBlank(outputPath)) {
            log.error("OutputType or outputPath is empty or null");
            generationFailed = Boolean.TRUE;
        }
        if (!outputPath.contains(HTML_EXTENSION)) {
            log.error("Output path is not html {} ", outputPath);
            generationFailed = Boolean.TRUE;
        }
        
        return generationFailed;
    }
    /**
     * 
     * @param fmditaTopicrefs
     * @param session
     * @param item
     * @param wfsession
     * @throws ValueFormatException
     * @throws IllegalStateException
     * @throws RepositoryException
     */
    private void putAllTopicsToWorkflow(final Value[] fmditaTopicrefs, final Session session, final WorkItem item, 
    		final WorkflowSession wfsession) throws ValueFormatException, IllegalStateException, RepositoryException {
    	
    	List<String> topics = new ArrayList<String>();
        getAllTopics(fmditaTopicrefs, topics, session); 
        StringBuilder orgTopics = new StringBuilder();
        for(String topic: topics) {
        	log.debug(" Each topic in the DITAMAP is :: " + topic);
        	orgTopics.append(topic).append(DITAConstants.ITEM_SEPARATOR);
        }
        // Finally set the topics in the workflow session.
        updateWorkflowData(orgTopics.toString(), item.getWorkflowData(), item, wfsession);
    }
    
    /**
     * Method to get the list of topics from the given pay-load (DITAMAP).
     * 
     * @param payload
     * @param session
     * @return
     */
    private final Value[] getTopicRefs(final String payload, final Session session) {
    	Node rootDitamap;
    	Value[] fmditaTopicrefs = null;
		try {
			rootDitamap = session.getNode(payload + JCR_PATH);
	        if (rootDitamap.hasProperty("fmditaTopicrefs")) {
	        	fmditaTopicrefs = rootDitamap.getProperty("fmditaTopicrefs").getValues();
	        }
	        if(null != fmditaTopicrefs & fmditaTopicrefs.length > 0) {
	        	return fmditaTopicrefs;
	        }
		} catch (PathNotFoundException e) {
			log.error("Error getting the node for the ditamap {}", e);
		} catch (RepositoryException e) {
			log.error("Error getting the node for the ditamap {}", e);
		}
    	return fmditaTopicrefs;
    }
    
    /**
     * Method to get all the topics from the root ditamap and sub ditamaps.
     * 
     * @param topicRefs
     * @param topics
     * @param session
     * @return
     * @throws RepositoryException 
     * @throws IllegalStateException 
     * @throws ValueFormatException 
     */
    private List<String> getAllTopics(final Value[] topicRefs, final List<String> topics, 
    		final Session session) throws ValueFormatException, IllegalStateException, RepositoryException {
    	
    	if(topicRefs == null) {
    		return topics;
    	}
    	for (Value topic : topicRefs) {
    		String dita = topic.getString().replaceFirst(",", "");
    		if(StringUtils.isNotBlank(dita)) {
    			if(dita.endsWith(DITAConstants.DITA_EXTENSION)) {
            		topics.add(dita);
            	} else if(dita.endsWith(DITAConstants.DITAMAP_EXT)) {
            		Value[] topicSubRefs = getTopicRefs(dita, session);
            		getAllTopics(topicSubRefs, topics, session);
            	}
    		}
        }
    	return topics;
    }

    /**
     * Method to update the workflow with the dita topics and set for the next process to pickup and update the doc status.
     * 
     * @param wfData
     * @param workItem
     * @param wfsession
     */
    private void updateWorkflowData(final String orgTopics, final WorkflowData wfData, 
    		final WorkItem workItem, final WorkflowSession wfsession) {
    	String map = workItem.getWorkflowData().getPayload().toString();
    	// Prepare a list of dita to get the it populated to the WF session
    	wfData.getMetaDataMap().put(DITAConstants.ORIGINAL_TOPICS, orgTopics);
    	wfData.getMetaDataMap().put(DITAConstants.IS_DITAMAP, Boolean.TRUE);
    	wfData.getMetaDataMap().put(DITAConstants.DITAMAP, map);
    	wfsession.updateWorkflowData(workItem.getWorkflow(), wfData);
    }
    
}
