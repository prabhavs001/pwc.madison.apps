package com.pwc.madison.core.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.commons.ReferenceSearch;
import com.pwc.madison.core.beans.FileManagementReportRow;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.FileManagementReportService;
import com.pwc.madison.core.services.MadisonDomainsService;

/**
 * @author sevenkat
 * 
 *         The Class FileManagementReportService is the OSGi service for configuring custom reports and its lists all
 *         the AEM pages created out of dita topic and map.
 */
@Component(service = FileManagementReportService.class, immediate = true)
public class FileManagementReportServiceImpl implements FileManagementReportService {

    private static final String PATH = "path";

    private static final String TRUE = "true";

    private static final String P_GUESS_TOTAL = "p.guessTotal";

    private static final String TYPE = "type";

    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    SlingSettingsService slingService;

    @Reference
    private MadisonDomainsService domainService;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileManagementReportServiceImpl.class);

    @Override
    public List<Object> populateQueryResults(String assetPath, SlingHttpServletRequest request, int limit, int offset,
            List<Object> results) throws RepositoryException {
        if (null != request) {
            Map<String, FileManagementReportRow> reportMap = new HashMap<>();
            ResourceResolver resolver = request.getResourceResolver();
            final Session session = resolver.adaptTo(Session.class);
            final Map<String, String> map = new HashMap<>();
            map.put(PATH, assetPath);
            map.put(TYPE, "dam:Asset");
            map.put("orderby", "@jcr:content/metadata/dc:title");
            map.put("daterange.property", "jcr:content/jcr:lastModified");
            map.put(P_GUESS_TOTAL, TRUE);
            String fromDate = request.getParameter("fromDate");
            String toDate = request.getParameter("toDate");
            if (StringUtils.isNotBlank(fromDate)) {
                map.put("daterange.lowerBound", fromDate);
                map.put("daterange.lowerOperation", ">=");
            }
            if (StringUtils.isNotBlank(fromDate)) {
                map.put("daterange.upperBound", toDate);
                map.put("daterange.upperOperation", "<=");
            }
            final Query query = queryBuilder.createQuery(PredicateGroup.create(map), session);
            query.setStart(offset);
            query.setHitsPerPage(limit);

            SearchResult searchResult = query.getResult();
            for (final Hit hit : searchResult.getHits()) {
                String path = hit.getPath();
                Resource res = resolver.getResource(path);
                results.add(res);
                FileManagementReportRow row = new FileManagementReportRow();
                List<String> referencesList = populateSearchResults(path, resolver);
                row.setAssetPath(referencesList);
                List<String> urlList = getReferencePageUrls(referencesList, resolver);
                row.setReferenceUrls(urlList);
                List<String> statusList = getReferencePageStatus(referencesList, resolver);
                row.setPageStatus(statusList);
                reportMap.put(path, row);
            }
            request.setAttribute("reportMap", reportMap);
        }
        LOGGER.info("FileManagementReportServiceImpl method Query executed and set results in request Attribute");
        return results;
    }

    /**
     * @param referenceList
     * @param rs
     * @return
     */
    private List<String> getReferencePageUrls(List<String> referenceList, ResourceResolver rs) {
        List<String> urlList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(referenceList)) {
            for (String eachPage : referenceList) {
                urlList.add(domainService.getPublishedPageUrl(eachPage, true));
            }
        }
        return urlList;
    }

    /**
     * @param referenceList
     * @param rs
     * @return
     */
    private List<String> getReferencePageStatus(List<String> referenceList, ResourceResolver rs) {
        List<String> statusList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(referenceList)) {
            for (String eachPage : referenceList) {
                Resource res = rs.getResource(eachPage);
                if(null!=res) {
                    Resource jcrRes=res.getChild("jcr:content");
                    if (null != jcrRes && jcrRes.getValueMap().containsKey("cq:lastReplicationAction")) {
                        statusList.add(jcrRes.getValueMap().get("cq:lastReplicationAction", String.class));
                    } 
                }
            }
        }
        return statusList;
    }

    /**
     * @param assetPath
     * @param resourceResolver
     * @param session
     * @param pm 
     * @throws RepositoryException
     */
    private List<String> populateSearchResults(String assetPath, ResourceResolver resourceResolver) {
        if(assetPath.contains(" ")) {
            assetPath= assetPath.trim().replaceAll(" ", "%20"); 
        }
        List<String>  referencesList = new ArrayList<>();
            ReferenceSearch referenceSearch=new ReferenceSearch();
            Map<String, ReferenceSearch.Info> references=referenceSearch.search(resourceResolver, assetPath);
            for(Map.Entry<String,ReferenceSearch.Info> entry: references.entrySet()) {
                ReferenceSearch.Info info=entry.getValue();
                /* excluding preview page references */
                if(!info.getPagePath().startsWith(MadisonConstants.PWC_MADISON_PREVIEW_BASEPATH)){
                    referencesList.add(info.getPagePath());
                }
            }
        return referencesList;
    }
}
