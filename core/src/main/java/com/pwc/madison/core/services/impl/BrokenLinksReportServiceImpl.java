package com.pwc.madison.core.services.impl;

import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.Externalizer;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.text.csv.Csv;
import com.pwc.madison.core.beans.BrokenLinksReportRow;
import com.pwc.madison.core.beans.PublishListenerReport;
import com.pwc.madison.core.beans.Summary;
import com.pwc.madison.core.beans.Topic;
import com.pwc.madison.core.beans.Xref;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.BrokenLinksReportService;
import com.pwc.madison.core.services.ReportService;
import com.pwc.madison.core.services.impl.BrokenLinksReportServiceImpl.BrokenLinksReportServiceConfiguration;
import com.pwc.madison.core.util.BrokenLinkReportUtils;
import com.pwc.madison.core.util.BrokenLinkReportUtils.LinkDetails;
import com.pwc.madison.core.util.ReportUtils;

/**
 * The Class brokenLinksReportServiceImpl is a service to retrieve the Vital
 * Stats Report as a List or Csv
 */
@Component(service = { BrokenLinksReportService.class }, configurationPolicy= ConfigurationPolicy.REQUIRE,immediate = true)
@Designate(ocd = BrokenLinksReportServiceConfiguration.class)
public class BrokenLinksReportServiceImpl implements BrokenLinksReportService {

	private static final String COLON_SEPARATOR = ":";

    private static final String CONTENT_FMDITACUSTOM_XREFPATHREFERENCES = "/content/fmditacustom/xrefpathreferences";

    private static final String SEPARATOR = ",";

    private static final Logger LOG = LoggerFactory.getLogger(BrokenLinksReportServiceImpl.class);
    
    private String endApi;
    
    private static final String REPORTS = "reports";
    private static final String LIST = "list";
    private static final String VISIT = "visit";
    private static final String PATHS = "paths";
    private static final String KEY_SPACE = "keySpace";
    private static final String VISITOR = "visitor";
    private static final String TRAVERSE = "traverse";
    private static final String OPERATION = ":operation";

    @Reference
    private Externalizer externalizer;

    @Reference
    private XSSAPI xssAPI;
    
    @Reference
    private ReportService reportService;

    @ObjectClassDefinition(name = "PwC Viewpoint Broken Links Report Configuration")
    public @interface BrokenLinksReportServiceConfiguration {
        @AttributeDefinition(
                name = "Publish Listener Servlet End Point",
                description = "Publish Listener Servlet End Point")
        String pubListenerEndPointApi() default "/bin/publishlistener";
    }

	
	@Override
	public Csv getBrokenLinksCsvReport(List<BrokenLinksReportRow> brokenLinksReport, Csv csv, Writer writer) {
		try {
		    if(writer != null && csv != null) {
		        csv.writeRow("Topic Path", "Broken Links", "Content References", "AEM Page Links");
	             writer.flush();
	             if(brokenLinksReport != null && !brokenLinksReport.isEmpty()) {
	                  Iterator<BrokenLinksReportRow> brokenLinksReportIterator = brokenLinksReport.iterator();
	                    while (brokenLinksReportIterator.hasNext()) {
	                        BrokenLinksReportRow brokenLinksReportRow = brokenLinksReportIterator.next();
	                        try {
	                            List<String> brokenLinks = new ArrayList<String>();
	                            List<String> contentReferences = new ArrayList<String>();
	                            List<String> aemPage = new ArrayList<String>();
	                            if(null == brokenLinksReportRow){
	                                continue;
                                }
	                            List<Xref> links = brokenLinksReportRow.getBrokenLinks();
	                            if(CollectionUtils.isNotEmpty(links)) {
	                                links.forEach((val) -> brokenLinks.add(val.getPath()+COLON_SEPARATOR+val.getLinkStatus()+COLON_SEPARATOR+val.getScope()));    
	                            }
	                            
	                            links = brokenLinksReportRow.getContentReferences();
	                            if(CollectionUtils.isNotEmpty(links)) {
	                                links.forEach((val) -> contentReferences.add(val.getPath()+COLON_SEPARATOR+val.getLinkStatus()));     
	                            }
	                           
	                            links = brokenLinksReportRow.getAemPageLinks();
	                            if(CollectionUtils.isNotEmpty(links)) {
	                                links.forEach((val) -> aemPage.add(val.getPath()+COLON_SEPARATOR+val.getLinkStatus()));
	                            }
	                            csv.writeRow(brokenLinksReportRow.getTopicPath(),String.join(SEPARATOR, brokenLinks),String.join(SEPARATOR, contentReferences),String.join(SEPARATOR, aemPage));
	                            writer.flush();
	                        } catch (IOException e) {
	                            LOG.error("An error while writing csv", e);
	                        }
	                    }
	            }
		    }
			writer.flush();
		} catch (IOException e1) {
			LOG.error("An error while writing csv", e1);
		}
		return csv;
	}

	
	/**
     * Gets the topics list from the folder and populates the response
     *
     * @param folderPath
     * @param resolver
     */
	@Override
	public Map<String, List<Xref>> populateTopicsMapFromFolder(String folderPath, ResourceResolver resolver) {
	    Map<String, List<Xref>> topicsMap = Collections.EMPTY_MAP;
	    if(resolver != null) {
	        Resource folderResource = resolver.getResource(folderPath);
	        if (null != folderResource) {
	            Iterator<Asset> assetIterator = DamUtil.getAssets(folderResource);
	            topicsMap = new HashMap<String, List<Xref>>();
	            while (assetIterator.hasNext()) {
	                Asset childAsset = assetIterator.next();
	                if (childAsset.getName().endsWith(DITAConstants.DITA_EXTENSION)) {
	                    getSMEDetails(childAsset, resolver,topicsMap);
	                }
	            }
	        }
	    }
        return topicsMap;
    }

