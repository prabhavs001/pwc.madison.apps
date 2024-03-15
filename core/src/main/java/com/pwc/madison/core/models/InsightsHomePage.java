package com.pwc.madison.core.models;

import java.util.List;

import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface InsightsHomePage {
	
	public List<InsightsHomePageTile> getFilteredList();

	public String getComponentName();
}
