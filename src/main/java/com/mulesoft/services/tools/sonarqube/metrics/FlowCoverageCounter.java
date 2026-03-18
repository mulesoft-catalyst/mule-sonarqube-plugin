package com.mulesoft.services.tools.sonarqube.metrics;

import java.util.ArrayList;

/**
 * Aggregates MUnit coverage data for a Mule configuration file.
 *
 * <p>The counter can operate in two modes:
 * <ul>
 *   <li><b>Line-based</b>: explicit line numbers are provided by the MUnit report</li>
 *   <li><b>Processor-count-based</b>: only message processor counts are available and must be mapped to lines</li>
 * </ul>
 *
 * @version 1.1.0
 * @since 1.1.0
 * @see CoverageSensor
 */
public class FlowCoverageCounter {

	private int processors = 0;
	private int coveredProcessors = 0;
	private boolean hasLineNumbers = false;
	private ArrayList<Integer> coveredLines = new ArrayList<Integer>();
	private ArrayList<Integer> notcoveredLines = new ArrayList<Integer>();

	/**
	 * Adds message processors to the total processor count.
	 *
	 * @param processors number of processors to add
	 */
	public void addProcessors(int processors) {
		this.processors += processors;
	}

	/**
	 * Adds message processors to the covered processor count.
	 *
	 * @param processors number of covered processors to add
	 */
	public void addCoveredProcessors(int processors) {
		this.coveredProcessors += processors;
	}

	/**
	 * Returns the number of covered message processors.
	 *
	 * @return covered processor count
	 */
	public int getCoveredProcessors() {
		return coveredProcessors;
	}

	/**
	 * Returns the total number of message processors.
	 *
	 * @return total processor count
	 */
	public int getProcessors() {
		return processors;
	}

	/**
	 * Replaces the list of covered line numbers.
	 *
	 * @param coveredLines covered line numbers
	 */
	public void setCoveredLines(ArrayList<Integer> coveredLines) {
		this.coveredLines = coveredLines;
	}

	/**
	 * Returns the list of covered line numbers.
	 *
	 * @return covered line numbers
	 */
	public ArrayList<Integer> getCoveredLines() {
		return coveredLines;
	}

	/**
	 * Returns the list of uncovered line numbers.
	 *
	 * @return uncovered line numbers
	 */
	public ArrayList<Integer> getNotcoveredLines() {
		return notcoveredLines;
	}

	/**
	 * Replaces the list of uncovered line numbers.
	 *
	 * @param notcoveredLines uncovered line numbers
	 */
	public void setNotcoveredLines(ArrayList<Integer> notcoveredLines) {
		this.notcoveredLines = notcoveredLines;
	}

	/**
	 * Marks whether the report provides explicit line numbers.
	 *
	 * @param hasLineNumbers true when line numbers are available
	 */
	public void setHasLineNumbers(boolean hasLineNumbers) {
		this.hasLineNumbers = hasLineNumbers;
	}

	/**
	 * Indicates whether the report provides explicit line numbers.
	 *
	 * @return true when line numbers are available
	 */
	public boolean hasLineNumbers() {
		return hasLineNumbers;
	}
}
