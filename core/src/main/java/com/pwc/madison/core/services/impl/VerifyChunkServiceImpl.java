package com.pwc.madison.core.services.impl;

import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.asset.api.AssetManager;
import com.day.cq.commons.Externalizer;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.DamConstants;
import com.pwc.madison.core.beans.BackwardReference;
import com.pwc.madison.core.beans.BackwardReferencesReport;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.DitaMapDetails;
import com.pwc.madison.core.services.VerifyChunkService;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.ReportUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * This is the service implementation for VerifyChinkService which contains methods to check the topic is chunked and provide its chunked root
 */

@Component(service = VerifyChunkService.class,
    immediate = false,
    property = {Constants.SERVICE_DESCRIPTION + "= Verify Chunk Service Implementation"})
public class VerifyChunkServiceImpl implements VerifyChunkService {
    private static final Logger LOG = LoggerFactory.getLogger(VerifyChunkServiceImpl.class);
    private static final String LINK_MANAGEER_ENDPOINT = "/bin/linkmanager";
    private static final String GETBACKWARDREFS = "getbackwardrefs";
    private static final String OPERATION = "operation";
    private static final String ITEMS = "items";
    private static final String HREF = "href";

    HashSet<String> finalTopicList = null;
    String cookieValue = StringUtils.EMPTY;
    String postRequestApiEndPoint = StringUtils.EMPTY;
    List<DitaMapDetails> maprefs = null;
    URL url = null;
    String selectedMap = StringUtils.EMPTY;
    boolean chunkRootfound = false;

    @Reference
    private Externalizer externalizer;

    @Override
    public HashSet<String> getChunkedRoot(String[] topicPaths, SlingHttpServletRequest request, ResourceResolver resolver, String selectedMap) {
        LOG.debug("Inside getChunkedRoot");
        final long startTime = System.currentTimeMillis();
        finalTopicList = new HashSet<>();
        this.selectedMap = selectedMap;
        List<String> topics = Arrays.asList(topicPaths);
        cookieValue = MadisonUtil.getTokenCookieValue(request);
        postRequestApiEndPoint = externalizer.externalLink(resolver, Externalizer.LOCAL,
            LINK_MANAGEER_ENDPOINT);
        try {
            url = new URL(postRequestApiEndPoint);
            BackwardReferencesReport backwardRefsReport = getBackwardReferenceReport(topics);
            List<BackwardReference> backwardRefs = backwardRefsReport.getBackwardRefs();
            parseAndGetChunkeRoot(backwardRefs, resolver);
            final long endTime = System.currentTimeMillis();
            LOG.debug("getChunkedRoot took {} seconds to complete the process", (endTime - startTime) / 1000);
        } catch (MalformedURLException e) {
            LOG.error("Error while getting chunk root", e);
        }
        return finalTopicList;
    }


    /**
     * Iterate though the references of a topic and finds out chunk-root
     * @param listenerResponse
     * @param resolver
     */
    private void parseAndGetChunkeRoot(List<BackwardReference> listenerResponse, ResourceResolver resolver) {
        LOG.debug("Inside parseAndGetChunkeRoot");
        maprefs = DITAUtils.getDitaMapsRefs(selectedMap, resolver, null);
        for (BackwardReference ref : listenerResponse) {
            if (ref.getBackwardRefs().isEmpty() ) {
                finalTopicList.add(ref.getPath());
            } else {
                List<String> backwardRefs = ref.getBackwardRefs();
                String topicPath = ref.getPath();
                for (String mapPath : backwardRefs) {
                    if (mapPath.contains(DITAConstants.DITAMAP_EXT) && isValidMap(maprefs, mapPath, selectedMap)) {
                        Resource topicResource = resolver.getResource(ref.getPath());
                        if (null == topicResource) {
                            continue;
                        }
                        String topicName = topicResource.getName();
                        parseMap(ref.getPath(), mapPath, resolver, topicName, topicPath);
                    }
                }
            }
        }
    }


