package com.pwc.madison.core.models;

/**
 * Model for Citation Pattern Result elements
 */
public class CitationPatternResultModel {

	private String status;
	private String sourcePath;
	private String targetPath;
	private String failureReason;
	private String patternName;

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(final String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getTargetPath() {
		return targetPath;
	}

	public void setTargetPath(final String targetPath) {
		this.targetPath = targetPath;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public void setFailureReason(final String failureReason) {
		this.failureReason = failureReason;
	}

	public String getPatternName() {
		return patternName;
	}

	public void setPatternName(final String patternName) {
		this.patternName = patternName;
	}

	@Override
	public String toString() {
		return "CitationPatternResultModel [status=" + status + ", sourcePath=" + sourcePath + ", targetPath="
				+ targetPath + ", failureReason=" + failureReason + ", patternName=" + patternName + "]";
	}

}
