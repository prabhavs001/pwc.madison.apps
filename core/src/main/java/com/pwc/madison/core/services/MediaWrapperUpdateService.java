package com.pwc.madison.core.services;

import javax.jcr.Node;

import org.apache.sling.api.resource.ResourceResolver;

public interface MediaWrapperUpdateService {

    public void updateMediaWrapperProperties(Node mediaWrapperPageNode, ResourceResolver resourceResolver);
}
