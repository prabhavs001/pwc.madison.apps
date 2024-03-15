package com.pwc.madison.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BTree {

    private final Node root;
    private final int minChildren; // Currently unused maybe used later to join nodes in btree
    private final int maxChildren; // must be 2*minChildren
    private final Comparator<String> order;
    private final Comparator<Property> itemOrder;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public BTree(final Node root) throws RepositoryException {
        this(root, 10, 20);
    }

    public BTree(final Node root, final int minChildren, final int maxChildren) throws RepositoryException {
        this(root, minChildren, maxChildren, org.apache.jackrabbit.commons.flat.Rank.<String>comparableComparator());
    }

    public BTree(final Node root, final int minChildren, final int maxChildren, final Comparator<String> order)
            throws RepositoryException {
        this.root = root;
        this.minChildren = minChildren;
        this.maxChildren = maxChildren;
        this.order = order;
        itemOrder = new Comparator<Property>() {
            @Override
            public int compare(final Property i1, final Property i2) {
                try {
                    return BTree.this.order.compare(i1.getName(), i2.getName());
                } catch (final RepositoryException e) {
                    log.error(e.getMessage());
                }
                return 0;
            }
        };
    }

    public void addProperty(final String key, final Value value) throws RepositoryException {
        final Node node = getPredecessor(key);
        node.setProperty(key, value);
        split(node);
    }

    public Property getProperty(final String key) throws RepositoryException {
        final Node node = getPredecessor(key);
        return node.getProperty(key);
    }

    public Node getBTreeNodeForKey(final String key) throws RepositoryException {
        final Node node = getPredecessor(key);
        return node;
    }

    public void addPropertyToNode(final Node node, final String key, final Value value) throws RepositoryException {
        node.setProperty(key, value);
        split(node);
    }

    public void split(final Node node) throws RepositoryException {
        final LinkedList<Property> properties = getProperties(node);
        final int count = (int) properties.size();
        if (count >= 0 && count <= maxChildren) {
            return;
        }
        final Rank<Property> rank = new Rank<Property>(properties, itemOrder);
        if (root.isSame(node)) {

            // Leave count/2 in current node
            final Property leftProp = rank.take(1).next();
            final NodeIterator restnodes = root.getNodes();
            final Node leftNode = root.addNode(UUID.randomUUID().toString(), "nt:unstructured");
            move(leftProp, leftNode);
            final Iterator<Property> leftProps = rank.take(count / 2 - 1);
            while (leftProps.hasNext()) {
                final Property prop = leftProps.next();
                move(prop, leftNode);
            }
            rank.take(1).next();
            final Property rightProp = rank.take(1).next();
            final Node rightNode = root.addNode(UUID.randomUUID().toString(), "nt:unstructured");
            move(rightProp, rightNode);
            final Iterator<Property> rightProps = rank.take(rank.size());
            while (rightProps.hasNext()) {
                final Property prop = rightProps.next();
                move(prop, rightNode);
            }
            int counter = 0;
            while (restnodes.hasNext()) {
                counter++;
                if (counter <= ((count + 1) / 2)) {
                    move(restnodes.nextNode(), leftNode);
                } else {
                    move(restnodes.nextNode(), rightNode);
                }
            }
        } else {
            final Node parent = node.getParent();
            // Leave count/2 in current node
            rank.take(count / 2);
            // Get mid property to move to parent
            final Property mid = rank.take(1).next();
            // Get name for new node by getting next property
            final Property newprop = rank.take(1).next();
            final Node newnode = parent.addNode(UUID.randomUUID().toString(), "nt:unstructured");
            // Order of nodes must be maintained
            parent.orderBefore(newnode.getName(), getSuccessor(node, parent).getName());
            move(newprop, newnode);
            move(mid, parent);
            final Iterator<Property> restprops = rank.take(rank.size());
            while (restprops.hasNext()) {
                final Property restprop = restprops.next();
                move(restprop, newnode);
            }
            final NodeIterator restnodes = node.getNodes();
            int counter = 0;
            while (restnodes.hasNext()) {
                counter++;
                if (counter > ((count + 1) / 2)) {
                    move(restnodes.nextNode(), newnode);
                } else {
                    restnodes.nextNode();
                }
            }
            split(parent);
        }
    }

    protected final Node getPredecessor(final String key) throws RepositoryException {
        Node p;
        Node n = root;
        do {
            p = n;

            if (p.hasProperty(key)) {
                break;
            }

        } while ((n = getPredecessor(p, key)) != null);
        return p;
    }

    protected final Node getPredecessor(final Node node, final String key) throws RepositoryException {
        if (!node.hasNodes()) {
            return null;
        }

        int counter = 0;
        final Iterator<Property> properties = getProperties(node).iterator();
        while (properties.hasNext()) {
            final Property p = properties.next();
            final String childKey = p.getName();
            if (order.compare(key, childKey) > 0) {
                counter++;
            }
        }
        final NodeIterator nodes = node.getNodes();
        nodes.skip(counter);
        return nodes.nextNode();
    }

    public static Node getSuccessor(final Node node, final Node parent) throws RepositoryException {
        if (parent == null) {
            return null;
        }
        final NodeIterator nodes = parent.getNodes();
        while (nodes.hasNext()) {
            final Node n = nodes.nextNode();
            if (n.isSame(node)) {
                if (nodes.hasNext()) {
                    return nodes.nextNode();
                } else {
                    return null;
                }
            }
        }
        return null;

    }

    public static void move(final Node node, final Node parent) throws RepositoryException {
        final Session session = node.getSession();
        final String destpath = parent.getPath() + "/" + node.getName();
        session.move(node.getPath(), destpath);
    }

    public static void move(final Property prop, final Node node) throws RepositoryException {
        node.setProperty(prop.getName(), prop.getValue());
        prop.remove();
    }

    public static LinkedList<Property> getProperties(final Node node) throws RepositoryException {
        final PropertyIterator iter = node.getProperties();
        final LinkedList<Property> filtered = new LinkedList<>();
        while (iter.hasNext()) {
            final Property prop = iter.nextProperty();
            if (prop.getName().startsWith("jcr:")) {
                continue;
            }
            filtered.add(prop);
        }
        return filtered;
    }
}

class Rank<T> {
    LinkedList<T> values;
    Comparator<T> itemOrder;

    public Rank(final LinkedList<T> values, final Comparator<T> itemOrder) {
        this.values = values;
        this.itemOrder = itemOrder;
        Collections.sort(values, itemOrder);
    }

    public Iterator<T> take(int n) {
        final List<T> newvalues = new ArrayList<>();
        while (n > 0) {
            newvalues.add(values.removeFirst());
            n--;
        }
        return newvalues.iterator();
    }

    public int size() {
        return values.size();
    }
}
