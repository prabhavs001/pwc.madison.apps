package com.pwc.madison.core.beans;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class GeneratedResponse {

	@SerializedName("outputs")
	@Expose
	private List<Output> outputs = null;
	@SerializedName("queuedOutputs")
	@Expose
	private List<QueuedOutput> queuedOutputs = null;

	public List<Output> getOutputs() {
		return outputs;
	}

	public void setOutputs(List<Output> outputs) {
		this.outputs = outputs;
	}

	public List<QueuedOutput> getQueuedOutputs() {
		return queuedOutputs;
	}

	public void setQueuedOutputs(List<QueuedOutput> queuedOutputs) {
		this.queuedOutputs = queuedOutputs;
	}
}