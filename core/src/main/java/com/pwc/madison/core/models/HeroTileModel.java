package com.pwc.madison.core.models;

import java.util.List;

import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface HeroTileModel {

	public List<HeroTileItem> getFilteredList();

	public String getComponentName();
}