    /**
     * Iteratively parse the ditamap from child map till selected map until chunk-root is found
     * @param currentElement
     * @param mapPath
     * @param resolver
     * @param currentElementName
     * @param intialTopicPath
     * @return chunkRoot
     */
    private String parseMap(String currentElement, String mapPath, ResourceResolver resolver, String currentElementName, String intialTopicPath) {
        LOG.debug("parsing map: {} to check chunked reference of {}", mapPath, currentElement);
        chunkRootfound = false;
        InputStream inputStream;
        AssetManager assetMgr = resolver.adaptTo(AssetManager.class);
        Asset map = assetMgr.getAsset(mapPath);
        if (null == map) {
            finalTopicList.add(currentElement);
            chunkRootfound = true;
            return "";
        }
        inputStream = map.getRendition(DamConstants.ORIGINAL_FILE).getStream();
        try {
            Document document = DITAUtils.convertInputStreamToDocument(inputStream);
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("//mapref[@chunk='to-content'] | //topicref[@chunk='to-content']");
            NodeList chunkedNodeList = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
            if (chunkedNodeList.getLength() > 0) {
                LOG.debug("Chunked element found on current ditamap");
                processChunkNodes(chunkedNodeList, currentElementName, currentElement, mapPath, resolver);
                if(chunkRootfound){
                    return "";
                }
            }
            LOG.debug("Chunked element not found on current ditamap");
            if(mapPath.equals(selectedMap)){
                LOG.debug("current map is selected map so no more iteration to parent ditamap");
                finalTopicList.add(intialTopicPath);
                chunkRootfound =  true;
                return "";
            }
            ArrayList<String> mapList = new ArrayList<>();
            mapList.add(mapPath);
            BackwardReferencesReport backwardRefsReport = getBackwardReferenceReport(mapList);
            List<BackwardReference> backwardReferenceList = backwardRefsReport.getBackwardRefs();
            for (BackwardReference ref : backwardReferenceList) {
                if (ref.getBackwardRefs().isEmpty()) {
                    finalTopicList.add(intialTopicPath);
                    chunkRootfound =  true;
                    return "";
                } else {
                    List<String> backwardRefs = ref.getBackwardRefs();
                    for (String path : backwardRefs) {
                        if (path.contains(DITAConstants.DITAMAP_EXT) && isValidMap(maprefs, path, selectedMap)) {
                            Resource topicResource = resolver.getResource(ref.getPath());
                            if (null == topicResource) {
                                continue;
                            }
                            String topicName = topicResource.getName();
                            if(chunkRootfound){
                                return "";
                            }
                            parseMap(ref.getPath(), path, resolver, topicName, intialTopicPath);
                        }
                    }
                }
            }
        } catch (XPathExpressionException | IOException | SAXException | ParserConfigurationException e) {
            LOG.error("Error Occurred while processing dita object{} ", e);
        } finally {
            try {
                if (null != inputStream) {
                    inputStream.close();
                }
            } catch (IOException e) {
                LOG.error("Error Occurred in finally block while closing the opened streams {} ", e);
            }
        }
        return intialTopicPath;
    }

