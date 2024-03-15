package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
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

import com.adobe.fmdita.api.baselines.BaselineUtils;
import com.day.cq.commons.Externalizer;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.BulkDitaUtil;

/**
 * Servlet for triggering bulk baselining under given DAM folder with publishing point status as yes. 
 * curl -u <user>:<password> <host>/bin/pwc-madison/bulk-baselining?dam-path=<valid-dam-path>\&bulk-type=<revision|baseline>\&label<optional-lable-for-revision-baseline>
 * 
 * @author vijendrasetty
 *
 */
@Component(service = Servlet.class,
           property = { Constants.SERVICE_DESCRIPTION + "=Bulk Baselining Servlet",
                   "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                   "sling.servlet.paths=" + "/bin/pwc-madison/bulk-baselining" },
           configurationPolicy = ConfigurationPolicy.REQUIRE)
public class BulkBaselineServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final String APPROVED_LABEL = "Approved";
    private static final String BASELINE_NAME = "Publish Approved Topics ";
    private static final String OUTPUT_PRESET_BASELINE_NAME = "fmdita-baselineName";
    private static final String OUTPUT_PRESET_USE_BASELINE = "fmdita-useBaseline";
    
    @Reference
    private Externalizer externalizer;
    
    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    transient ResourceResolverFactory resourceResolverFactory;

    private final Logger LOG = LoggerFactory.getLogger(BulkBaselineServlet.class);

    @Override
    protected void doGet(@Nonnull final SlingHttpServletRequest request,
            @Nonnull final SlingHttpServletResponse response) throws ServletException, IOException {
        final String damPath = request.getParameter("dam-path");
        final String bulkType = request.getParameter("bulk-type");
        final String label = request.getParameter("label");
        //final String destination = request.getParameter("destination");
        final PrintWriter writer = response.getWriter();
        LOG.info(" Fetching publishing point DITAMAP's from the following path ::  " + damPath);

        if (StringUtils.isBlank(damPath) || StringUtils.isBlank(bulkType) || (!"baseline".equals(bulkType) && !"revision".equals(bulkType))) {
            writer.println(" dam-path or bulk-type (possible values 'baseline' or 'revision') not provided. Please check the request parameters");
            writer.close();
            return;
        }

        final ResourceResolver resourceResolver = request.getResourceResolver();
        if (null == resourceResolver) {
            writer.println(" Unable to get fmdita-serviceuser resolver to process data. Please check the permissions");
            writer.close();
            return;
        }

        try {
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
	        writer.flush();
	
	        final Date currentDate = currentDate();
	        final List<String> publishingPoints = fetchPublishingPoints(damPath, resourceResolver);
	        writer.println("Number of DITAMAP's found for bulk baselining -> " + publishingPoints.size());
	    	writer.flush();
	        // Just log all the ditamaps (publishing points)
	        for (String publishingPoint: publishingPoints) {
	        	writer.println(" Publishing point DITAMAP -> " + publishingPoint);
	        	writer.flush();
	        }
	        // Create a Baseline or revision based on the request parameter
			if ("baseline".equalsIgnoreCase(bulkType)) {
                createBaseLine(writer, resourceResolver, publishingPoints, BASELINE_NAME + currentDate, currentDate,
                        label);
			} else if ("revision".equalsIgnoreCase(bulkType)) {
				createRevision(writer, resourceResolver, publishingPoints, label, currentDate);
			}
	        
        
        } finally {
            if (resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }
        writer.close();

    }
    
    /**
     * @param writer
     * @param resourceResolver
     * @param publishingPoints
     * @param baselineLabel
     * @param versionDate
     */
    private void createRevision(final PrintWriter writer, final ResourceResolver resourceResolver, 
    		final List<String> publishingPoints, final String baselineLabel, final Date versionDate) {
    	
    	final Session session = resourceResolver.adaptTo(Session.class);
    	if(null == publishingPoints || publishingPoints.size() < 1) {
    		LOG.error(" No DITAMAP's found for bulk baselining");
    		writer.println(" No DITAMAP's found for bulk baselining");
    		return;
    	}
    	
    	for (String ditaMap : publishingPoints ) {
    		createBaseVersionForAllTopics(writer, session, ditaMap, resourceResolver, baselineLabel);
    		writer.println(" Created revision for all the topics in ditamap -> " + ditaMap);
			writer.flush();
    	}
    }
    
    /**
     * @param writer
     * @param resourceResolver
     * @param publishingPoints
     * @param baselineTitle
     * @param versionDate
     */
    private void createBaseLine(final PrintWriter writer, final ResourceResolver resourceResolver,
            final List<String> publishingPoints, final String baselineTitle, final Date versionDate,
            final String label) {

        final Session session = resourceResolver.adaptTo(Session.class);
        if (null == publishingPoints || publishingPoints.size() < 1) {
            LOG.error(" No DITAMAP's found for bulk baselining");
            writer.println(" No DITAMAP's found for bulk baselining");
            return;
        }
    	// Get the label from parameter else set Approved.
    	String baselineLabel = APPROVED_LABEL;
    	if(StringUtils.isNotBlank(label)) {
    		baselineLabel = label;
    	}
    	for (String ditaMap : publishingPoints ) {
    		try {
    			writer.println(" Creating baseline for ditamap -> " + ditaMap);
    			writer.flush();
    			String baseLine = BaselineUtils.createBaseline(session, ditaMap, baselineTitle, versionDate);
                // update the preset mean time
				final Resource resource = resourceResolver.getResource(ditaMap);
				boolean status = updateDitaMapBaselineName(resource, resourceResolver, baseLine);
				writer.println(" Baseline name " + baseLine + " set for outputpresets for ditamap status :: " + status);
				writer.flush();
				// apply the baseline label to ditamap.
				BaselineUtils.applyLabel(session, ditaMap, baseLine, baselineLabel);
				writer.println(" Baseline " + baseLine + " created for ditamap -> " + ditaMap);
				writer.flush();
			} catch (Exception e) {
				writer.println(" Error creating baseline for ditamap -> " + ditaMap + " :: " + e);
			}
    	}
    }
    
	/**
	 * @param ditaMap
	 * @param resourceResolver
	 * @param baselineName
	 * @return true if updated, false if not
	 */
    private boolean updateDitaMapBaselineName(final Resource ditaMap, final ResourceResolver resourceResolver, final String baselineName) {
    	
    	// Output Preset for AEM Site
    	final Resource aemSiteOutputPreset = ditaMap.getChild("jcr:content/metadata/namedoutputs/aemsite");
    	boolean updated = Boolean.FALSE;

        if (null == aemSiteOutputPreset) {
            return updated;
        }
        try {
        	// Update the baseline name to the baseline created.
            final ModifiableValueMap presetProperties = aemSiteOutputPreset.adaptTo(ModifiableValueMap.class);
            presetProperties.put(OUTPUT_PRESET_USE_BASELINE, Boolean.TRUE);
            presetProperties.put(OUTPUT_PRESET_BASELINE_NAME, baselineName);
            if (resourceResolver.hasChanges()) {
                resourceResolver.commit();
            }
            updated = Boolean.TRUE;
        } catch (final PersistenceException e) {
            LOG.error("Unable to update the output preset target path for ditamap in bulk baselining {} ", ditaMap.getPath());
            return false;
        } 
		return updated;
    }
    
    /**
     * Create a revision and label as approved for all the topics under publishing point ditamaps.
     * 
     * @param writer
     * @param session
     * @param payload
     * @param resolver
     */
    private void createBaseVersionForAllTopics(final PrintWriter writer, final Session session, 
			final String payload, final ResourceResolver resolver, final String baselineLabel) {
		try {
			String label = APPROVED_LABEL;
			// Fetch all the topics from given ditamap.
			final List<String> topics = BulkDitaUtil.fetchAllTopicsFromDitamap(payload, session);
			topics.add(payload);
			// Create a revision and set label as Published
			if (StringUtils.isNoneBlank(baselineLabel)) {
				label = baselineLabel;
			}
			writer.println(" Creating revision as " + label + " for all the topics in ditamap -> " + payload);
			writer.flush();
			BulkDitaUtil.createBulkRevision(label, "Baseline Increment", topics, session, resolver);
			LOG.info(Thread.currentThread().getName() + " Created revision for all the topics in the ditamap -> "
					+ payload);
		} catch (IllegalStateException | RepositoryException e) {
			LOG.error(" Error creating the revision for topics in the ditamap -> " + payload + " " + e);
		}

	}

    /**
	 * Method to get current Date
	 * 
	 * @return
	 */
	private Date currentDate() {
		final DateFormat df = new SimpleDateFormat("d-MM-yyyy H:mm");
		final Date currentDate = new Date(System.currentTimeMillis());
		try {
			return df.parse(df.format(currentDate));
		} catch (ParseException e) {
			LOG.error("error in parsing the date", e);
		}
		return currentDate;
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
