package com.mulesoft.services.tools.sonarqube.metrics;

import java.util.ArrayList;

public class FlowCoverageCounter {

	private int processors = 0;
	private int coveredProcessors = 0;
	private boolean hasLineNumbers = false;
	private ArrayList<Integer> coveredLines = new ArrayList<Integer>();
	private ArrayList<Integer> notcoveredLines = new ArrayList<Integer>();

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

	public void setCoveredLines(ArrayList<Integer> coveredLines) {
		this.coveredLines = coveredLines;
	}

	public ArrayList<Integer> getCoveredLines() {
		return coveredLines;
	}

	public ArrayList<Integer> getNotcoveredLines() {
		return notcoveredLines;
	}

	public void setNotcoveredLines(ArrayList<Integer> notcoveredLines) {
		this.notcoveredLines = notcoveredLines;
	}

	public void setHasLineNumbers(boolean hasLineNumbers) {
		this.hasLineNumbers = hasLineNumbers;
	}

	public boolean hasLineNumbers() {
		return hasLineNumbers;
	}
}
