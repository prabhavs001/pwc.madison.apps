package com.pwc.madison.core.models;

import java.util.HashSet;
import java.util.Set;

/**
 * Model for Citation Text File Reference elements
 */
public class CitationTextFileRefrenceModel {

	private String citationText;
	private String citationId;
	private Set<String> filePaths = new HashSet<>();

	public String getCitationText() {
		return citationText;
	}

	public Set<String> getFilePaths() {
		return filePaths;
	}

	public String getCitationId() {
		return citationId;
	}

	public void setCitationId(final String citationId) {
		this.citationId = citationId;
	}

	public void setCitationText(final String citationText) {
		this.citationText = citationText;
	}

	public void setFilePaths(final Set<String> filePaths) {
		this.filePaths = filePaths;
	}

}
