package com.pwc.madison.core.models;

import java.util.List;

import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface FeaturedContentModel {
	
	public List<FeaturedContentItem> getFilteredList();

	public String getComponentName();
}
