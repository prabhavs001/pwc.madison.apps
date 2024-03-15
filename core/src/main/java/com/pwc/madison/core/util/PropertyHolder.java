package com.pwc.madison.core.util;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a utility file pulled from fmdita code for inline review node creation
 */
public class PropertyHolder{
  protected Property property;
  protected final Logger log = LoggerFactory.getLogger(this.getClass());

  public PropertyHolder(Property property){
    this.property = property;
  }

  public String getName(){

    try{
      return this.property.getName();
    }
    catch(RepositoryException e){
      log.error("Unable to getName due to {}", e.getMessage() );
      return null;
    }
  }
  public String getString(){

    try{
      return this.property.getString();
    }
    catch(RepositoryException e){
      log.error("Unable to getValue due to {}", e.getMessage() );
      return null;
    }
  }
  public long getLong(){
    try{
      Value value = this.property.getValue();
      return value.getLong();
    }
    catch(RepositoryException e){
      log.error("Unable to getLong due to {}", e.getMessage() );

    }
    return -1;
  }

  public Boolean getBoolean(){

    try{
      return this.property.getBoolean();
    }
    catch(RepositoryException e){
      log.error("Unable to getBoolean due to {}", e.getMessage() );
      return null;
    }
  }


}