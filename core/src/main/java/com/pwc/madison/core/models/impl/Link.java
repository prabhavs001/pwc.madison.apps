package com.pwc.madison.core.models.impl;

import java.util.List;

import javax.jcr.Node;

import com.pwc.madison.core.constants.DITAConstants;

public class Link {

    private String path;
    private String tocPath;
    private String title;
    private Integer level;
    private String pageName;
    private final String nodeLink;
    private final Boolean toc;
    private Boolean hasChildren;
    private String pathHash;
    private String jcrTitle;
    private String joinedSectionUrl;

    public String getPathHash() {
        return pathHash;
    }

    public void setPathHash(final String pathHash) {
        this.pathHash = pathHash;
    }

    private List<Link> childLinks;

    public List<Link> getChildLinks() {
        return childLinks;
    }

    public void setChildLinks(final List<Link> childLinks) {
        this.childLinks = childLinks;
    }

    /**
     * @return the page
     */
    public Node getNode() {
        return node;
    }

    /**
     * @param node
     *            the page to set
     */
    public void setNode(final Node node) {
        this.node = node;
    }

    /**
     * @param path
     *            the path to set
     */
    public void setPath(final String path) {
        this.path = path;
    }
    
    /**
     * @param path
     *            the path to set
     */
    public void setTocPath(final String path) {
        this.tocPath = null != path && path.contains(DITAConstants.DITA_CONTENT_ROOT) ? path.replace(DITAConstants.DITA_CONTENT_ROOT, DITAConstants.DITA_CONTENT_ROOT_SHORT) : path;
    }

    /**
     * @param title
     *            the title to set
     */
    public void setTitle(final String title) {
        this.title = title;
    }
    
    /**
     * @param jcrTitle
     *            the title to set
     */
    public void setJcrTitle(final String jcrTitle) {
        this.jcrTitle = jcrTitle;
    }

    /**
     * @param level
     *            the level to set
     */
    public void setLevel(final int level) {
        this.level = level;
    }

    /**
     * @param pageName
     *            the level to set
     */
    public void setPageName(final String pageName) {
        this.pageName = pageName;
    }
    
    public void setHasChildren(final boolean hasChildren) {
        this.hasChildren = hasChildren;
    }
    
    /**
     * @param path
     *            the path to set
     */
    public void setJoinedTocPagePath(final String path) {
        this.joinedSectionUrl = null != path && path.contains(DITAConstants.DITA_CONTENT_ROOT) ? path.replace(DITAConstants.DITA_CONTENT_ROOT, DITAConstants.DITA_CONTENT_ROOT_SHORT) : path;
    }

    private Node node;

    public Link(final String path, final String title, final Integer level, final Node node, final String pageName,
            final String nodeLink, final Boolean toc, final String pathHash) {
        this.title = title;
        this.level = level;
        this.node = node;
        this.pageName = null != pageName && pageName.contains(DITAConstants.DITA_CONTENT_ROOT) ? pageName.replace(DITAConstants.DITA_CONTENT_ROOT, DITAConstants.DITA_CONTENT_ROOT_SHORT) : pageName;
        this.nodeLink = null != nodeLink && nodeLink.contains(DITAConstants.DITA_CONTENT_ROOT) ? nodeLink.replace(DITAConstants.DITA_CONTENT_ROOT, DITAConstants.DITA_CONTENT_ROOT_SHORT) : nodeLink;
        this.toc = toc;
        this.pathHash = pathHash;
    }

    public String getPath() {
        return path;
    }
    
    public String getTocPath() {
        return tocPath;
    }
    
    public String getJoinedTocPagePath() {
        return joinedSectionUrl;
    }

    public int getLevel() {
        return level;
    }

    public String getTitle() {
        return title;
    }
    
    public String getJcrTitle() {
        return jcrTitle;
    }

    public String getPageName() {
        return pageName;
    }

    public String getNodeLink() {
        return nodeLink;
    }

    public Boolean getToc() {
        return toc;
    }
    
    public Boolean getHasChildren() {
        return hasChildren;
    }
}