    /**
     * Get topics from the topicpaths string and populates the response
     *
     * @param topicPaths
     * @param resourceResolver
     */
    @Override
    public Map<String, List<Xref>> populateTopicsMapFromTopics(String topicPaths, ResourceResolver resourceResolver) {
        Map<String, List<Xref>> topicsMap = new HashMap<String, List<Xref>>();
        if(resourceResolver != null && StringUtils.isNotBlank(topicPaths)) {
            String[] topicsArray = topicPaths.split(";");
            for (String topic : topicsArray) {
                Resource topicRes = resourceResolver.getResource(topic);
                if (null != topicRes) {
                    Asset childAsset = topicRes.adaptTo(Asset.class);
                    if (childAsset.getName().endsWith(DITAConstants.DITA_EXTENSION)) {
                        getSMEDetails(childAsset, resourceResolver,topicsMap);
                    }
                }
            }
        }
        return topicsMap;
    }

    /**
     * Populates the final response map with unpublished pages in the topic metadata
     *
     * @param topic
     * @param resourceResolver
     * @param topicsMap 
     */
    private void getSMEDetails(Asset topic, ResourceResolver resourceResolver, Map<String, List<Xref>> topicsMap) {
        List<Xref> unPublishedPages = new ArrayList<Xref>();
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        String topicMetaPath = topic.getPath() + MadisonConstants.METADATA_PATH;
        ValueMap valueMap = resourceResolver.getResource(topicMetaPath).getValueMap();
        if (valueMap.containsKey(MadisonConstants.SME_PROPERTY)) {
            String[] smePages = valueMap.get(MadisonConstants.SME_PROPERTY,String[].class);
            for (String smePage : smePages) {
                Xref link = new Xref();
                Page page = pageManager.getPage(smePage);
                if (null != page) {
                    link.setPath(page.getPath());
                    ValueMap pageValMap = page.getProperties();
                    if (!pageValMap.containsKey(MadisonConstants.REPLICATION_ACTION_PROPERTY)) {
                        link.setLinkStatus(false);
                    }
                    else if(pageValMap.containsKey(MadisonConstants.REPLICATION_ACTION_PROPERTY) && StringUtils.equalsIgnoreCase(MadisonConstants.REPLICATION_ACTION_ACTIVATE, pageValMap.get(MadisonConstants.REPLICATION_ACTION_PROPERTY, String.class))) {
                        link.setLinkStatus(true);
                    }
                    else if(pageValMap.containsKey(MadisonConstants.REPLICATION_ACTION_PROPERTY) && StringUtils.equalsIgnoreCase(MadisonConstants.REPLICATION_ACTION_DEACTIVATE, pageValMap.get(MadisonConstants.REPLICATION_ACTION_PROPERTY, String.class))) {
                        link.setLinkStatus(false);
                    }
                    unPublishedPages.add(link);
                } else{
                    link.setPath(smePage);
                    link.setLinkStatus(false);
                    unPublishedPages.add(link);
                }
            }
        }
        topicsMap.put(topic.getPath(), unPublishedPages);
    }


