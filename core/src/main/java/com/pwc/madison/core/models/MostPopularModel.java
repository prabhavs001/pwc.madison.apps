package com.pwc.madison.core.models;

import java.util.List;

import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface MostPopularModel {
	public List<MostPopularItem> getFilteredList();
	public String getComponentName();
}
