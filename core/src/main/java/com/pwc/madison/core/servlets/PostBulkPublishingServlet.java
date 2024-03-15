package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.Externalizer;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.BulkDitaUtil;
import com.pwc.madison.core.util.DITAUtils;

/**
 * Servlet for fixing the DITA draft version post bulk publishing under given DAM folder with publishing point status as yes. 
 * curl -u <user>:<password> <host>/bin/pwc-madison/post-bulk-publishing?dam-path=<valid-dam-path>
 * 
 * @author vijendrasetty
 *
 */
@Component(service = Servlet.class,
           property = { Constants.SERVICE_DESCRIPTION + "=Post Bulk Publishing Servlet",
                   "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                   "sling.servlet.paths=" + "/bin/pwc-madison/post-bulk-publishing" },
           configurationPolicy = ConfigurationPolicy.REQUIRE)
public class PostBulkPublishingServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final String DRAFT_STATUS = "Draft";
    
    @Reference
    private Externalizer externalizer;
    
    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    transient ResourceResolverFactory resourceResolverFactory;

    private final Logger LOG = LoggerFactory.getLogger(PostBulkPublishingServlet.class);

    @Override
    protected void doGet(@Nonnull final SlingHttpServletRequest request,
            @Nonnull final SlingHttpServletResponse response) throws ServletException, IOException {
    	
        final String damPath = request.getParameter("dam-path");
        final PrintWriter writer = response.getWriter();
        LOG.info(" Fetching publishing point DITAMAP's from the following path ::  " + damPath);

        if (StringUtils.isBlank(damPath)) {
            writer.println(" DAM path not provided. Please check the request");
            writer.close();
            return;
        }

        final ResourceResolver resourceResolver = request.getResourceResolver();
        if (null == resourceResolver) {
            writer.println(" Unable to get fmdita-serviceuser resolver to process data. Please check the permissions");
            writer.close();
            return;
        }

        final Resource sourceResource = resourceResolver.getResource(damPath);

        if (null == sourceResource) {
            writer.println(" DAM path provided is invalid. Please check/validate and try again");
            writer.close();
            return;
        }
        String resourceType = (String) sourceResource.getValueMap().getOrDefault(JcrConstants.JCR_PRIMARYTYPE,
                org.apache.commons.lang3.StringUtils.EMPTY);
        
		if (!resourceType.contains(MadisonConstants.STR_FOLDER)) {
			writer.println(" DAM path provided is not a folder. Please provide proper path");
            writer.close();
            return;
		}
        writer.println(" Fetching publishing point DITAMAP's from the following path ::  " + damPath);
        
		Session session = resourceResolver.adaptTo(Session.class);
		// Fetch all publishing point ditamaps
		List<String> publishingPoints = fetchPublishingPoints(damPath, resourceResolver);
		// Fetch all draft dita's from publishing point maps and process.
		processAllDraftTopics(writer, publishingPoints, session, request);

		writer.println(" Finished executing....... ");
		writer.close();

    }
    
    
    /**
     * Method to fetch all the draft topics from the publishing point ditamaps.
     * 
     * @param writer
     * @param publishingPoints
     * @param resourceResolver
     * @param request
     * @return
     */
    private void processAllDraftTopics(final PrintWriter writer, final List<String> publishingPoints, 
    		final Session session, final SlingHttpServletRequest request) {
    	
    	if(null == publishingPoints || publishingPoints.size() < 1) {
    		LOG.error(" No DITAMAP's found from publish point");
    		writer.println(" No DITAMAP's found from publishing point");
    		return;
    	}
    	writer.println(" Number of DITAMAP's found from publishing point -> " + publishingPoints.size());
    	
    	// Fetch all the dita topics from the publishing point ditamaps and process it.
    	List<String> draftTopicsList = new ArrayList<String>(100);
    	for(String ditaMap : publishingPoints) {
    		try {
    			// Invoke the bulk document state setting method to update the draft status to Published state.
    			draftTopicsList = getDraftTopics(BulkDitaUtil.fetchAllTopicsFromDitamap(ditaMap, session), session);
    			if(draftTopicsList != null && draftTopicsList.size() > 0) {
    				writer.println(" Number fo topics found with Draft status in publishing point "+ditaMap+" is "+draftTopicsList.size());
    				BulkDitaUtil.setBulkDocStatus(draftTopicsList, "Published", session);
    			}
			} catch (Exception e) {
				LOG.error(" Error fetching topics from publishing point or to set the document status ", e);
	    		writer.println(" Error fetching topics from publishing point or to set the document status "+e);
			} 
    	}
    }
    
   
    /**
     * Get topics list who's status of the topics as draft.
     * 
     * @param publishPointTopics
     * @param session
     * @return
     */
    private List<String> getDraftTopics(final List<String> publishPointTopics, final Session session) {
    	List<String> draftTopics = new ArrayList<String>(10);
    	for(String topic : publishPointTopics) {
    		String docState = DITAUtils.getDocState(topic, session);
    		if(docState != null && DRAFT_STATUS.equalsIgnoreCase(docState)) {
    			draftTopics.add(topic);
    			LOG.debug(" Topic {} status is Draft", topic);
    		}
    	}
    	return draftTopics;
    }
    
    /**
     * Fetches all the ditamaps having publishing points "yes".
     *
     * @param damPath
     * @return
     */
    private List<String> fetchPublishingPoints(final String damPath, final ResourceResolver resourceResolver) {
    	
        final Resource ditamapRes = resourceResolver.getResource(damPath);
        if (null == ditamapRes) {
        	return null;
        }
        final Map<String, Object> map = createDitaMapQuery(damPath);
        final Query query = queryBuilder
                .createQuery(PredicateGroup.create(map), resourceResolver.adaptTo(Session.class));
        final SearchResult ditaMapPaths = query.getResult();
        return populateDitamaps(ditaMapPaths, resourceResolver);
    }

    /**
     * @param ditaMapPaths
     * @param resourceResolver
     * @return
     */
    private List<String> populateDitamaps(final SearchResult ditaMapPaths, final ResourceResolver resourceResolver) {
    	
    	final List<String> ditaMaps = new ArrayList<>();
    	if(null == ditaMapPaths) {
    		return ditaMaps;
    	}
    	
        final Iterator<Resource> pathResources = ditaMapPaths.getResources();
        while (pathResources.hasNext()) {
        	 final Resource resource = pathResources.next();
        	 // Check for only ditamaps
        	 if(StringUtils.isNoneBlank(resource.getPath()) 
        			 && resource.getPath().endsWith(DITAConstants.DITAMAP_EXT)) {
        		 ditaMaps.add(resource.getPath());
        	 }
        }
        return ditaMaps;
    }
    
    /**
     * Creates query to fetch DitaMap path from dam path based on publication point.
     *
     * @param damPath
     * @return
     */
    private  Map<String, Object> createDitaMapQuery(final String damPath) {
        final Map<String, Object> predicateMap = new HashMap<>();
        predicateMap.put("path", damPath);
        predicateMap.put("type", "dam:Asset");
        predicateMap.put("property", "@jcr:content/" + "metadata/pwc:isPublishingPoint");
        predicateMap.put("property.value", "yes");
        predicateMap.put("p.limit", "-1");
        return predicateMap;
    }
    
}
