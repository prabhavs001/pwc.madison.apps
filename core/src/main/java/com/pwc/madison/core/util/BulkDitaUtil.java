package com.pwc.madison.core.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;

/**
 * Bulk utility class for performing the bulk update of the topics for a given DITAMAP.
 * 
 * @author vhs
 *
 */
public class BulkDitaUtil {

	private static final String JCR_PATH = "/jcr:content";
	private static final Logger LOG = LoggerFactory.getLogger(BulkDitaUtil.class);
	
	
	/**
	 * Method to fetch all the topics from the given DITAMAP recursively.
	 * 
	 * @param ditaMap
	 * @param resourceResolver
	 * @return
	 * @throws ValueFormatException
	 * @throws IllegalStateException
	 * @throws RepositoryException
	 */
	public static final List<String> fetchAllTopicsFromDitamap(final String ditaMap, 
			final Session session) throws ValueFormatException, IllegalStateException, RepositoryException {
		
		final List<String> topics = new ArrayList<String>();
		// Fetch all the ditamap topic references. 
		final Value[] fmditaTopicrefs = getTopicRefs(ditaMap, session);
		if (null == fmditaTopicrefs || fmditaTopicrefs.length == 0) {
			LOG.error(
					"DITAMAP does not have any topics or sub maps to create a version. Cannot proceed with dita status update.");
			return topics;
		}
		// Fetch all topics from ditamap recursively and populate.
        fetchAllTopics(fmditaTopicrefs, topics, session); 
		return topics;
	}
	
	/**
	 * @param topics
	 * @param session
	 * @param resourceResolver
	 * @return
	 */
	public static boolean createBulkRevision(final String label, final String comment, 
			final List<String> topics, final Session session, final ResourceResolver resourceResolver) {
		
		if(null == topics || topics.size() == 0 || null == session || null == resourceResolver) {
			LOG.error( "Failed to create new version for DITA, parameters are null" );
			return false;
		}
		// Else create revision and save session.
		for(String topic: topics) {
        	final Resource res = resourceResolver.getResource(topic);
			try {
				DITAUtils.createRevision(topic, label, comment, res, session);
				session.save();
			} catch (Exception ex) {
				LOG.error( "Failed to create new version for DITA : " + topic, ex);
			}
        }
		return true;
	}
	
    /**
     * Set last published date for all the topics.
     *
     * @param paths
     * @param lastPublished
     * @param adminSession
     * @throws RepositoryException
     */
    public static void setBulkLastPublishedDate(final List<String> topics, final String lastPublished,
            final Session adminSession) {
    	
    	String publishDate = lastPublished;
        if (null == topics || topics.size() == 0 || null == adminSession) {
            LOG.error("Error setting the last published :: parameters are null");
            return;
        }
        
        if(StringUtils.isBlank(lastPublished)) {
        	// Get current date.
        	publishDate = currentDate();
        }
        String dita = StringUtils.EMPTY;
        for (final String topic : topics) {
            try {
                if (null != adminSession.getNode(topic)) {
                    // Capturing the DITA path to local variable in order to log, in case of error.
                    dita = topic;
                    final javax.jcr.Node node = adminSession.getNode(topic)
                            .getNode(org.apache.jackrabbit.JcrConstants.JCR_CONTENT + "/metadata");
                    if (null != node) {
                        node.setProperty(DITAConstants.PN_METADATA_LAST_PUBLISHED, publishDate);
                        adminSession.save();
                    }
                }
            } catch (final RepositoryException e) {
                LOG.error("Failed to save the last published date for the DITA :" + dita);
            }
        }
    }	
	
	/**
	 * Method to get current Date
	 * 
	 * @return
	 */
	public static String currentDate() {
		final DateFormat df = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
		return df.format(new Date(System.currentTimeMillis()));
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
    private static void fetchAllTopics(final Value[] topicRefs, final List<String> topics, 
    		final Session session) throws ValueFormatException, IllegalStateException, RepositoryException {
    	
    	if(topicRefs == null) {
    		return;
    	}
    	for (Value topic : topicRefs) {
    		String dita = topic.getString().replaceFirst(MadisonConstants.COMMA_SEPARATOR, StringUtils.EMPTY);
    		if(StringUtils.isNotBlank(dita)) {
            	topics.add(dita);
            	if(dita.endsWith(DITAConstants.DITAMAP_EXT)) {
            		Value[] topicSubRefs = getTopicRefs(dita, session);
            		fetchAllTopics(topicSubRefs, topics, session);
            	}
    		}
        }
    }
	
    /**
     * Method to get the list of topics from the given pay-load (DITAMAP).
     * 
     * @param payload
     * @param session
     * @return
     */
    public static final Value[] getTopicRefs(final String payload, final Session session) {
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
			LOG.error("Error getting the node for the ditamap {}", e);
		} catch (RepositoryException e) {
			LOG.error("Error getting the node for the ditamap {}", e);
		}
    	return fmditaTopicrefs;
    }
    
   
    /**
     * Method to set the bulk status of the topics.
     * 
     * @param topics
     * @param docstate
     * @param adminSession
     * @param save
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public static void setBulkDocStatus(final List<String> topics, final String docstate, 
    		final Session adminSession) throws PathNotFoundException, RepositoryException {
    	
    	if (null == topics || topics.size() == 0 || null == adminSession) {
            LOG.error("Error setting the bulk doc status :: parameters are null");
            return;
        }
    	
        for (final String topic : topics) {
            if (null != adminSession.getNode(topic)) {
                final javax.jcr.Node node = adminSession.getNode(topic)
                        .getNode(org.apache.jackrabbit.JcrConstants.JCR_CONTENT + "/metadata");
                if (null != node) {
                    node.setProperty(DITAConstants.PN_METADATA_DOCSTATE, docstate);
                    adminSession.save();
                }
            }
        }
    }
}
