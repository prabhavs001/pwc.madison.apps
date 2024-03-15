
package com.pwc.madison.core.util;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PropertyIterator;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.commons.JcrUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a utility file pulled from fmdita code for inline review node creation
 */
public class NodeHolder {
  protected Node node;
  protected final Logger log = LoggerFactory.getLogger(this.getClass());

  public NodeHolder(Node node) {
    this.node = node;
  }

  public NodeHolder(String path, State state) {
    this.node = state.getJCRNode(path);
  }

  public boolean hasNode(String relPath) {
    try {
      return this.node.hasNode(relPath);
    } catch (RepositoryException e) {
      log.error("Unable to hasNode  due to ", e.getMessage());
      return false;
    }
  }

  public NodeHolderIterator getNodes() {
    try {
      return new NodeHolderIterator(this.node.getNodes());
    } catch (RepositoryException e) {
      log.error("Unable to getNodes  due to ", e.getMessage());
      return null;
    }
  }

  public NodeHolderIterator getString() throws RepositoryException {
    return new NodeHolderIterator(this.node.getNodes());
  }

  public boolean hasProperty(String key) {
    try {
      return this.node.hasProperty(key);
    } catch (RepositoryException e) {
      log.error("Unable to check hasProperty due to ", e.getMessage());
      return false;
    }
  }

  public Node getJcrNode() {
    return this.node;
  }

  public String getString(String key) {
    Property property;
    String value = null;
    try {
      property = this.node.getProperty(key);
      value = property.getString();
    } catch (RepositoryException e) {
      log.error("Unable to getProperty due to ", e.getMessage());
    }
    return value;
  }

  public boolean setProperty(String name, String value) {
    try {
      this.node.setProperty(name, value);
      return true;
    } catch (RepositoryException e) {
      log.error("Unable to setProperty due to ", e.getMessage());
      return false;
    }
  }

  public boolean setProperty(String name, Long value) {

    try {
      this.node.setProperty(name, (long) value);
      return true;
    } catch (RepositoryException e) {
      log.error("Unable to setProperty due to ", e.getMessage());
      return false;
    }
  }

  public boolean setProperty(String name, Boolean value) {

    try {
      this.node.setProperty(name, value);
      return true;
    } catch (RepositoryException e) {
      log.error("Unable to setProperty due to ", e.getMessage());
      return false;
    }
  }

  public boolean setProperty(String name, Binary value) {
    try {
      this.node.setProperty(name, value);
      return true;
    } catch (RepositoryException e) {
      log.error("Unable to setProperty due to ", e.getMessage());
      return false;
    }
  }

  public boolean setProperty(String name, int value) {

    try {
      this.node.setProperty(name, value);
      return true;
    } catch (RepositoryException e) {
      log.error("Unable to setProperty due to ", e.getMessage());
      return false;
    }
  }

  public boolean setProperty(String name, Integer value) {

    try {
      this.node.setProperty(name, value);
      return true;
    } catch (RepositoryException e) {
      log.error("Unable to setProperty due to ", e.getMessage());
      return false;
    }
  }

  public NodeHolder addNode(String relPath) {
    try {

      Node newNode = this.node.addNode(relPath);
      return new NodeHolder(newNode);
    } catch (RepositoryException e) {
      log.error("Unable to add Node due to ", e.getMessage());
      return null;
    }

  }

  public NodeHolder getOrAddNode(String relPath, String type) {
    try {
      Node newNode = JcrUtils.getOrAddNode(this.node, relPath, type);
      return new NodeHolder(newNode);
    } catch (RepositoryException e) {
      log.error("Unable to add Node due to ", e.getMessage());
      return null;
    }
  }

  public NodeHolder getNode(String id) {
    try {
      Node newNode = this.node.getNode(id);
      return new NodeHolder(newNode);
    } catch (RepositoryException e) {
      log.error("Unable to get Node due to {}", e.getMessage());
      return null;
    }
  }

  public PropertyHolder getProperty(String relPath) throws RepositoryException {
    return new PropertyHolder(this.node.getProperty(relPath));
  }

  public PropertyHolder getPropertyIfExists(String relPath) {
    try {
      return new PropertyHolder(this.node.getProperty(relPath));
    } catch (RepositoryException e) {
      log.trace("Error to getProperty:: ", e);
      log.error("Error to getProperty:: {}", e.getMessage());
    }
    return null;
  }

  public PropertyHolderIterator getProperties(){
    try{
      PropertyIterator iter = this.node.getProperties();
      return new PropertyHolderIterator(iter);
    }
    catch(RepositoryException e){
      log.error("Unable to getProperties due to {}", e.getMessage() );
      return null;
    }

  }
  public boolean hasNodes(){
    try{
      return this.node.hasNodes();
    }
    catch(RepositoryException e){
      log.error("Unable to hasNodes due to ", e.getMessage() );
      return false;
    }
  }
  public String getPath(){
      try {
          return this.node.getPath();
      } catch (RepositoryException ex) {
          log.error("Unable to get Path for node due to ", ex.getMessage() );
          return null;
      }
  }

  public NodeHolder addNode(String parent , String Type){
      try {
          Node newNode = this.node.addNode(parent, Type);
          return new NodeHolder(newNode);
      } catch (RepositoryException ex) {
          log.error("Unable to add node due to ", ex.getMessage() );
          return null;
      }
  }

}