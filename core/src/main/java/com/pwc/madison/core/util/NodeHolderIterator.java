package com.pwc.madison.core.util;

import javax.jcr.Node;
import javax.jcr.NodeIterator;


/**
 * This is a utility file pulled from fmdita code for inline review node creation
 */
public class NodeHolderIterator{
  protected NodeIterator iter;
  public NodeHolderIterator(NodeIterator nodeIterator){
    this.iter = nodeIterator;
  }

  public NodeHolder nextNode(){
    Node node = this.iter.nextNode();
    return new NodeHolder(node);
  }
  public boolean hasNext(){
    return this.iter.hasNext();
  }
  public long getSize(){
    return this.iter.getSize();
  }
  public void skip(long count){
    this.iter.skip(count);
  }
}