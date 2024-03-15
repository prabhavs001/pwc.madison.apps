package com.pwc.madison.core.reports;
import com.adobe.acs.commons.reports.api.ReportException;
import com.adobe.acs.commons.reports.api.ReportExecutor;
import com.adobe.acs.commons.reports.api.ResultsPage;
import com.day.cq.commons.Externalizer;
import com.day.cq.commons.predicate.AbstractResourcePredicate;
import com.day.cq.commons.predicate.PredicateProvider;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.wcm.commons.ReferenceSearch;
import com.pwc.madison.core.beans.*;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.ReportService;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.ReportUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

/**
 * The Class UnpublishedAssetsReportExecutor is a Extension of ACS Commons Query Report Executor, which gets the assets and
 * filters based on references to find unused assets.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class AssetsReportExecutorModel implements ReportExecutor {

    private static final String JCR_CONTENT_METADATA = "/jcr:content/metadata";

	private static final String ACTIVE = "active";

	private static final String PWC_CONTENT_STATUS = "pwc-content-status";

	private AssetsReportConfig config;

    private int page;

    @SlingObject
    private SlingHttpServletRequest request;

    protected static final Logger log = LoggerFactory.getLogger(UnusedAssetsReportExecutor.class);

    @Inject
    ReportService reportService;

    @Inject
    private Externalizer externalizer;

    @Inject
    private PredicateProvider provider;

    private static final String GETBACKWARDREFS = "getbackwardrefs";

    private static final String GETFORWARDREFS = "getforwardrefs";

    private static final String OPERATION = "operation";

    private static final String ITEMS = "items";

    private static final String WCMCONTENT_PREDICATE = "wcmcontent";

    /*
     * (non-Javadoc)
     *
     * @see com.adobe.acs.commons.reports.api.ReportExecutor#getResults()
     */
    @Override
    public ResultsPage getResults() throws ReportException {
        return fetchUnusedAssets(config.getPageSize(), config.getPageSize() * page);
    }

    /**
     * Fetch unused assets.
     *
     * @param limit
     *            the limit
     * @param offset
     *            the offset
     * @return the results page
     * @throws ReportException
     *             the report exception
     */
    private ResultsPage fetchUnusedAssets(int limit, int offset) {
        ResourceResolver resolver = request.getResourceResolver();
        String path = request.getParameter("path");
        List<Object> results = new ArrayList<>();
        if (path != null) {
            Resource folderResource = resolver.getResource(path);
            if (folderResource != null) {
                Iterator<Asset> assetIterator = DamUtil.getAssets(folderResource);
                List<String> unusedAssets = getUnusedAssets(assetIterator);
                Iterator<String> unusedAssetsIterator = getUnusedAssetsResult(unusedAssets, limit, offset);
                List<MadisonAsset> unusedAssetsList = getUnusedAssetsSortedByTitle(unusedAssetsIterator, resolver);
                for (MadisonAsset asset : unusedAssetsList) {
                    results.add(resolver.getResource(asset.getPath()));
                }
            }
        }

        return new ResultsPage(results, config.getPageSize(), page);
    }

    /**
     * Gets the unused assets sorted by title.
     *
     * @param unusedAssetsIterator
     *            the unused assets iterator
     * @param resolver
     *            the resolver
     * @return the unused assets sorted by title
     */
    private List<MadisonAsset> getUnusedAssetsSortedByTitle(Iterator<String> unusedAssetsIterator,
                                                            ResourceResolver resolver) {
        List<MadisonAsset> unusedAssetsList = new ArrayList<>();
        while (unusedAssetsIterator.hasNext()) {
            MadisonAsset madisonAsset = new MadisonAsset();
            Resource assetResource = resolver.getResource(unusedAssetsIterator.next());
            if (assetResource != null) {
                Asset asset = assetResource.adaptTo(Asset.class);
                if (asset != null) {
                    madisonAsset.setPath(assetResource.getPath());
                    if (asset.getMetadataValue("dc:title") != null) {
                        madisonAsset.setTitle(asset.getMetadataValue("dc:title"));
                    } else {
                        madisonAsset.setTitle(StringUtils.EMPTY);
                    }

                    unusedAssetsList.add(madisonAsset);
                }
            }

        }
        Collections.sort(unusedAssetsList);
        return unusedAssetsList;
    }

    /**
     * Gets the unused assets result.
     *
     * @param unusedAssets
     *            the unused assets
     * @param limit
     *            the limit
     * @param offset
     *            the offset
     * @return the unused assets result
     */
    private Iterator<String> getUnusedAssetsResult(List<String> unusedAssets, int limit, int offset) {
        if (page != -1) {
            if (offset < unusedAssets.size()) {
                return unusedAssets
                        .subList(offset,
                                ((offset + limit) < unusedAssets.size() ? (offset + limit) : unusedAssets.size()))
                        .iterator();
            } else {
                return new ArrayList<String>().iterator();
            }

        } else {
            return unusedAssets.iterator();
        }
    }

    /**
     * Gets the unused assets.
     *
     * @param nodes
     *            the nodes
     * @return the unused assets
     */
    protected List<String> getUnusedAssets(Iterator<Asset> assetIterator) {
        String cookieValue = MadisonUtil.getTokenCookieValue(request);
        List<String> paths = new ArrayList<>();
        while (assetIterator.hasNext()) {
            paths.add(assetIterator.next().getPath());
        }

        if (!paths.isEmpty() && StringUtils.isNotBlank(cookieValue)) {
            String endApi = getPostUrl(request.getResourceResolver());
            if (StringUtils.isNotBlank(endApi)) {
                URL url;
                try {
                    url = new URL(endApi);
                    List<BackwardReferencesReport> backwardRefsReportList = getBackwardReferencesReportList(paths, url,
                            endApi, cookieValue);
                    removeUsedAssetsFromBackwardRefs(backwardRefsReportList, paths, request.getResourceResolver());
                    List<ForwardReferencesReport> forwardRefsReportList = getForwardReferencesReportList(paths, url,
                            endApi, cookieValue);
                    removeUsedAssetsFromForwardRefs(forwardRefsReportList, paths, request.getResourceResolver());
                    //removeUsedAssetsFromPages(paths, request.getResourceResolver());
                } catch (MalformedURLException e) {
                    log.error("Error getting hostname", e);
                }
            }
        }
        return paths;
    }


    /**
     * Gets the forward references report list.
     *
     * @param paths the paths
     * @param url the url
     * @param endApi the end api
     * @param cookieValue the cookie value
     * @return the forward references report list
     */
    protected List<ForwardReferencesReport> getForwardReferencesReportList(List<String> paths, URL url, String endApi,
                                                                         String cookieValue) {
        int batchLimit = reportService.getBatchLimit();
        List<ForwardReferencesReport> forwardRefsReportList = new ArrayList<>();
        if (paths.size() > batchLimit) {
            int offset = 0;
            int toIndex = 0;
            while (offset < paths.size()) {
                toIndex = (offset + batchLimit) < paths.size() ? (offset + batchLimit) : paths.size();
                List<BasicNameValuePair> postParams = new ArrayList<>();
                postParams.add(new BasicNameValuePair(ITEMS,
                        String.join(MadisonConstants.SEMI_COLON_SEPARATOR, paths.subList(offset, toIndex))));
                postParams.add(new BasicNameValuePair(OPERATION, GETFORWARDREFS));
                ForwardReferencesReport forwardRefsReport = ReportUtils.getForwardReferencesReport(endApi, cookieValue,
                        url.getHost(), postParams, reportService.getReadTimeOut());
                forwardRefsReportList.add(forwardRefsReport);
                offset = (offset + batchLimit) < paths.size() ? (offset + batchLimit) : paths.size();
            }

        } else {
            List<BasicNameValuePair> postParams = new ArrayList<>();
            postParams.add(new BasicNameValuePair(ITEMS, String.join(MadisonConstants.SEMI_COLON_SEPARATOR, paths)));
            postParams.add(new BasicNameValuePair(OPERATION, GETFORWARDREFS));
            ForwardReferencesReport forwardRefsReport = ReportUtils.getForwardReferencesReport(endApi, cookieValue,
                    url.getHost(), postParams, reportService.getReadTimeOut());
            forwardRefsReportList.add(forwardRefsReport);
        }
        return forwardRefsReportList;
    }

    /**
     * Gets the backward references report list.
     *
     * @param paths the paths
     * @param url the url
     * @param endApi the end api
     * @param cookieValue the cookie value
     * @return the backward references report list
     */
    protected List<BackwardReferencesReport> getBackwardReferencesReportList(List<String> paths, URL url, String endApi,
                                                                           String cookieValue) {
        int batchLimit = reportService.getBatchLimit();
        List<BackwardReferencesReport> backwardRefsReportList = new ArrayList<>();
        if (paths.size() > batchLimit) {
            int offset = 0;
            int toIndex = 0;
            while (offset < paths.size()) {
                toIndex = (offset + batchLimit) < paths.size() ? (offset + batchLimit) : paths.size();
                List<BasicNameValuePair> postParams = new ArrayList<>();
                postParams.add(new BasicNameValuePair(ITEMS,
                        String.join(MadisonConstants.SEMI_COLON_SEPARATOR, paths.subList(offset, toIndex))));
                postParams.add(new BasicNameValuePair(OPERATION, GETBACKWARDREFS));
                BackwardReferencesReport backwardRefsReport = ReportUtils.getBackwardReferencesReport(endApi,
                        cookieValue, url.getHost(), postParams, reportService.getReadTimeOut());
                backwardRefsReportList.add(backwardRefsReport);
                offset = (offset + batchLimit) < paths.size() ? (offset + batchLimit) : paths.size();
            }

        } else {
            List<BasicNameValuePair> postParams = new ArrayList<>();
            postParams.add(new BasicNameValuePair(ITEMS, String.join(MadisonConstants.SEMI_COLON_SEPARATOR, paths)));
            postParams.add(new BasicNameValuePair(OPERATION, GETBACKWARDREFS));
            BackwardReferencesReport backwardRefsReport = ReportUtils.getBackwardReferencesReport(endApi, cookieValue,
                    url.getHost(), postParams, reportService.getReadTimeOut());
            backwardRefsReportList.add(backwardRefsReport);
        }
        return backwardRefsReportList;
    }

    /**
     * Removes the asset paths which are referenced in pages.
     *
     * @param paths
     *            the paths
     * @param resourceResolver
     *            the resource resolver
     */
    protected void removeUsedAssetsFromPages(List<String> paths, ResourceResolver resourceResolver) {
        List<String> copyOfPaths = new ArrayList<>(Arrays.asList(new String[paths.size()]));
        Collections.copy(copyOfPaths, paths);
        for (String path : copyOfPaths) {
            if (StringUtils.isNotBlank(path) && !path.endsWith(DITAConstants.DITA_EXTENSION)
                    && !path.endsWith(DITAConstants.DITAMAP_EXT)) {
                ReferenceSearch referenceSearch = new ReferenceSearch();
                referenceSearch.setExact(false);
                referenceSearch.setHollow(true);
                referenceSearch.setMaxReferencesPerPage(-1);
                Predicate predicate = provider.getPredicate(WCMCONTENT_PREDICATE);
                if (predicate instanceof AbstractResourcePredicate) {
                    referenceSearch.setPredicate((AbstractResourcePredicate) predicate);
                }
                Collection<ReferenceSearch.Info> resultSet = referenceSearch.search(resourceResolver, path).values();
                if (!resultSet.isEmpty() && paths.contains(path)) {
                    paths.remove(path);
                }

            }
        }

    }

    /**
     * Removes the asset paths which has forward references.
     *
     * @param forwardRefsReportList
     *            the forward references report list
     * @param paths
     *            the paths
     */
    protected void removeUsedAssetsFromForwardRefs(List<ForwardReferencesReport> forwardRefsReportList,
                                                 List<String> paths, ResourceResolver resourceResolver) {
        if (forwardRefsReportList != null) {
            for (ForwardReferencesReport forwardRefsReport : forwardRefsReportList) {
                List<ForwardReference> forwardRefs = forwardRefsReport.getForwardRefs();
                for (ForwardReference forwardReference : forwardRefs) {
                    String forwardRefPath = forwardReference.getPath();
                    if (forwardRefPath.contains(".dita#")) {
                        forwardRefPath = forwardRefPath.substring(0, forwardRefPath.indexOf('#'));
                    }
                    remove(paths, forwardRefPath, forwardReference.getForwardRefs(), resourceResolver);
                }
            }
        }

    }

    /**
     * Removes the asset paths which have backward references
     *
     * @param report
     *            list the report list
     * @param paths
     *            the paths
     * @param resourceResolver 
     */
    protected void removeUsedAssetsFromBackwardRefs(List<BackwardReferencesReport> reportList,
                                                  List<String> paths, ResourceResolver resourceResolver) {
        if (reportList != null) {
            for (BackwardReferencesReport report : reportList) {
                List<BackwardReference> backwardRefs = report.getBackwardRefs();
                for (BackwardReference backwardReference : backwardRefs) {
                    String backwardRefPath = backwardReference.getPath();
                    if (backwardRefPath.contains(".dita#")) {
                        backwardRefPath = backwardRefPath.substring(0, backwardRefPath.indexOf('#'));
                    }
                    remove(paths, backwardRefPath, backwardReference.getBackwardRefs(), resourceResolver);
                }
            }

        }
    }
    
    private void remove(List<String> paths, String refPath, List<String> references, ResourceResolver resourceResolver) {
    	if (paths.contains(refPath)) {
        	for(String ref : references) {
        		Resource resource = resourceResolver.getResource(ref+JCR_CONTENT_METADATA);
        		if(null != resource && !ResourceUtil.isNonExistingResource(resource)) {
        			ValueMap valueMap = resource.adaptTo(ValueMap.class);
        			if(valueMap.containsKey(PWC_CONTENT_STATUS) && ACTIVE.equals(valueMap.get(PWC_CONTENT_STATUS, String.class))) {
        				paths.remove(refPath);
        				break;
        			}
        		}
        	}
        }
    }

    /**
     * Gets the post url.
     *
     * @param resourceResolver
     *            the resource resolver
     * @return the post url
     */
    protected String getPostUrl(ResourceResolver resourceResolver) {
        String postRequestApiEndPoint = StringUtils.EMPTY;
        if (externalizer != null) {
            log.info("Externalizer not null");
            postRequestApiEndPoint = externalizer.externalLink(resourceResolver, Externalizer.LOCAL,
                    "/bin/linkmanager");
            log.info("Post request api {}", postRequestApiEndPoint);
        }
        return postRequestApiEndPoint;
    }


    /*
     * (non-Javadoc)
     *
     * @see com.adobe.acs.commons.reports.api.ReportExecutor#setConfiguration(org.apache.sling.api.resource.Resource)
     */
    @Override
    public void setConfiguration(Resource config) {
        this.config = config.adaptTo(AssetsReportConfig.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.adobe.acs.commons.reports.api.ReportExecutor#setPage(int)
     */
    @Override
    public void setPage(int page) {
        this.page = page;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.adobe.acs.commons.reports.api.ReportExecutor#getDetails()
     */
    @Override
    public String getDetails() throws ReportException {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Page", Integer.toString(page));
        details.put("Page Size", Integer.toString(config.getPageSize()));
        details.put("Query",
                "This report does not use query and uses Folder Iteration and Reference Search APIs to fetch unused assets.");

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : details.entrySet()) {
            sb.append("<dt>" + StringEscapeUtils.escapeHtml(entry.getKey()) + "</dt>");
            sb.append("<dd>" + StringEscapeUtils.escapeHtml(entry.getValue()) + "</dd>");
        }

        return "<dl>" + sb.toString() + "</dl>";
    }

    /*
     * (non-Javadoc)
     *
     * @see com.adobe.acs.commons.reports.api.ReportExecutor#getParameters()
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

    /*
     * (non-Javadoc)
     *
     * @see com.adobe.acs.commons.reports.api.ReportExecutor#getAllResults()
     */
    @Override
    public ResultsPage getAllResults() throws ReportException {
        setPage(-1);
        return fetchUnusedAssets(Integer.MAX_VALUE, 0);
    }
}