    /**
     * Iterates through the chunked nodes and returns the chunk root.
     * @param chunkedNodeList
     * @param currentElementName
     * @param currentElement
     * @param mapPath
     * @param resolver
     * @return chunkRoot
     */
    private String processChunkNodes(NodeList chunkedNodeList, String currentElementName, String currentElement, String mapPath, ResourceResolver resolver){
        LOG.debug("inside processChunkNodes");
        for (int i = 0; i < chunkedNodeList.getLength(); i++) {
            NamedNodeMap nodeMap = chunkedNodeList.item(i).getAttributes();
            if(null == nodeMap.getNamedItem(HREF)){
                continue;
            }
            String chunkedHref = nodeMap.getNamedItem(HREF).getNodeValue();
            if (chunkedHref.endsWith(DITAConstants.DITA_EXTENSION)) {
                ArrayList<String> childTopicList = new ArrayList<>();
                LOG.debug("chunking done at dita level");
                if(chunkedHref.endsWith(currentElementName)){
                    finalTopicList.add(currentElement);
                    chunkRootfound =  true;
                }
                NodeList childNodes = chunkedNodeList.item(i).getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {

                    NamedNodeMap attributeMap = childNodes.item(j).getAttributes();
                    if (null == attributeMap || null == attributeMap.getNamedItem(HREF)) {
                        continue;
                    }
                    String href = attributeMap.getNamedItem(HREF).getNodeValue();
                    String childElementPath = getActualPath(href, mapPath, resolver);
                    if(childElementPath.endsWith(DITAConstants.DITA_EXTENSION)){
                        childTopicList.add(childElementPath);
                    }else{
                        childTopicList.add(childElementPath);
                        final List<com.day.cq.dam.api.Asset> topics = getTopicRefsWithMaps(childElementPath, resolver, null);
                        for (com.day.cq.dam.api.Asset asset : topics) {
                            childTopicList.add(asset.getPath());
                        }
                    }
                    if (href.endsWith(currentElementName)) {
                        String actualPath = getActualPath(chunkedHref, mapPath, resolver);
                        LOG.debug("Chunk root: {} found for {}",actualPath, currentElement);
                        finalTopicList.add(actualPath);
                        chunkRootfound =  true;
                    }
                }
                if(chunkRootfound){
                    finalTopicList.addAll(childTopicList);
                    return "";
                }
            } else if (chunkedHref.endsWith(DITAConstants.DITAMAP_EXT)) {
                LOG.debug("chunking done at ditamap level");
                String chunkRoot = getChunkRootForDitaMap(currentElement, chunkedHref, mapPath, resolver);
                if(null != chunkRoot){
                    finalTopicList.add(chunkRoot);
                    chunkRootfound =  true;
                    return "";
                }
            }
        }
        return "";
    }

    /**
     * This method returns respective absolute path for the relative path mentioned on href attribute
     * @param relativePath
     * @param mapPath
     * @param resolver
     * @return actualPath
     */
    private String getActualPath(String relativePath, String mapPath, ResourceResolver resolver){
        String actualPath = StringUtils.EMPTY;
        if(null == relativePath || null == mapPath){
            return  actualPath;
        }
        /* remove dots from relative path (i.e. ../accounting_guides/bankruptcies_and_liq/chapter_2_accounting/21_chapter_overview.US.dita) */
        String[] pathArray = relativePath.split("\\./");
        String  childMapPath = pathArray[pathArray.length-1];
        final List<String> references = getReferences(mapPath, resolver);
        for (String asset :references) {
            Resource assetResource = resolver.getResource(asset);
            if(null == assetResource){
                return  actualPath;
            }
            String assetPath = assetResource.getPath();
            if(assetPath.endsWith(childMapPath)){
                return assetPath;
            }
        }
        return actualPath;
    }

    /**
     * Returns chunk root if chunking is applied on mapref or topicref which points to ditamap
     * @param currentElement
     * @param chunkedHref
     * @param mapPath
     * @param resolver
     * @return chunkRoot
     */
    private String getChunkRootForDitaMap(String currentElement, String chunkedHref, String mapPath, ResourceResolver resolver){
        String ditaMapPath = getActualPath(chunkedHref, mapPath, resolver);
        if(currentElement.endsWith(DITAConstants.DITAMAP_EXT)){
            if(currentElement.equals(ditaMapPath)){
                LOG.debug("Chunk root: {} found for: {}", currentElement, ditaMapPath);
                return currentElement;
            }
        }else {
            final List<com.day.cq.dam.api.Asset> topicRefs = DITAUtils.getTopicRefs(ditaMapPath, resolver, null);
            for (com.day.cq.dam.api.Asset asset : topicRefs) {
                if (currentElement.equals(asset.getPath())) {
                    return ditaMapPath;
                }
            }
        }
        return null;
    }


