package com.pwc.madison.core.services;

import com.day.cq.commons.Externalizer;
import com.google.gson.JsonObject;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;

/**
 * The Interface ActivitiesAuditReportService is used to get the activity fields such as user login, modified date and
 * activity.
 */
public interface PublishDitamapService {

    /**
     * @param publishingPoints
     * @param resourceResolver
     * @param outputType
     * @param externalizer
     */
    void initiatePublishingProcess(String publishingPoints, ResourceResolver resourceResolver,
                                   String outputType, Externalizer externalizer);

    /**
     * @param publishingURL
     * @param ditaMap
     * @param cookieStore
     * @param requestConfig
     */
    boolean publishDitaMap(String publishingURL, String ditaMap, RequestConfig requestConfig,
                                   List<Header> header);

    /**
     * Generates a custom RequestConfig with specified timeout values and disables authentication.
     *
     * @return The RequestConfig with custom timeout settings and authentication disabled.
     */
    RequestConfig setConnectionData();

    /**
     * @param resourceResolver
     * @param assetPath
     * @param permission
     * @return
     */
    boolean checkIfUserHasPermission(ResourceResolver resourceResolver, String assetPath, String permission);

    /**
     * @param json
     * @return
     */
    boolean isSiteGenerated(String json, String outputType, ResourceResolver resourceResolver);

    /**
     * Fetches the publishing status for a DITA map.
     *
     * @param ditaMap           The DITA map to fetch publishing status for.
     * @param requestConfig     The request configuration for fetching the status.
     * @param outputType        The type of output.
     * @param externalizer      An instance of Externalizer for URL generation.
     * @param resourceResolver  The ResourceResolver for handling resources.
     * @return                  The publishing status as a String.
     */
    String fetchPublishingStatus(String ditaMap, String outputType,
                                 Externalizer externalizer, ResourceResolver resourceResolver);

    /**
     * Method to create a revision, published date and document status for all the topics of the given ditamap.
     *
     * @param ditaMap
     */
    void generateRevisionAndSetDocState(String ditaMap, ResourceResolver resolver);

    boolean isPublishingPoint(ResourceResolver resourceResolver, Resource ditaMapResource);

    List<Header> getHeaders();

    void sendAutoPublishCompletionNotifications(final String publishingPointPath, final ResourceResolver resourceResolver);
}
