package com.pwc.madison.core.models;

import java.util.List;

import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface NewsListModel {
	
	public List<NewsListItem> getNewsItems();

	public String getComponentName();

}
