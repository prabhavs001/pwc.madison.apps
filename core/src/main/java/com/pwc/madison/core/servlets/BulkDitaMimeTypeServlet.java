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

/**
 * Servlet for fixing the DITA mimeType under given DAM folder. 
 * curl -u <user>:<password> <host>/bin/pwc-madison/bulk-dita-mimetype?dam-path=<valid-dam-path>
 * 
 * @author vijendrasetty
 *
 */
@Component(service = Servlet.class,
           property = { Constants.SERVICE_DESCRIPTION + "= Bulk Dita mimeType Servlet",
                   "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                   "sling.servlet.paths=" + "/bin/pwc-madison/bulk-dita-mimetype" },
           configurationPolicy = ConfigurationPolicy.REQUIRE)
public class BulkDitaMimeTypeServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    
    @Reference
    private Externalizer externalizer;
    
    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    transient ResourceResolverFactory resourceResolverFactory;

    private final Logger LOG = LoggerFactory.getLogger(BulkDitaMimeTypeServlet.class);

    @Override
    protected void doGet(@Nonnull final SlingHttpServletRequest request,
            @Nonnull final SlingHttpServletResponse response) throws ServletException, IOException {
    	
        final String damPath = request.getParameter("dam-path");
        final PrintWriter writer = response.getWriter();
        LOG.info(" Fetching all the topicsfrom the following path ::  " + damPath);

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
        writer.println(" Fetching all DITA's from the following path ::  " + damPath);
        
		Session session = resourceResolver.adaptTo(Session.class);
		// Fetch all dita's not having the mimeType
		List<String> topics = fetchTopics(damPath, resourceResolver);
		// Update the mimeType to application/xml for all the topics.
		processAllTopics(writer, topics, session, request);

		writer.println(" Finished executing....... ");
		writer.close();

    }
    
    
    /**
     * Method to update all the dita's who's mimeType missing to set as application/xml.
     * 
     * @param writer
     * @param topics
     * @param resourceResolver
     * @param request
     * @return
     */
    private void processAllTopics(final PrintWriter writer, final List<String> topics, 
    		final Session session, final SlingHttpServletRequest request) {
    	
    	if(null == topics || topics.size() < 1) {
    		LOG.error(" No DITA's found...");
    		writer.println(" No DITA's found..");
    		return;
    	}
    	writer.println(" Number of DITA's found -> " + topics.size());
    	
    	for(String topic : topics) {
    		try {
    			 if (null != session.getNode(topic)) {
    	                final javax.jcr.Node node = session.getNode(topic)
    	                        .getNode(org.apache.jackrabbit.JcrConstants.JCR_CONTENT + "/renditions/original/jcr:content");
    	                if (null != node) {
    	                    node.setProperty("jcr:mimeType", "application/xml");
    	                    session.save();
    	                }
    	            }
			} catch (Exception e) {
				LOG.error(" Error setting the mimeType for the dita ", topic);
	    		writer.println(" Error setting the mimeType for the dita :: "+topic+" :: "+e);
			} 
    	}
    }
    
   
    /**
     * Fetches all the dita's not having the mimeType property.
     *
     * @param damPath
     * @return
     */
    private List<String> fetchTopics(final String damPath, final ResourceResolver resourceResolver) {
    	
        final Resource ditamapRes = resourceResolver.getResource(damPath);
        if (null == ditamapRes) {
        	return null;
        }
        final Map<String, Object> map = createDitaQuery(damPath);
        final Query query = queryBuilder
                .createQuery(PredicateGroup.create(map), resourceResolver.adaptTo(Session.class));
        final SearchResult ditaPaths = query.getResult();
        return populateDitamap(ditaPaths, resourceResolver);
    }

    /**
     * @param ditaPaths
     * @param resourceResolver
     * @return
     */
    private List<String> populateDitamap(final SearchResult ditaPaths, final ResourceResolver resourceResolver) {
    	
    	final List<String> ditas = new ArrayList<>();
    	if(null == ditaPaths) {
    		return ditas;
    	}
    	
        final Iterator<Resource> pathResources = ditaPaths.getResources();
        while (pathResources.hasNext()) {
        	 final Resource resource = pathResources.next();
        	 // Check for only dita's
        	 if(StringUtils.isNoneBlank(resource.getPath()) 
        			 && resource.getPath().endsWith(DITAConstants.DITA_EXTENSION)) {
        		 ditas.add(resource.getPath());
        	 }
        }
        return ditas;
    }
    
    /**
     * Creates query to fetch dita's from dam path which are not having any mimeType in it.
     *
     * @param damPath
     * @return
     */
    private  Map<String, Object> createDitaQuery(final String damPath) {
        final Map<String, Object> predicateMap = new HashMap<>();
        predicateMap.put("path", damPath);
        predicateMap.put("type", "dam:Asset");
        predicateMap.put("1_property", "jcr:content/renditions/original/jcr:content/jcr:mimeType");
        predicateMap.put("1_property.operation", "exists");
        predicateMap.put("1_property.value", "false");
        predicateMap.put("2_property", "jcr:content/renditions/original/jcr:content/jcr:primaryType");
        predicateMap.put("2_property.value", "nt:resource");
        predicateMap.put("p.limit", "-1");
        return predicateMap;
    }
}
