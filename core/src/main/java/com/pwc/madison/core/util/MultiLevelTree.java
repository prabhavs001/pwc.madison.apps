package com.pwc.madison.core.util;
import java.util.ArrayDeque;

/**
 * This is a utility file pulled from fmdita code for inline review node creation
 */
public class MultiLevelTree {
    protected NodeHolder rootNode;
    protected int depth = 3;
    protected int maxChildren = 20;

    public MultiLevelTree(NodeHolder root, int depth, int maxChildren){
        this.rootNode = root;
        this.maxChildren = maxChildren;
        this.depth = depth;   
    }
    
    public NodeHolder insert(String idParam, String newId){
        return this.insertInOrder(this.rootNode, 1, idParam, newId, "");
    }
    
    protected NodeHolder insertInOrder( NodeHolder parent, int level, String idParam, String newId, String currPath){
        if(level > this.depth){
            if(!this.isLevelFull(parent)){
                return this.addChild(parent, idParam, newId, currPath);
            }
            return null;
        }
        else{
            int i = 1;
            for(;; i++){
                String strChildName = String.valueOf(i);
                NodeHolder childNode = parent.getOrAddNode(strChildName, "nt:unstructured");
                String childPath = (currPath.isEmpty())? strChildName: currPath + "/" + strChildName;
                NodeHolder newNode = insertInOrder(childNode, level +1, idParam, newId, childPath);
                if(newNode != null){
                    return newNode;
                }
                else if(i> this.maxChildren && level >1){
                    return null;
                }
            }
        }
    }
    protected boolean isLevelFull(NodeHolder parent){
        
        NodeHolderIterator iter = parent.getNodes();
        return iter.getSize() >= this.maxChildren;
        
    }

    public NodeHolder addChild(NodeHolder parentNode, String idParam, String newId, String currPath){
        NodeHolder newNode = parentNode.addNode(newId);
        String pathId = currPath + "/" + newId;
        newNode.setProperty(idParam, pathId);
        return newNode;
    }

    static public ArrayDeque<PropertyHolder> getProperties(NodeHolder node) {
        
        PropertyHolderIterator iter = node.getProperties();
        ArrayDeque<PropertyHolder> filtered = new ArrayDeque<>();
        while (iter.hasNext()) {
            PropertyHolder prop = iter.nextProperty();
            if (prop.getName().startsWith("jcr:")) {
                continue;
            }
            filtered.add(prop);
        }
        return filtered;
    }
    
    public NodeHolder find(String id){
        return this.rootNode.getNode(id);
    }    
}