package com.pwc.madison.core.models.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.models.FootNote;
import com.pwc.madison.core.services.FootNoteService;

@Model(
    adaptables = { SlingHttpServletRequest.class },
    adapters = { FootNote.class },
    resourceType = { FootNoteImpl.RESOURCE_TYPE },
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class FootNoteImpl implements FootNote {

    protected static final String RESOURCE_TYPE = "pwc-madison/components/content/footnote";
    private static final String FOOTNOTE_RESOURCE_TYPE = "pwc-madison/components/ditacontent/footnote";
    private static final Logger LOGGER = LoggerFactory.getLogger(FootNoteImpl.class);
    public static final String P_TAG_NODE = "p";

    @ScriptVariable
    private Page currentPage;

    @SlingObject
    private Resource resource;

    @OSGiService
    private FootNoteService footNoteService;

    List<String> footNoteList = new ArrayList<>();
    Map<String, String> footNoteMap = new LinkedHashMap<>();

    @PostConstruct
    protected void init() {
        resource = currentPage.adaptTo(Resource.class);
        getFootNoteMapForPage(resource, footNoteMap);
    }
    
    public static void getFootNoteMapForPage(Resource resource, Map<String, String> fNMap) {
        if (null == resource) {
            return;
        }
        final Resource childResource = resource.getChild(resource.getPath() + DITAConstants.DITA_PWC_TOPIC_CONTAINER_V1);
        if (null == childResource) {
            return;
        }

        Node node = childResource.adaptTo(Node.class);
        if (null == node) {
            return;
        }
        findAddFootNote(node, fNMap);
    }
    
    protected static void addToMap(Node node, Map<String, String> fNMap) throws RepositoryException {
        String calloutText;
        if (node.hasProperty(DITAConstants.CALLOUT_TEXT) && StringUtils.isNotBlank(node.getProperty(DITAConstants.CALLOUT_TEXT).getValue().getString())) {
            calloutText = node.getProperty(DITAConstants.CALLOUT_TEXT).getValue().getString();
        } else {
            calloutText = String.valueOf(fNMap.size() + 1);
        }

        Document parse = null;
        if(node.hasProperty(DITAConstants.PN_FN_HTML_STRING)) {
            parse = Jsoup.parse(node.getProperty(DITAConstants.PN_FN_HTML_STRING).getValue().getString(),"", Parser.xmlParser());
        }else if(node.hasProperty(DITAConstants.FOOTNOTE_TEXT)){
            parse = Jsoup.parse(node.getProperty(DITAConstants.FOOTNOTE_TEXT).getValue().getString(),"", Parser.xmlParser());
            parse.html("<div class=\"fnote \">"+parse.toString()+"</div>");
        }
        if(parse != null) {
            if (!StringUtils.isEmpty(fNMap.get(calloutText))) {
                calloutText = String.valueOf(fNMap.size() + 1);
            }
            parse.child(0).prepend("<sup>"+calloutText+"</sup>");
            fNMap.put(calloutText, parse.toString());
        }
    }

    protected static boolean isTypeFn(Node node) throws RepositoryException {
        boolean isTypeFn = false;
        if (node.getProperty(DITAConstants.PN_SLING_RESOURCE_TYPE).getValue().getString()
                .equals(FOOTNOTE_RESOURCE_TYPE)) {
            isTypeFn = true;
        }
        return isTypeFn;
    }

    protected static boolean hasNodes(Node node) throws RepositoryException {
        boolean hasNode = false;
        if (node.hasNodes()) {
            hasNode = true;
        }
        return hasNode;
    }

    protected static void findAddFootNote(Node currentNode, Map<String, String> fNMap) {
        try {
            if (isTypeFn(currentNode)) {
                addToMap(currentNode, fNMap);
            }
            NodeIterator nodeIterator = currentNode.getNodes();
            while (nodeIterator.hasNext()) {
                Node currNode = nodeIterator.nextNode();
                if (isTypeFn(currNode)) {
                    addToMap(currNode, fNMap);
                } else {
                    findAddFootNote(currNode, fNMap);
                }
            }
        } catch (RepositoryException repositoryException) {
            LOGGER.debug("RepositoryException in=>{}", repositoryException.getMessage());
        }

    }

    @Override
    public Map<String, String> getFootNoteMap() {
        return footNoteMap;
    }
}
