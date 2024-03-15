package com.pwc.madison.core.services;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.HashSet;

/**
 * This Service used to check the topic is chunked and provide its chunked root
 */
public interface VerifyChunkService {

    /**
     * Checks if the topic is chunked, if chunked returns chunk-root otherwise returns the same topic
     * @param topicPaths
     * @param request
     * @param resolver
     * @param selectedMap
     * @return chunkRoot
     */
    HashSet<String> getChunkedRoot(String[] topicPaths, SlingHttpServletRequest request, ResourceResolver resolver, String selectedMap);
}
