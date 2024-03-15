package com.pwc.madison.core.util;

import com.day.cq.commons.Externalizer;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.pwc.madison.core.beans.BackwardReference;
import com.pwc.madison.core.beans.BackwardReferencesReport;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Session;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility methods for See Also Section
 */
public class SeeAlsoUtil {
    private static final String FORWARD_SLASH = "/";
	private static final Logger LOG = LoggerFactory.getLogger(SeeAlsoUtil.class);
    private static final String PN_DESTINATION_PATH = "destinationPath";
    private static final String PN_SOURCE_PATH = "sourcePath";
    private static final String NODE_TYPE_NT_UNSTRUCTURED = "nt:unstructured";
    private static final String PN_PROPERTY_OPERATION = "property.operation";
    private static final String PN_PROPERTY = "property";
    private static final String PN_LIMIT = "p.limit";

    
    public static String getSourcePathForSubscriber(final String path, final Session session,
            final QueryBuilder queryBuilder) {

        String sourcePath = StringUtils.EMPTY;
        try {
            final Map<String, Object> predicateTocMap = new HashMap<>();
            predicateTocMap.put(PN_LIMIT, MadisonConstants.P_LIMIT);
            predicateTocMap.put(DITAConstants.PATH_PROP_NAME, MadisonConstants.CONF_SYNDICATION_SETTINGS_ROOT);
            predicateTocMap.put(DITAConstants.PROPERTY_TYPE, NODE_TYPE_NT_UNSTRUCTURED);
            predicateTocMap.put(PN_PROPERTY, "@isSyndicated");
            predicateTocMap.put(PN_PROPERTY_OPERATION, MadisonConstants.EXISTS);
            final Query querySyndicatedPaths = queryBuilder.createQuery(PredicateGroup.create(predicateTocMap),
                    session);
            final SearchResult searchResultSyndicatedPaths = querySyndicatedPaths.getResult();
            final Iterator<Resource> sydicatedResources = searchResultSyndicatedPaths.getResources();

            while (sydicatedResources.hasNext()) {
                final Resource resource = sydicatedResources.next();
                if (null != resource) {
                    final Node destNode = resource.adaptTo(Node.class);
                    if (null != destNode && destNode.hasProperty(PN_DESTINATION_PATH)
                            && path.contains(destNode.getProperty(PN_DESTINATION_PATH).getString())) {
                        sourcePath = destNode.getParent().getProperty(PN_SOURCE_PATH).getString();
                        LOG.debug("Source path for subscribing territory path {} is {}", path, sourcePath);
                        break;
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error("Error while retrieving source path from subscribing territory path {}", e.getMessage());
            sourcePath = StringUtils.EMPTY;
        }

        return sourcePath;
    }
    
    public static String getPostUrl(ResourceResolver resourceResolver, Externalizer externalizer) {
        String postRequestApiEndPoint = StringUtils.EMPTY;
        if (externalizer != null) {
        	LOG.debug("Externalizer not null");
            postRequestApiEndPoint = externalizer.externalLink(resourceResolver, Externalizer.LOCAL,
                    "/bin/linkmanager");
            LOG.debug("Post request api {}", postRequestApiEndPoint);
        }
        return postRequestApiEndPoint;
    }
    
    public static List<String> getUsedInReferencePages(String topicPath, String cookieValue, String endApi) {
    	List<String> refList = new ArrayList<>();
    	URL url = null;
		try {
			url = new URL(endApi);
		} catch (MalformedURLException e) {
			LOG.error("URL {} is not valid and MalformedURLException occured {}", endApi, e.getMessage());
			return refList;
		}
    	List<BasicNameValuePair> postParams = new ArrayList<>();
        postParams.add(new BasicNameValuePair("items", topicPath));
        postParams.add(new BasicNameValuePair("operation", "getbackwardrefs"));
        int timeout = 60000;
    	BackwardReferencesReport backwardReferencesReport = ReportUtils.getBackwardReferencesReport(endApi, cookieValue, url.getHost(), postParams, timeout);
        List<BackwardReference> backwardRefs = backwardReferencesReport.getBackwardRefs();
        for(BackwardReference ref : backwardRefs) {
        	if(ref.getPath().equals(topicPath)) {
        		refList = ref.getBackwardRefs().stream().filter(item -> item.endsWith(DITAConstants.DITA_EXTENSION)).collect(Collectors.toList());
        		break;
        	}
        	
        }
        return refList;
    }
    
	public static List<String> getTopicsCorrespondingToCurrentTopic(final ResourceResolver resourceResolver,
			List<String> refList, String topicTerritory) {
		List<String> withinTerritoryList = new ArrayList<>();
		for(String path : refList) {
			String tempTerritory = MadisonUtil.getTerritoryCodeForPath(path);
			String withinSameTerritoryTopicPath = path.replace(FORWARD_SLASH+ tempTerritory + FORWARD_SLASH, FORWARD_SLASH+ topicTerritory + FORWARD_SLASH);
			Resource res = resourceResolver.getResource(withinSameTerritoryTopicPath);
			if(null != res && !ResourceUtil.isNonExistingResource(res)) {
				withinTerritoryList.add(withinSameTerritoryTopicPath);
			}
		}
		return withinTerritoryList;
	}
}
