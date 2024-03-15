package com.pwc.madison.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.util.Comparator;
import java.util.LinkedList;

public class BTreeCustom {
    private Node root;
    private Comparator<String> order;
    private Logger log = LoggerFactory.getLogger(BTreeCustom.class);

    
    public BTreeCustom(final Node root, final Comparator<String> order) throws RepositoryException {
        this.root = root;
        this.order = order;
    }
    
    public Node getBTreeNodeForKey(final String key) throws RepositoryException {
        final Node node = this.getPredecessor(key);
        return node;
    }
    
    protected final Node getPredecessor(final String key) throws RepositoryException {
        Node n = this.root;
        Node p;
        do {
            p = n;
            if (p.hasProperty(key)) {
                break;
            }
        } while ((n = this.getPredecessor(p, key)) != null);
        return p;
    }
    
    protected final Node getPredecessor(final Node node, final String key) throws RepositoryException {
        if (!node.hasNodes()) {
            return null;
        }
        int counter = 0;
        for (final Property p : getProperties(node)) {
            final String childKey = p.getName();
            if (this.order.compare(key, childKey) > 0) {
                ++counter;
            }
        }
        final NodeIterator nodes = node.getNodes();
        nodes.skip((long)counter);
        return nodes.nextNode();
    }
    
    public static LinkedList<Property> getProperties(final Node node) throws RepositoryException {
        final PropertyIterator iter = node.getProperties();
        final LinkedList<Property> filtered = new LinkedList<Property>();
        while (iter.hasNext()) {
            final Property prop = iter.nextProperty();
            if (prop.getName().startsWith("jcr:") || prop.getName().startsWith("cq:") || prop.getName().startsWith("sling:")) {
                continue;
            }
            filtered.add(prop);
        }
        return filtered;
    }

}
