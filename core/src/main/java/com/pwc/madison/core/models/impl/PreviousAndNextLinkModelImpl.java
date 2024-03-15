package com.pwc.madison.core.models.impl;

import java.util.LinkedList;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.day.cq.wcm.api.PageManager;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.DITAUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.models.PreviousAndNextLinkModel;

/**
 * Sling model for Previous and Next component.
 *
 * @author vhs
 *
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = PreviousAndNextLinkModel.class)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class PreviousAndNextLinkModelImpl implements PreviousAndNextLinkModel {

    private final String tocEntry = "/jcr:content/toc";
    private static final Logger log = LoggerFactory.getLogger(PreviousAndNextLinkModelImpl.class);
    private static final String HASH_TOP_S = "-tOp-S";
    private final String toc = "toc";
    private final String validEntry = "entry";
    private final String link = "link";
    private final String title = "title";

    @ScriptVariable
    private Page currentPage;

    @SlingObject
    private ResourceResolver resourceResolver;

    @Override
    public Link getPrevPage() {
        return prevPage;
    }

    @Override
    public Link getNextPage() {
        return nextPage;
    }

    private final Link prevPage = new Link(null, null, 0, null, null, null, null, null);
    private final Link nextPage = new Link(null, null, 0, null, null, null, null, null);

    /**
     * Init Method of Model.
     */
    @PostConstruct
    protected void init() throws RepositoryException {
        log.debug("Inside PreviousAndNextLinkModelImpl ::: ");
        final Session session = resourceResolver.adaptTo(Session.class);

        final ValueMap properties = currentPage.getContentResource().getValueMap();
        final String basePath = properties.get("basePath", String.class);
        final String searchKey = currentPage.getPath().concat(".html");

        try {
            if (null != session && StringUtils.isNotBlank(basePath) && session.nodeExists(basePath + tocEntry)) {
                final Node rootNode = session.getNode(basePath + tocEntry);
                // Get the current page toc node.
                final Node tocNode = getBFS(rootNode, searchKey);
                if (null != tocNode) {
                    // Logic to get the previous page link and title
                    setPrevPageLinkTitle(tocNode, searchKey);
                    // Logic to get the next page link and title
                    setNextPageLinkTitle(tocNode, searchKey);
                }
            }
        } catch (final RepositoryException e) {
            log.error("Error in Model::: {}", e);
        }
    }

    /**
     * Method to get the previous page of the current page and set the title and link.
     * 
     * @param rootNode
     * @param currentPage
     * @throws RepositoryException
     */
    private void setPrevPageLinkTitle(final Node rootNode, final String currentPage) throws RepositoryException {

        Node prevNode = getPreviousToCNode(rootNode, currentPage);
        if (isChunked(prevNode)) {
            prevNode = getParentChunkedNode(prevNode);
        }
        if (null != prevNode) {
            prevPage.setPath(getPathForChunkedNode(getNodeLink(prevNode, Boolean.TRUE)));
            prevPage.setTitle(getNodeTitle(prevNode));
        }
    }

    private String getPathForChunkedNode(final String path) {
        // Add random hash value for chunked previous and next
        if (StringUtils.contains(path, "#")) {
            return path + HASH_TOP_S;
        }
        return path;
    }

    /**
     * Get parentChunkedNode
     * 
     * @param cNode
     * @return
     * @throws RepositoryException
     */
    private Node getParentChunkedNode(Node cNode) throws RepositoryException {
        if (cNode != null) {
            while (cNode.getParent() != null && isSamePage(cNode, cNode.getParent())) {
                cNode = cNode.getParent();
            }
        }
        return cNode;
    }

    /**
     * Method to get the next page of the current page and set the title and link.
     * 
     * @param rootNode
     * @param currentPage
     * @throws RepositoryException
     */
    private void setNextPageLinkTitle(final Node rootNode, final String currentPage) throws RepositoryException {

        final Node nextNode = getNextToCNode(rootNode, currentPage, false);
        if (null != nextNode) {
            nextPage.setPath(getPathForChunkedNode(getNodeLink(nextNode, Boolean.TRUE)));
            nextPage.setTitle(getNodeTitle(nextNode));
        }
    }

    /**
     * Method to find the current page equivalent toc node using breadth first search.
     * 
     * @param root
     * @param currentPage
     * @return
     * @throws RepositoryException
     */
    private Node getBFS(final Node root, final String currentPage) throws RepositoryException {
        int curlevel = 1;
        int nextlevel = 0;
        final LinkedList<Node> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            final Node node = queue.remove(0);
            if (curlevel == 0) {
                curlevel = nextlevel;
                nextlevel = 0;
            }

            final NodeIterator linksIterator = node.getNodes();
            if (null != linksIterator) {
                while (linksIterator.hasNext()) {
                    final Node child = linksIterator.nextNode();
                    if (child.getName().startsWith(validEntry)
                            && getNodeLink(child, Boolean.FALSE).equals(currentPage)) {
                        return child;
                    }
                    queue.addLast(child);
                    nextlevel++;
                }
            }
            curlevel--;
        }
        return null;
    }

    private Node getPreviousToCNode(final Node currentToCNode, final String currentPage) throws RepositoryException {

        Node previous = null;
        // Get the parent ToC Node of the current page.
        final Node parent = currentToCNode.getParent();
        String parentLink;
        if (parent.getName().equals(toc)) {
            parentLink = toc;
        } else {
            parentLink = getNodeLink(parent, Boolean.FALSE);
        }
        final NodeIterator linksIterator = parent.getNodes();
        int index = 0;
        while (linksIterator.hasNext()) {
            final Node childNode = linksIterator.nextNode();
            final String childLink = getNodeLink(childNode, Boolean.FALSE);
            if (index == 0 && currentPage.equals(childLink)) {
                if (parentLink.equals(currentPage)) {
                    return getPreviousToCNode(childNode, currentPage);
                } else if (toc.equals(parentLink)) {
                    return null;
                } else {
                    return parent;
                }
            }
            // Check if the child is current page.
            if (currentPage.equals(childLink)) {
                break;
            } else {
                previous = childNode;
            }
            index++;
        }
        if (null == previous) {
            previous = parent;
        } else {
            previous = getLastChildNode(previous);
        }
        return previous;
    }

    private Node getLastChildNode(final Node parent) throws RepositoryException {
        Node lastNode = parent;
        final NodeIterator linksIterator = parent.getNodes();
        while (linksIterator.hasNext()) {
            final Node childNode = linksIterator.nextNode();
            if (childNode.getName().startsWith(validEntry)) {
                lastNode = childNode;
            }
        }
        if (lastNode.getNodes().hasNext()) {
            lastNode = getLastChildNode(lastNode);
        }
        return lastNode;
    }

    private Node getFirstChildNode(final Node parent) throws RepositoryException {
        Node firstNode = parent;
        final NodeIterator linksIterator = parent.getNodes();
        final String parentLink = getNodeLink(parent, Boolean.FALSE);
        int index = 0;
        while (linksIterator.hasNext()) {
            final Node childNode = linksIterator.nextNode();
            if (childNode.getName().startsWith(validEntry)) {
                if (index == 0 && getNodeLink(childNode, Boolean.FALSE).equals(parentLink)) {
                    final Node grandChild = getFirstChildNode(childNode);
                    if (childNode != grandChild && !isSamePage(childNode, grandChild)) {
                        return grandChild;
                    }
                    firstNode = childNode;
                } else {
                    return childNode;
                }
                index++;
            }
        }
        return firstNode;
    }

    private Node getNextToCNode(final Node currentToCNode, final String currentPage, final boolean visited)
            throws RepositoryException {

        Node next = null;
        // Get the parent ToC Node of the current page.
        final Node parent = currentToCNode.getParent();
        String parentLink;
        if (parent.getName().equals(toc)) {
            parentLink = toc;
        } else {
            parentLink = getNodeLink(parent, Boolean.FALSE);
        }
        final NodeIterator linksIterator = parent.getNodes();
        boolean match = Boolean.FALSE;
        while (linksIterator.hasNext()) {
            final Node childNode = linksIterator.nextNode();
            if (childNode.getName().startsWith(validEntry)) {
                if (currentPage.equals(getNodeLink(childNode, Boolean.FALSE))) {
                    match = Boolean.TRUE;
                    if (!visited) {
                        final Node firstChild = getFirstChildNode(childNode);
                        if (null != firstChild && childNode != firstChild && !isSamePage(childNode, firstChild)) {
                            return firstChild;
                        }
                    }
                } else if (match) {
                    next = childNode;
                    break;
                }
            }
        }
        if (null != next) {
            return next;
        } else if (match) {
            next = getNextToCNode(parent, parentLink, true);
        }
        return next;
    }

    private String getNodeLink(final Node node, final boolean isPath) throws RepositoryException {
        String nodeLink = "";
        if (node.hasProperty(link)) {
            nodeLink = node.getProperty(link).getString();
            // Validate if it is chunked content, remove id and get the actual content page
            if (isChunked(node) && !isPath) {
                log.debug("Inside PreviousAndNextLinkModelImpl :: getNodeLink: nodeLink with # {}", nodeLink);
                nodeLink = nodeLink.substring(0, nodeLink.indexOf("#"));
            }
        }
        return nodeLink;

    }

    /**
     * Check is node is chuncked
     * 
     * @param node
     * @return
     * @throws RepositoryException
     */
    private boolean isChunked(final Node node) throws RepositoryException {
        if (node != null && node.hasProperty(link)) {
            return StringUtils.contains(node.getProperty(link).getString(), "#");
        }
        return Boolean.FALSE;
    }

    /**
     * Check page path for chunked nodes
     * 
     * @param node
     * @param parentNode
     * @return
     * @throws RepositoryException
     */
    private boolean isSamePage(final Node node, final Node parentNode) throws RepositoryException {
        return StringUtils.equals(getNodeLink(parentNode, Boolean.FALSE), getNodeLink(node, Boolean.FALSE));
    }

    private String getNodeTitle(final Node node) throws RepositoryException {
        String nodeTitle = "";
        if (node.getPath().matches(MadisonConstants.FASB_CONTENT_REGEX)) {
            String pageLink = getNodeLink(node, Boolean.FALSE);
            if(isChunked(node)) {
                PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
                Page page = pageManager.getPage(pageLink.substring(0, pageLink.indexOf(MadisonConstants.HTML_EXTN)));
                String numbering = DITAUtils.getDitaAncestryValue(page, resourceResolver);
                if(StringUtils.isNotEmpty(numbering)){
                    nodeTitle = numbering+" ";
                }
                nodeTitle += node.getProperty(title).getString();
            }else{
                if (node.hasProperty(title)) {
                    nodeTitle = node.getProperty(title).getString();
                }
            }
        }else {
            if (node.hasProperty(title)) {
                nodeTitle = node.getProperty(title).getString();
            }
        }
        return nodeTitle;
    }
}
