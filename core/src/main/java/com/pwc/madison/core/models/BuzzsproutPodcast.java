package com.pwc.madison.core.models;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;

/**
 * Buzzsprout Podcast Model.
 * This model would be used on landing pages to render Buzzsprout Podcast component.
 */

@Model(adaptables = Resource.class)
public class BuzzsproutPodcast {
	
	@Inject
	@Optional
	public Resource scripts;

}