    /**
     * Returns list of topics by iteratively checking topicrefs under the ditamap
     * @param mapPath
     * @param resolver
     * @return references
     */
    private ArrayList<String> getReferences(String mapPath, ResourceResolver resolver){
        ArrayList<String> references = new ArrayList<>();
        if(StringUtils.isBlank(mapPath)){
            return references;
        }
        final Resource ditaMap = resolver.getResource(mapPath);
        if(null == ditaMap){
            return  references;
        }
        final Resource ditamapContent = ditaMap.getChild(JcrConstants.JCR_CONTENT);
        if(null == ditamapContent){
            return references;
        }
        // read the properties
        final ValueMap properties = ditamapContent.getValueMap();

        final String[] topicReferences = properties.get(DITAConstants.PN_FMDITATOPICREFS, new String[] {});

        if (topicReferences.length > 0) {
            for (String topicRef : topicReferences) {
                if (topicRef.startsWith(",")) {
                    references.add(topicRef.split(",")[1]);
                }
            }
        }
        return references;
    }

    /**
     * Check if the passed reference lies under the publishing point hierarchy
     * @param mapList
     * @param refMappath
     * @param selectedMap
     * @return
     */
    private boolean isValidMap(List<DitaMapDetails> mapList, String refMappath, String selectedMap){
        boolean isValid = false;
        if(null == mapList || StringUtils.isBlank(refMappath) || StringUtils.isBlank(selectedMap)){
            return isValid;
        }
        if(refMappath.equals(selectedMap)){
            isValid = true;
        }else{
            for (DitaMapDetails mapDetails : mapList) {
                if(refMappath.equals(mapDetails.getDitaMapPath())){
                    isValid = true;
                }
            }
        }
        return isValid;
    }

    /**
     * Returns all backward references for given assets
     * @param pathList
     * @return backwardRefsReport
     */
    private BackwardReferencesReport getBackwardReferenceReport(List<String> pathList){
        List<BasicNameValuePair> postParams = new ArrayList<>();
        postParams.add(new BasicNameValuePair(ITEMS,
            String.join(MadisonConstants.SEMI_COLON_SEPARATOR, pathList)));
        postParams.add(new BasicNameValuePair(OPERATION, GETBACKWARDREFS));
        BackwardReferencesReport backwardRefsReport = ReportUtils.getBackwardReferencesReport(postRequestApiEndPoint,
            cookieValue, url.getHost(), postParams, 60000);
        return backwardRefsReport;
    }

    /**
     * Method returns the list of all topic references within a DITAMAP
     *
     * @param ditamapPath
     * @param requestResolver
     * @param topicRefs
     * @return
     */
    public static List<com.day.cq.dam.api.Asset> getTopicRefsWithMaps(final String ditamapPath, final ResourceResolver requestResolver,
                                                              List<com.day.cq.dam.api.Asset> topicRefs) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(ditamapPath) || requestResolver == null) {
            return null;
        }
        if (topicRefs == null) {
            topicRefs = new ArrayList<>();
        }
        final Resource ditaMap = requestResolver.getResource(ditamapPath);
        if (ditaMap != null) {
            final Resource ditamapContent = ditaMap.getChild(JcrConstants.JCR_CONTENT);
            final ValueMap properties = ditamapContent.getValueMap();
            final String[] topicReferences = properties.get(DITAConstants.PN_FMDITATOPICREFS, new String[] {});
            if (topicReferences != null && topicReferences.length > 0) {
                for (String topicRef : topicReferences) {
                    if (topicRef.startsWith(",")) {
                        topicRef = topicRef.split(",")[1];
                    }
                    final Resource map = requestResolver.getResource(topicRef);
                    topicRefs.add(map.adaptTo(com.day.cq.dam.api.Asset.class));
                    getTopicRefsWithMaps(topicRef, requestResolver, topicRefs);
                }
            } else {
                topicRefs.add(ditaMap.adaptTo(com.day.cq.dam.api.Asset.class));
                return topicRefs;
            }
        }
        return topicRefs;
    }

}
