package com.pwc.madison.core.models;

/**
 * Model used to convert node list to JSON string array.
 */
public interface NodeJson {

    /**
     * Return the JSON array string of the child nodes of a given path.
     * 
     * @return {@link String}
     */
    public String getNodeJsonString();

}
