package com.pwc.madison.core.reports;

import com.adobe.acs.commons.reports.api.ReportException;
import com.adobe.acs.commons.reports.api.ReportExecutor;
import com.adobe.acs.commons.reports.api.ResultsPage;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.Map.Entry;

/**
 * The Class SyndicationReportExecutor is a Extension of ACS Commons Query Report Executor, which gets the information about syndicated assets.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class SyndicationReportExecutor implements ReportExecutor {

    private SyndicationReportConfig config;

    private int page;

    private SlingHttpServletRequest request;

    private static final Logger log = LoggerFactory.getLogger(SyndicationReportExecutor.class);

    private static final String SOURCE_PATH = "sourcePath";
    private static final String DESTINATION_PATH = "destinationPath";
    private ResourceResolver syndicationResourceResolver;


    @OSGiService
    ResourceResolverFactory resourceResolverFactory;

    /**
     * Instantiates a new syndication report executor.
     *
     * @param request
     *            the request
     */
    public SyndicationReportExecutor(SlingHttpServletRequest request) {
        this.request = request;
    }


    /**
     * gets result for syndication with pagination
     * @return syndication results
     * @throws ReportException
     */
    @Override
    public ResultsPage getResults() throws ReportException {
        return fetchSyndicatedAssets(config.getPageSize(), config.getPageSize() * page);
    }

    private ResultsPage fetchSyndicatedAssets(int limit, int offset) {
        List<Object> results = new ArrayList<>();
        try {
            String path = request.getParameter(MadisonConstants.SYNDICATION_INPUT_PATH);
            log.debug("Syndication report executes for path: {}", path);
            final ResourceResolver resolver = request.getResourceResolver();
            syndicationResourceResolver = MadisonUtil
                .getResourceResolver(resourceResolverFactory, MadisonConstants.SYNDICATION_SERVICE_USER);
            List<Resource> syndicationSubscribersList = getSyndicatedSubscribers(path, syndicationResourceResolver);
            if (syndicationSubscribersList.size() > 0) {
                setSubscriberList(syndicationSubscribersList);
                List<Resource> resultResourceList = new ArrayList<>();
                for (Resource subscriber : syndicationSubscribersList) {
                    Iterator<Asset> assetIterator = DamUtil.getAssets(subscriber);
                    while (assetIterator.hasNext()) {
                        String assetPath = assetIterator.next().getPath();
                        if (null != assetPath && assetPath.endsWith(DITAConstants.DITA_EXTENSION)) {
                            Resource currentResource = resolver.getResource(assetPath);
                            if (null != currentResource) {
                                resultResourceList.add(currentResource);
                            }
                        }
                    }
                }
                if (resultResourceList.size() > 0) {
                    Iterator<Resource> filteredSyndicatedList = filterSyndicatedList(resultResourceList, limit, offset);
                    while (filteredSyndicatedList.hasNext()) {
                        Resource filteredResource = filteredSyndicatedList.next();
                        results.add(resolver.getResource(filteredResource.getPath()));
                    }
                }
            }
        }catch (Exception e){
            log.error("Error while fetching syndicated assets: ", e);
        }finally {
            if(syndicationResourceResolver != null){
                syndicationResourceResolver.close();
            }
        }
        return new ResultsPage(results, config.getPageSize(), page);
    }


    /**
     * gets syndication subscriber list also supports sub folder paths
     * @param source
     * @param resourceResolver
     * @return
     */
    private List<Resource> getSyndicatedSubscribers(final String source,
                                                    final ResourceResolver resourceResolver) {
        final List<Resource> syndicatedSubscribers = new ArrayList<>();

        final String confPath = MadisonConstants.CONF_SYNDICATION_SETTINGS_ROOT;
        if(null == resourceResolver){
            return syndicatedSubscribers;
        }
        final Resource syndicationConfRoot = resourceResolver.getResource(confPath);

        // configuration not available
        if (null == syndicationConfRoot) {
            return syndicatedSubscribers;
        }

        // Path not given
        if(StringUtils.isBlank(source)){
            return syndicatedSubscribers;
        }

        final Iterator<Resource> syndicationSources = syndicationConfRoot.listChildren();
        while (syndicationSources.hasNext()) {
            final Resource syndicationSource = syndicationSources.next();
            final String sourcePath = syndicationSource.getValueMap().get(SOURCE_PATH, String.class);

            if (source.equals(sourcePath) || source.startsWith(sourcePath)) {
                final Iterator<Resource> syndicationSubscribers = syndicationSource.listChildren();
                while (syndicationSubscribers.hasNext()) {
                    final Resource syndicationSubscriber = syndicationSubscribers.next();

                    if (syndicationSubscriber.getValueMap().containsKey(MadisonConstants.PN_IS_SYNDICATED)
                        && syndicationSubscriber.getValueMap().get(MadisonConstants.PN_IS_SYNDICATED,
                        Boolean.class)) {
                        if(source.equals(sourcePath)){
                            syndicatedSubscribers.add(resourceResolver
                                .getResource(syndicationSubscriber.getValueMap().get(DESTINATION_PATH, String.class)));
                        }else{
                            /* get destination subfolder path*/
                            String destinationPath = syndicationSubscriber.getValueMap().get(DESTINATION_PATH, String.class);
                            String subfolder = source.replace(sourcePath, "");
                            String destinationSubFolder = destinationPath + subfolder;
                            syndicatedSubscribers.add(resourceResolver
                                .getResource(destinationSubFolder));
                        }
                    }
                }
            }
        }

        return syndicatedSubscribers;
    }


    /**
     * Filters syndicated to show only selected page content
     * @param syndicatedList
     * @param limit
     * @param offset
     * @return filteredSyndicationList
     */
    private Iterator<Resource> filterSyndicatedList(List<Resource> syndicatedList, int limit, int offset) {
        if (page != -1) {
            if (offset < syndicatedList.size()) {
                return syndicatedList
                    .subList(offset,
                        ((offset + limit) < syndicatedList.size() ? (offset + limit) : syndicatedList.size()))
                    .iterator();
            } else {
                return new ArrayList<Resource>().iterator();
            }
        } else {
            return syndicatedList.iterator();
        }
    }

    private void setSubscriberList(List<Resource> pathList){
        String[] subscriberPathList = new String[pathList.size()];
        for(int i=0; i< pathList.size(); i++){
            subscriberPathList[i] = pathList.get(i).getPath();
        }
        request.setAttribute(MadisonConstants.SUBSCRIBER_LIST, subscriberPathList);
    }


    /**
     * sets configuration for syndication report
     * @param config
     */
    @Override
    public void setConfiguration(Resource config) {
        this.config = config.adaptTo(SyndicationReportConfig.class);
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
                "This report does not use query and uses Folder Iteration to fetch syndicated content list.");

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
     * @return all syndication results
     * @throws ReportException
     */
    @Override
    public ResultsPage getAllResults() throws ReportException {
        setPage(-1);
        return fetchSyndicatedAssets(Integer.MAX_VALUE, 0);
    }

}
