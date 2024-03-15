package com.pwc.madison.core.util;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.commons.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is a utility file pulled from fmdita code for inline review node creation
 */
public class State{
    protected Session session;
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    public State(Session session){
        this.session = session;
    }

    public void refresh(boolean saveChanges) throws RepositoryException {
        session.refresh(saveChanges);
    }

    public boolean nodeExists(String absPath) {
        try {
            return this.session.nodeExists(absPath);
        } catch (RepositoryException e) {
            log.error("Unable to check  nodeExists due to {} ", e.getMessage() );
            return false;
        }
    }

    public NodeHolder getNode(String path){
        try{
            return new NodeHolder(this.session.getNode(path));
        }
        catch(RepositoryException e){
            log.error("Unable to get Node due to {} ", e.getMessage() );
            return null;
        }
    }
    public NodeHolder getOrAddNode(String absolutePath, String intermediateType, String type){
        try{
            Node newNode = JcrUtils.getOrCreateByPath(absolutePath, false,
                    intermediateType, type, this.session, false);
            return new NodeHolder(newNode);
        }
        catch(RepositoryException e){
            log.error("Unable to add Node due to {} ", e.getMessage() );
            return null;
        }
    }
    public Node getJCRNode(String path){
        try{
            return this.session.getNode(path);
        }
        catch(RepositoryException e){
            log.error("Unable to get Node due to ", e.getMessage() );
            return null;
        }
    }
    public boolean save(){
        try{
            this.session.save();
            return true;
        }
        catch(Exception e){
            return false;
        }
    }

    public ValueFactory getValueFactory(){
        try {
            return this.session.getValueFactory();
        } catch (RepositoryException ex) {
            log.error("Unable to get valuefactory ", ex.getMessage() );
            return null;
        }
    }

    public Session getSession() {
        return this.session;
    }

}
