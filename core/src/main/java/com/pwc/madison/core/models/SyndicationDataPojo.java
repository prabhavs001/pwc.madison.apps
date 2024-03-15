package com.pwc.madison.core.models;

import java.util.ArrayList;
import java.util.List;

/**
 * This pojo will hold the syndication status for a source path to various subscribing territories
 * 
 * @author Urmila
 *
 */
public class SyndicationDataPojo {

	private String srcPath;
	private List<String> syndicatedContentStatusList = new ArrayList<String>();
	private boolean sourceSyndicationStatus;

	public String getSrcPath() {
		return srcPath;
	}

	public void setSrcPath(String srcPath) {
		this.srcPath = srcPath;
	}

	public List<String> getSyndicatedContentStatusList() {
		return syndicatedContentStatusList;
	}

	public void setSyndicatedContentStatusList(List<String> syndicatedContentStatusList) {
		this.syndicatedContentStatusList = syndicatedContentStatusList;
	}

	public boolean isSourceSyndicationStatus() {
		return sourceSyndicationStatus;
	}

	public void setSourceSyndicationStatus(boolean sourceSyndicationStatus) {
		this.sourceSyndicationStatus = sourceSyndicationStatus;
	}

}
