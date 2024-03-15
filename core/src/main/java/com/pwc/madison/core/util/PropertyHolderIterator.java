package com.pwc.madison.core.util;

import javax.jcr.Property;

import javax.jcr.PropertyIterator;

/**
 * This is a utility file pulled from fmdita code for inline review node creation
 */
public class PropertyHolderIterator{
  protected PropertyIterator iter;
  public PropertyHolderIterator(PropertyIterator iter){
    this.iter = iter;
  }
  public PropertyHolder nextProperty(){
    Property prop = this.iter.nextProperty();
    return new PropertyHolder(prop);
  }
  public boolean hasNext(){
    return this.iter.hasNext();
  }
}