    @Override
    public PublishListenerReport getPublishListenerReport(ResourceResolver resourceResolver,Map<String, List<Xref>> topicsMap,String cookieValue) {
        PublishListenerReport report = null;
        List<String> paths = new ArrayList<String>();
        if(topicsMap != null && !topicsMap.isEmpty()) {
            topicsMap.entrySet().forEach((entry) ->{
                paths.add(entry.getKey());  
            });
        }
        if(CollectionUtils.isNotEmpty(paths) && StringUtils.isNotBlank(cookieValue)) {
            String endApi = getPostUrl(resourceResolver);
            if(StringUtils.isNotBlank(endApi)) {
                URL url;
                try {
                    url = new URL(endApi);
                    List<BasicNameValuePair> postParams = getPostParams(
                            String.join(MadisonConstants.PIPE_SEPARATOR, paths));
                    report = ReportUtils.getPublishListenerReport(endApi, cookieValue, url.getHost(), postParams,
                            reportService.getReadTimeOut());
                    parseReport(resourceResolver, report);
                } catch (MalformedURLException e) {
                   LOG.error("Error getting hostname");
                }
            }
        }
        return report;
    }
    
    private List<BasicNameValuePair> getPostParams(String paths) {
        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair(OPERATION, VISIT));
        params.add(new BasicNameValuePair(TRAVERSE, LIST));
        params.add(new BasicNameValuePair(VISITOR, REPORTS));
        params.add(new BasicNameValuePair(KEY_SPACE, "{}"));
        params.add(new BasicNameValuePair(PATHS, paths));
        return params;
    }

    /**
     * 
     * @param resourceResolver
     * @param report
     */
    private void parseReport(ResourceResolver resourceResolver, PublishListenerReport report) {
        if(report != null) {
            List<Topic> topics = report.getTopics();
            if(CollectionUtils.isNotEmpty(topics)) {
                topics.parallelStream().forEach((tp) -> {
                    String path = tp.getPath();
                    Map<String,BrokenLinkReportUtils.LinkDetails> links = BrokenLinkReportUtils.getScopeDetails(path, resourceResolver);
                    updateReport(tp,links,resourceResolver);
                });
            }
        }
    }
    
    /**
     * Update links based on scope peer
     * @param topic
     * @param links
     */
    private void updateReport(Topic topic,Map<String, LinkDetails> links,ResourceResolver resourceResolver) {
        if(links != null && !links.isEmpty() && resourceResolver != null) {
            Summary summary = topic.getSummary();
            if(summary != null) {
                List<Xref> xrefs = summary.getXrefs();
                if(CollectionUtils.isNotEmpty(xrefs)) {
                    for(Xref ref : xrefs) {
                        BrokenLinkReportUtils.LinkDetails details = links.get(ref.getPath());
                        addScope(resourceResolver, ref, details);
                    }
                }
            }
        }
        
    }


    /**
     * add scope value for xref
     * @param resourceResolver
     * @param ref
     * @param details
     */
    private void addScope(ResourceResolver resourceResolver, Xref ref, BrokenLinkReportUtils.LinkDetails details) {
        if(null != details) {
            if(StringUtils.equalsIgnoreCase(DITAConstants.PEER_SCOPE, details.getScope())) {
                if(isReferenced(ref.getPath(),resourceResolver)) {
                    ref.setLinkStatus(true);
                    ref.setScope(details.getScope());
                }
                else {
                    ref.setLinkStatus(false);
                    ref.setScope(details.getScope());
                }
            }
            else {
                ref.setScope(details.getScope());
            }
        }
    }

    /**
     * Check if path is referenced
     * @param path
     * @param resourceResolver
     * @return
     */
    private boolean isReferenced(String key,ResourceResolver resourceResolver) {
        boolean isReferenced = false;
        try {
            if(StringUtils.isNotBlank(key)) {
                LOG.debug("Searching key {}",key);
                isReferenced = BrokenLinkReportUtils.isReferenced(resourceResolver, key);
            }
        } catch (Exception e) {
            LOG.error("Error reading property");
        }
        LOG.info("Is path {} referenced {}",key,isReferenced);
        return isReferenced;
    }
    
    /**
     * 
     * @param resourceResolver
     * @return
     */
    private String getPostUrl(ResourceResolver resourceResolver) {
        String postRequestApiEndPoint = StringUtils.EMPTY;
        if(externalizer != null) {
            postRequestApiEndPoint = externalizer.externalLink(resourceResolver, Externalizer.LOCAL, this.endApi);
            LOG.info("Post request api {}",postRequestApiEndPoint);
        }
       return postRequestApiEndPoint;
    }


    @Override
    public List<BrokenLinksReportRow> getBrokenLinks(ResourceResolver resourceResolver,Map<String, List<Xref>> topicsMap,String cookieValue) {
        try {
            PublishListenerReport pubListenerreport = getPublishListenerReport(resourceResolver,topicsMap,cookieValue);
            return getReport(pubListenerreport,topicsMap);
        } catch (Exception e) {
            LOG.error("Error in gettingBrokenLinkReport {}",e);
        }
        return null;
    }
    
    /**
     * Get report
     * @param reportTable
     * @return
     */
    private List<BrokenLinksReportRow> getReport(PublishListenerReport pubListenerReport,Map<String, List<Xref>> topicsMap){
        List<BrokenLinksReportRow> brokenLinksReport = new ArrayList<BrokenLinksReportRow>();
        if(pubListenerReport != null) {
            List<Topic> topics = pubListenerReport.getTopics();
            if(CollectionUtils.isNotEmpty(topics)) {
                topics.forEach((row) ->{
                    BrokenLinksReportRow brokenLinksReportRow = new BrokenLinksReportRow();
                    brokenLinksReportRow.setTopicPath(row.getPath());
                    Summary summary = row.getSummary();
                    if(summary != null) {
                        getBrokenLinksFromSummary(summary.getXrefs(),brokenLinksReportRow);
                        getBrokenImagesFromSummary(summary.getImages(),brokenLinksReportRow);
                        getContentReferencesFromSummary(summary.getUsedIn(),brokenLinksReportRow);
                    }
                    brokenLinksReportRow.setAemPageLinks(topicsMap.get(row.getPath()));
                    if(brokenLinksReportRow != null) {
                        brokenLinksReport.add(brokenLinksReportRow);
                    }
                });
            }

        }
        return brokenLinksReport;
    }
    
    /**
     * Get broken links
     * @param xrefs
     * @param brokenLinksReportRow
     */
    private void getBrokenLinksFromSummary(List<Xref> xrefs,BrokenLinksReportRow brokenLinksReportRow){
        List<Xref> links = new ArrayList<Xref>();
        if(CollectionUtils.isNotEmpty(xrefs)) {
           links = xrefs;
        }
        brokenLinksReportRow.setBrokenLinks(links);
    }
    
    /**
     * Get references
     * @param usedIn
     * @param brokenLinksReportRow
     */
    private void getContentReferencesFromSummary(List<Xref> usedIn,BrokenLinksReportRow brokenLinksReportRow){
        List<Xref> links = new ArrayList<Xref>();
        if(CollectionUtils.isNotEmpty(usedIn)) {
            links = usedIn;
        }
        brokenLinksReportRow.setContentReferences(links);
    }
    
    /**
     * Get broken image links
     * @param images
     * @param brokenLinksReportRow
     */
    private void getBrokenImagesFromSummary(List<Xref> images,BrokenLinksReportRow brokenLinksReportRow){
        List<Xref> links = brokenLinksReportRow.getBrokenLinks();
        if(CollectionUtils.isNotEmpty(images)) {
            links.addAll(images);
        }
        brokenLinksReportRow.setBrokenLinks(links);
    }
    
    @Activate
    @Modified
    protected void Activate(final BrokenLinksReportServiceConfiguration brokenLinksReportServiceConfiguration) {
        LOG.info("BrokenLinksReportService : Entered Activate/Modify");
        this.endApi = brokenLinksReportServiceConfiguration.pubListenerEndPointApi();
        LOG.debug("BrokenLinksReportService Activate() Endpoint : {}",
                this.endApi);
    }

}
