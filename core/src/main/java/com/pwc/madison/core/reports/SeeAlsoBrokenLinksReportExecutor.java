package com.pwc.madison.core.reports;

import com.adobe.acs.commons.reports.api.ReportException;
import com.adobe.acs.commons.reports.api.ReportExecutor;
import com.adobe.acs.commons.reports.api.ResultsPage;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.pwc.madison.core.util.DITALinkUtils;
import com.pwc.madison.core.util.DITAUtils;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.Map.Entry;

import javax.jcr.Session;

/**
 * The Class SeeAlsoBrokenLinksReportExecutor is a Extension of ACS Commons Query Report Executor, which gets the information about see also broken links.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class SeeAlsoBrokenLinksReportExecutor implements ReportExecutor {
	
	private static final String CONTENT_DAM_PWC_MADISON = "/content/dam/pwc-madison";

    private SeeAlsoBrokenLinksReportConfig config;

    private int page;

    private SlingHttpServletRequest request;
    
    @OSGiService private XSSAPI xssAPI;

    private static final Logger log = LoggerFactory.getLogger(SeeAlsoBrokenLinksReportExecutor.class);

    /**
     * Instantiates a new see also broken links report executor.
     *
     * @param request
     *            the request
     */
    public SeeAlsoBrokenLinksReportExecutor(SlingHttpServletRequest request) {
        this.request = request;
    }


    /**
     * gets result for referred topics with pagination
     * @return xref results
     * @throws ReportException
     */
    @Override
    public ResultsPage getResults() throws ReportException {
        return fetchXrefs(config.getPageSize(), config.getPageSize() * page);
    }

    private ResultsPage fetchXrefs(int limit, int offset) {
        List<Object> results = new ArrayList<>();
        try {
            String path = request.getParameter("path");
            log.debug("See also broken report executes for path: {}", path);
            final ResourceResolver resolver = request.getResourceResolver();
            List<String> topicXrefs = getTopicXrefs(resolver, path);
            for(String topicPath : topicXrefs) {
            	Resource  res = resolver.getResource(topicPath + "/jcr:content");
            	if(null != res && !ResourceUtil.isNonExistingResource(res)) {
            		ValueMap contentValueMap = res.adaptTo(ValueMap.class);
            		String[] xrefs = contentValueMap.get("fmditaXrefs", String[].class);
            		if(null !=  xrefs && ArrayUtils.isNotEmpty(xrefs) && brokenLinksExists(xrefs, resolver)) {
            			results.add(resolver.getResource(topicPath));
            		}
            		
            		
            	}
            }
        }catch (Exception e){
            log.error("Error while fetching see also broken assets: ", e);
        }
        return new ResultsPage(results, config.getPageSize(), page);
    }
    
    private static List<String> getTopicXrefs(final ResourceResolver resolver, String lookupPath) {
        final List<String> topicsList = new ArrayList<>();
        final Map<String, Object> predicateMap = new HashMap<>();
        predicateMap.put("p.limit", "-1");
        predicateMap.put("type", "dam:Asset");
        predicateMap.put("path", lookupPath);
        predicateMap.put("property", "jcr:content/@fmditaXrefs");
        predicateMap.put("property.operation", "exists");
        final QueryBuilder queryBuilder = resolver.adaptTo(QueryBuilder.class);

        if (null == queryBuilder) {
            return topicsList;
        }

        final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap),
                resolver.adaptTo(Session.class));
        final SearchResult searchResult = query.getResult();

        final Iterator<Resource> resources = searchResult.getResources();
        while (resources.hasNext()) {
        	topicsList.add(resources.next().getPath());
        }

        return topicsList;

    }
    
    private boolean brokenLinksExists(String[] xrefs, ResourceResolver resolver) {
    	boolean isBrokenLinksExist = false;
    	if(null !=  xrefs && ArrayUtils.isNotEmpty(xrefs)) {
			for(String item : xrefs) {
				if(StringUtils.isNotEmpty(item) && item.contains(CONTENT_DAM_PWC_MADISON)) {
					String path = item.replace(",", StringUtils.EMPTY).trim();
					Resource  res = resolver.getResource(path);
	            	if(null == res || ResourceUtil.isNonExistingResource(res)) {
	            		isBrokenLinksExist = true;
	            		break;
	            	} else {
	            		String pageUrl = DITALinkUtils.getUpdatedXrefLink(resolver.adaptTo(Session.class), path);
	            		//String pagePath = DITAUtils.getPageFromXrefDita(path, resolver, xssAPI);
	            		Resource  pageRes = resolver.getResource(pageUrl.split(".html")[0]);
	            		if(null == pageRes || ResourceUtil.isNonExistingResource(pageRes)) {
		            		isBrokenLinksExist = true;
		            		break;
		            	}
	            	}
				}
			}
		}
    	return isBrokenLinksExist;
    }


    /**
     * sets configuration for see also broken link report
     * @param config
     */
    @Override
    public void setConfiguration(Resource config) {
        this.config = config.adaptTo(SeeAlsoBrokenLinksReportConfig.class);
    }

    /**
     * sets page number
     * @param page
     */
    @Override
    public void setPage(int page) {
        this.page = page;
    }


    /**
     * get the detail of configuration
     * @return
     * @throws ReportException
     */
    @Override
    public String getDetails() throws ReportException {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Page", Integer.toString(page));
        details.put("Page Size", Integer.toString(config.getPageSize()));
        details.put("Query",
                "This report does not use query and uses Folder Iteration to fetch see also content list.");

        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : details.entrySet()) {
            sb.append("<dt>" + StringEscapeUtils.escapeHtml(entry.getKey()) + "</dt>");
            sb.append("<dd>" + StringEscapeUtils.escapeHtml(entry.getValue()) + "</dd>");
        }

        return "<dl>" + sb.toString() + "</dl>";
    }


    /**
     * gets request parameters
     * @return params
     * @throws ReportException
     */
    @Override
    public String getParameters() throws ReportException {
        List<String> params = new ArrayList<>();
        Enumeration<String> keys = request.getParameterNames();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            for (String value : request.getParameterValues(key)) {
                try {
                    params.add(URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new ReportException("UTF-8 encoding available", e);
                }
            }
        }
        return StringUtils.join(params, "&");
    }


    /**
     * gets all results without pagination
     * @return all xref results
     * @throws ReportException
     */
    @Override
    public ResultsPage getAllResults() throws ReportException {
        setPage(-1);
        return fetchXrefs(Integer.MAX_VALUE, 0);
    }

}
