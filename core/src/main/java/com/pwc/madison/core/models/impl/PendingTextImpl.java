package com.pwc.madison.core.models.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ExporterConstants;
import com.pwc.madison.core.models.PendingText;

/**
 * Model used to fetch Pending Text information
 */
@Model(adaptables = Resource.class, adapters = { PendingText.class }, resourceType = PendingTextImpl.RESOURCE_TYPE)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class PendingTextImpl implements PendingText {
    private static final Logger LOGGER = LoggerFactory.getLogger(PendingTextImpl.class);

    static final String RESOURCE_TYPE = "pwc-madison/components/ditacontent/pendingtext";
    static final String ASC_TEXT = "asc-text";
    static final String DATE_EFFECTIVE = "date-effective";
    static final String XREF = "xref";
    private String dateEffective = StringUtils.EMPTY;
    private String transitionRef = StringUtils.EMPTY;
    private List<String> ascTextList = Collections.emptyList();

    @Self
    Resource currentResource;

    @Inject
    ResourceResolver resourceResolver;

    /**
     * Fetches date-effective, transitionreference and asc-text from pending-text
     */
    @PostConstruct
    protected void init() {
        ascTextList = new ArrayList<>();
        final Resource dateEffectiveResource = currentResource.getChild(DATE_EFFECTIVE);
        if (null != dateEffectiveResource) {
            dateEffective = dateEffectiveResource.getPath();
        }
        final Resource transitionRefResource = currentResource.getChild(XREF);
        if (null != transitionRefResource) {
            transitionRef = transitionRefResource.getPath();
        }
        final Node currentNode = currentResource.adaptTo(Node.class);
        findAddAcsText(currentNode, ascTextList);
    }

    /**
     * Iterate through the child nodes and fetch all acc-text nodes
     *
     * @param currentNode
     * @param ascTextList
     */
    private void findAddAcsText(final Node currentNode, final List<String> ascTextList) {

        if (null == currentNode) {
            return;
        }

        try {

            final NodeIterator nodeIterator = currentNode.getNodes();
            while (nodeIterator.hasNext()) {
                final Node currNode = nodeIterator.nextNode();
                if (!currNode.getName().equals(DATE_EFFECTIVE) && !currNode.getName().equals(XREF)) {
                    ascTextList.add(currNode.getPath());
                }
            }
        } catch (final RepositoryException repositoryException) {
            LOGGER.debug("RepositoryException in=>{}", repositoryException.getMessage());
        }
    }

    @Override
    public String getDateEffective() {
        return dateEffective;
    }

    @Override
    public String getTransitionRef() {
        return transitionRef;
    }

    @Override
    public List<String> getAscTextList() {
        return ascTextList;
    }
}
