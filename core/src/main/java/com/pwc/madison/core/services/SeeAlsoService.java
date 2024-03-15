package com.pwc.madison.core.services;

import org.apache.sling.api.resource.ResourceResolver;

public interface SeeAlsoService {

	void addSeeAlsoSection(ResourceResolver resolver, String sourcePath);

}
