package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.Externalizer;
import com.day.cq.search.QueryBuilder;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.SeeAlsoUtil;

@Component(
    service = Servlet.class,
    property = { Constants.SERVICE_DESCRIPTION + "=Set the used in references to each topic",
            "sling.servlet.methods=" + HttpConstants.METHOD_POST,
            "sling.servlet.paths=" + "/bin/pwc-madison/autoSetUsedInReferences" })
public class AutoSetUsedInReferences extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    
    @Reference
    private Externalizer externalizer;
    
    @Reference
    private QueryBuilder queryBuilder;
    
    @Reference
    private XSSAPI xssAPI;
    
    transient ValueMap valueMap;
    transient List<Resource> resourceList;

    @Override
    protected void doGet(@Nonnull final SlingHttpServletRequest request,
            @Nonnull final SlingHttpServletResponse response) throws ServletException, IOException {

        final ResourceResolver resourceResolver = request.getResourceResolver();
        resourceList = new ArrayList<>();
        final PrintWriter writer = response.getWriter();

        if (null == resourceResolver) {
            writer.write("Unable to get service resolver to process data in class AutoSetUsedInReferences. Please check the permissions\n");
            writer.close();
            return;
        }

        final String ditaMapPath = request.getParameter("item");
        LOGGER.debug("Dita Path : {}", xssAPI.encodeForHTML(ditaMapPath));
        if (StringUtils.isBlank(ditaMapPath)) {
            response.setStatus(500);
            writer.write(StringUtils.EMPTY);
            writer.close();
            return;
        }
        Set<String> topicsReferredInMapList = new HashSet<>();
        ValueMap contentValueMap = resourceResolver.getResource(ditaMapPath + "/jcr:content").adaptTo(ValueMap.class);
        if(null != contentValueMap) {
        	String[] topicRefs = contentValueMap.get("fmditaTopicrefs", String[].class);
        	extractAllTopics(resourceResolver, topicRefs, topicsReferredInMapList);
        }
        
        String cookieValue = MadisonUtil.getTokenCookieValue(request);
        String endApi = SeeAlsoUtil.getPostUrl(resourceResolver, externalizer);
        
        for(String topicPath : topicsReferredInMapList) {
        	List<String> refList = SeeAlsoUtil.getUsedInReferencePages(topicPath, cookieValue, endApi);
        	
        	// Check for syndication cases
            if(refList.isEmpty()) {
            	String topicTerritory = MadisonUtil.getTerritoryCodeForPath(topicPath);
            	String sourcePathForSubscriber = SeeAlsoUtil.getSourcePathForSubscriber(topicPath, resourceResolver.adaptTo(Session.class), queryBuilder);
            	if(StringUtils.isNotEmpty(sourcePathForSubscriber)) {
            		String sourceTopicTerritory = MadisonUtil.getTerritoryCodeForPath(sourcePathForSubscriber);
            		String sourceTopicPath = topicPath.replace("/"+topicTerritory + "/", "/"+sourceTopicTerritory + "/");
            		refList = SeeAlsoUtil.getUsedInReferencePages(sourceTopicPath, cookieValue, endApi);
            	}
            	// convert all to within the same territory as current topic
            	if(!refList.isEmpty()) {
            		refList = SeeAlsoUtil.getTopicsCorrespondingToCurrentTopic(resourceResolver, refList, topicTerritory);
            	}
            	
            }
            if(!refList.isEmpty()) {
            	LOGGER.debug("Used in references list is {}", refList.toString());
            	Node metadataNode = resourceResolver.getResource(topicPath + "/jcr:content/metadata").adaptTo(Node.class);
            	try {
					metadataNode.setProperty(DITAConstants.META_PWC_SEE_ALSO_DEFAULT_USED_IN_REFS, refList.toArray(new String[0]));
            	} catch (ValueFormatException e) {
    				LOGGER.error("ValueFormatException occured while setting defaultUsedInReferences property {}", e.getMessage());
    			} catch (VersionException e) {
    				LOGGER.error("VersionException occured while setting defaultUsedInReferences property {}", e.getMessage());
    			} catch (LockException e) {
    				LOGGER.error("LockException occured while setting defaultUsedInReferences property {}", e.getMessage());
    			} catch (ConstraintViolationException e) {
    				LOGGER.error("ConstraintViolationException occured while setting defaultUsedInReferences property {}", e.getMessage());
    			} catch (RepositoryException e) {
    				LOGGER.error("RepositoryException occured while setting defaultUsedInReferences property {}", e.getMessage());
    			}
            }
        }
        if(resourceResolver.hasChanges()) {
        	resourceResolver.commit();
        }
    }

    
    private void extractAllTopics(ResourceResolver resourceResolver, String[] topicRefs, Set<String> topicsRerredInMapList) {
		for(String item : topicRefs) {
			String path = item.replace(",", StringUtils.EMPTY).trim();
			if(path.endsWith(DITAConstants.DITA_EXTENSION)) {
				topicsRerredInMapList.add(path);
			} else if(path.endsWith(DITAConstants.DITAMAP_EXT)) {
				ValueMap contentValueMap = resourceResolver.getResource(path + "/jcr:content").adaptTo(ValueMap.class);
		        if(null != contentValueMap) {
		        	String[] topicReferences = contentValueMap.get("fmditaTopicrefs", String[].class);
		        	extractAllTopics(resourceResolver, topicReferences, topicsRerredInMapList);
		        }
			}
		}
		
	}

	@Override
    protected void doPost(@Nonnull final SlingHttpServletRequest request,
            @Nonnull final SlingHttpServletResponse response) throws ServletException, IOException {
    	doGet(request, response);
    }

}
