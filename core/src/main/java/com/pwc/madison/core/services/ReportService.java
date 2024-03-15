package com.pwc.madison.core.services;

import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.RepositoryException;
import java.util.ArrayList;

/**
 * The Interface ReportService is used to configure Custom Reports Execution
 */
public interface ReportService {

    /**
     * Gets the batch limit.
     *
     * @return the batch limit
     */
    public int getBatchLimit();

    /**
     * Gets the read time out.
     *
     * @return the read time out
     */
    public int getReadTimeOut();

    /**
     * Method to return display text value for value passed
     * 
     * @param resourceResolver
     *            resource resolver
     * @param dataSource
     *            datasource
     * @param value
     *            value whose text value needs to be returned
     * @return display text value
     * @throws RepositoryException
     */
    String getOptionTextValue(ResourceResolver resourceResolver, String dataSource, String[] value)
            throws RepositoryException;

    ArrayList<String> getTagType(ResourceResolver resolver, String path, String type);

}
