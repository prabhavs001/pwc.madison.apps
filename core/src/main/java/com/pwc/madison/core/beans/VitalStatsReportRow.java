package com.pwc.madison.core.beans;

/**
 * 
 * The Class VitalStatsReportRow is a bean for defining the report columns in
 * Vital Stats Report for AICPA documents.
 * 
 */
public class VitalStatsReportRow {

	private String assetPath;

    private String anchors;

	private int words;

	private int links;

	private int tables;

	private int paragraphs;

	private String damSha1;

    public String getAnchors() {
		return anchors;
	}

    public void setAnchors(String anchors) {
		this.anchors = anchors;
	}

	public int getWords() {
		return words;
	}

	public void setWords(int words) {
		this.words = words;
	}

	public int getLinks() {
		return links;
	}

	public void setLinks(int links) {
		this.links = links;
	}

	public int getTables() {
		return tables;
	}

	public void setTables(int tables) {
		this.tables = tables;
	}

	public int getParagraphs() {
		return paragraphs;
	}

	public void setParagraphs(int paragraphs) {
		this.paragraphs = paragraphs;
	}

	public String getDamSha1() {
		return damSha1;
	}

	public void setDamSha1(String damSha1) {
		this.damSha1 = damSha1;
	}

	public String getAssetPath() {
		return assetPath;
	}

	public void setAssetPath(String assetPath) {
		this.assetPath = assetPath;
	}

}
