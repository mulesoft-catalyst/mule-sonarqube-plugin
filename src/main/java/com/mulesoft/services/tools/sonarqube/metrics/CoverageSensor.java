package com.mulesoft.services.tools.sonarqube.metrics;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mulesoft.services.tools.sonarqube.filter.MuleFilePredicate;
import com.mulesoft.services.tools.sonarqube.language.MuleLanguage;
import com.mulesoft.services.tools.sonarqube.properties.MuleProperties;
import com.mulesoft.services.tools.sonarqube.sensor.MuleSensor;

public class CoverageSensor implements Sensor {

	private final Logger logger = Loggers.get(CoverageSensor.class);
	private static final String MUNIT_NAME_PROPERTY = "mule.munit.properties.name";
	private static final String MUNIT_FLOWS_PROPERTY = "mule.munit.properties.flows";
	private static final String MUNIT_FILES_PROPERTY = "mule.munit.properties.files";
	private static final String MUNIT_LINES_PROPERTY = "mule.munit.properties.lines";
	private static final String MUNIT_COVERAGE_PROPERTY = "mule.munit.properties.coverage";
	private static final String MUNIT_PROCESSOR_COUNT = "mule.munit.properties.processorCount";
	private static final String MUNIT_COVERED_PROCESSOR_COUNT = "mule.munit.properties.coveredProcessorCount";
	private static final String MUNIT_LINE_NUMBER = "mule.munit.properties.lineNumber";
	private static final String MUNIT_COVERED = "mule.munit.properties.covered";

	ObjectMapper objectMapper = new ObjectMapper();

	private static final String MUNIT_REPORT_MULE_3 = java.io.File.separator + "target" + java.io.File.separator
			+ "munit-reports" + java.io.File.separator + "coverage-json" + java.io.File.separator + "report.json";
	private static final String MUNIT_REPORT_MULE_4 = java.io.File.separator + "target" + java.io.File.separator
			+ "site" + java.io.File.separator + "munit" + java.io.File.separator + "coverage" + java.io.File.separator
			+ "munit-coverage.json";

	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.name("Compute the coverage of the applications");
		descriptor.onlyOnLanguage(MuleLanguage.LANGUAGE_KEY);
	}

	@Override
	public void execute(SensorContext context) {
		File munitJsonReport = new File(context.fileSystem().baseDir()
				+ (MuleSensor.getLanguage(context).equals(MuleLanguage.LANGUAGE_MULE4_KEY) ? MUNIT_REPORT_MULE_4
						: MUNIT_REPORT_MULE_3));
		if (munitJsonReport.exists()) {
			Map<String, FlowCoverageCounter> coverage = loadResults(
					MuleProperties.getProperties(MuleSensor.getLanguage(context)), munitJsonReport);
			FileSystem fs = context.fileSystem();
			// Only ConfigurationFiles
			Iterable<InputFile> files = fs.inputFiles(new MuleFilePredicate(new MuleLanguage(context.config()).getFileSuffixes()));
			for (InputFile file : files) {
				saveCoverage(coverage, file.filename(), context, file);
			}
		}
	}

	private Map<String, FlowCoverageCounter> loadResults(Properties props, File munitJsonReport) {
		Map<String, FlowCoverageCounter> coverageMap = null;
		try (Scanner scanner = new Scanner(munitJsonReport)) {

			String jsonInput = scanner.useDelimiter("\\Z").next();
			JsonNode root = objectMapper.readTree(jsonInput);
			String coverage = root.get(props.getProperty(MUNIT_COVERAGE_PROPERTY)).asText();
			if (logger.isDebugEnabled()) {
				logger.debug("Total Coverage :" + coverage);
			}
			ArrayNode files = (ArrayNode) root.get(props.getProperty(MUNIT_FILES_PROPERTY));
			coverageMap = new HashMap<String, FlowCoverageCounter>();
			for (Iterator<JsonNode> iterator = files.iterator(); iterator.hasNext();) {
				JsonNode node = iterator.next();
				String name = node.get(props.getProperty(MUNIT_NAME_PROPERTY)).asText();
				FlowCoverageCounter counter = new FlowCoverageCounter();
				ArrayNode flows = (ArrayNode) node.get(props.getProperty(MUNIT_FLOWS_PROPERTY));
				for (Iterator<JsonNode> flowsIterator = flows.iterator(); flowsIterator.hasNext();) {
					JsonNode flowNode = flowsIterator.next();
					int messageProcessorCount = flowNode.get(props.getProperty(MUNIT_PROCESSOR_COUNT)).asInt();
					int coveredProcessorCount = flowNode.get(props.getProperty(MUNIT_COVERED_PROCESSOR_COUNT)).asInt();
					counter.addProcessors(messageProcessorCount);
					counter.addCoveredProcessors(coveredProcessorCount);
					ArrayNode lines = (ArrayNode) flowNode.get(props.getProperty(MUNIT_LINES_PROPERTY));
					if (lines != null) {
						counter.setHasLineNumbers(true);
						for (Iterator<JsonNode> linesIterator = lines.iterator(); linesIterator.hasNext();) {
							JsonNode linesNode = linesIterator.next();
							int lineNumber = linesNode.get(props.getProperty(MUNIT_LINE_NUMBER)).asInt();
							boolean covered = linesNode.get(props.getProperty(MUNIT_COVERED)).asBoolean();

							if (covered) {
								counter.getCoveredLines().add(lineNumber);
							} else {
								counter.getNotcoveredLines().add(lineNumber);
							}
						}
					}
				}
				String[] fileParts;
				if (name.contains(File.separator))
					fileParts = name.split(File.separator);
				else if (name.contains("/") && "\\".equals(File.separator))
					fileParts = name.split("/");
				else
					fileParts = new String[] { name };
				coverageMap.put(fileParts[fileParts.length - 1], counter);
				if (logger.isDebugEnabled()) {
					logger.debug("name :" + node.get("name") + " : coverage:" + node.get("coverage"));
				}
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return coverageMap;

	}

	private void saveCoverage(Map<String, FlowCoverageCounter> coverageMap, String fileName, SensorContext context,
			InputFile file) {

		FlowCoverageCounter coverage = coverageMap.get(fileName);
		if (coverage != null) {
			NewCoverage newCoverage = context.newCoverage().onFile(file);

			if (coverage.hasLineNumbers()) {
				for (Iterator<Integer> iterator = coverage.getCoveredLines().iterator(); iterator.hasNext();) {
					Integer lineNumber = iterator.next();
					newCoverage.lineHits(lineNumber, 1);
				}
				for (Iterator<Integer> iterator = coverage.getNotcoveredLines().iterator(); iterator.hasNext();) {
					Integer lineNumber = iterator.next();
					newCoverage.lineHits(lineNumber, 0);
				}
				newCoverage.save();
			} else {
				int processors = coverage.getProcessors();
				int covered = coverage.getCoveredProcessors();

				for (int i = 0; i < processors; i++) {
					newCoverage.lineHits(i + 1, covered > 0 ? 1 : 0);
					covered--;
				}

				newCoverage.save();
			}
		}
	}

}
