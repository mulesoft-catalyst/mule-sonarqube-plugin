package com.mulesoft.services.tools.sonarqube.metrics;

public class FlowCoverageCounter {

	private int processors = 0;
	private int coveredProcessors = 0;

	public void addProcessors(int processors) {
		this.processors += processors;
	}

	public void addCoveredProcessors(int processors) {
		this.coveredProcessors += processors;
	}

	public int getCoveredProcessors() {
		return coveredProcessors;
	}

	public int getProcessors() {
		return processors;
	}
}
