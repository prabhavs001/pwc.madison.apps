package com.pwc.madison.core.beans;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class QueuedOutput {

	@SerializedName("outputStatus")
	@Expose
	private String outputStatus;
	@SerializedName("generatedTime")
	@Expose
	private Float generatedTime;
	@SerializedName("outputPath")
	@Expose
	private String outputPath;
	@SerializedName("outputSetting")
	@Expose
	private String outputSetting;
	@SerializedName("initiator")
	@Expose
	private String initiator;
	@SerializedName("outputType")
	@Expose
	private String outputType;
	@SerializedName("ditaotFaliure")
	@Expose
	private Boolean ditaotFaliure;
	@SerializedName("outputTitle")
	@Expose
	private String outputTitle;
	@SerializedName("ditaotLogFile")
	@Expose
	private String ditaotLogFile;
	@SerializedName("generatedIn")
	@Expose
	private Float generatedIn;
	@SerializedName("errorsExist")
	@Expose
	private Boolean errorsExist;
	@SerializedName("elapsedTime")
	@Expose
	private Float elapsedTime;

	public String getOutputStatus() {
		return outputStatus;
	}

	public void setOutputStatus(String outputStatus) {
		this.outputStatus = outputStatus;
	}

	public Float getGeneratedTime() {
		return generatedTime;
	}

	public void setGeneratedTime(Float generatedTime) {
		this.generatedTime = generatedTime;
	}

	public String getOutputPath() {
		return outputPath;
	}

	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}

	public String getOutputSetting() {
		return outputSetting;
	}

	public void setOutputSetting(String outputSetting) {
		this.outputSetting = outputSetting;
	}

	public String getInitiator() {
		return initiator;
	}

	public void setInitiator(String initiator) {
		this.initiator = initiator;
	}

	public String getOutputType() {
		return outputType;
	}

	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}

	public Boolean getDitaotFaliure() {
		return ditaotFaliure;
	}

	public void setDitaotFaliure(Boolean ditaotFaliure) {
		this.ditaotFaliure = ditaotFaliure;
	}

	public String getOutputTitle() {
		return outputTitle;
	}

	public void setOutputTitle(String outputTitle) {
		this.outputTitle = outputTitle;
	}

	public String getDitaotLogFile() {
		return ditaotLogFile;
	}

	public void setDitaotLogFile(String ditaotLogFile) {
		this.ditaotLogFile = ditaotLogFile;
	}

	public Float getGeneratedIn() {
		return generatedIn;
	}

	public void setGeneratedIn(Float generatedIn) {
		this.generatedIn = generatedIn;
	}

	public Boolean getErrorsExist() {
		return errorsExist;
	}

	public void setErrorsExist(Boolean errorsExist) {
		this.errorsExist = errorsExist;
	}

	public Float getElapsedTime() {
		return elapsedTime;
	}

	public void setElapsedTime(Float elapsedTime) {
		this.elapsedTime = elapsedTime;
	}
}
