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

/**
 * Imports MUnit coverage results and reports them as SonarQube coverage on Mule configuration files.
 *
 * <p>The sensor supports different report locations/formats for Mule 3 and Mule 4:
 * <ul>
 *   <li>Mule 3: {@code target/munit-reports/coverage-json/report.json}</li>
 *   <li>Mule 4: {@code target/site/munit/coverage/munit-coverage.json}</li>
 * </ul>
 *
 * <p>The JSON schema differs between versions; field names are externalized via {@link MuleProperties}
 * so the same parsing logic can be used for both.
 *
 * <p>If the report is missing, the sensor records 0 hits for every line in each Mule config file so
 * coverage-based quality gates can fail deterministically.
 *
 * @version 1.1.0
 * @since 1.1.0
 */
public class CoverageSensor implements Sensor {

	private final Logger logger = Loggers.get(CoverageSensor.class);
	public static final String MUNIT_COVERAGE_JSON_REPORT_PATHS_KEY = "sonar.coverage.mulesoft.jsonReportPaths";
	public static final String MUNIT_COVERAGE_JSON_REPORT_PATH_KEY = "sonar.coverage.mulesoft.jsonReportPath";
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

	/**
	 * Describes this sensor for SonarQube and restricts it to Mule language projects.
	 *
	 * @param descriptor the sensor descriptor
	 */
	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.name("Compute the coverage of the applications");
		// Do not restrict to Mule "language" detection.
		// MuleLanguage intentionally defaults to no suffixes for language detection to avoid conflicts with the XML analyzer,
		// but this sensor can still safely scope itself by scanning Mule XML files via MuleFilePredicate.
	}

	/**
	 * Loads the MUnit coverage report (if present) and saves coverage onto each Mule configuration file.
	 *
	 * @param context sensor execution context
	 */
	@Override
	public void execute(SensorContext context) {
		File munitJsonReport = resolveCoverageReport(context);
		if (munitJsonReport != null && munitJsonReport.exists()) {
			Map<String, FlowCoverageCounter> coverage = loadResults(
					MuleProperties.getProperties(MuleSensor.getLanguage(context)), munitJsonReport);
			FileSystem fs = context.fileSystem();
			// Only ConfigurationFiles
			String[] scanSuffixes = context.config().getStringArray(MuleLanguage.SCAN_FILE_SUFFIXES_KEY);
			if (scanSuffixes.length == 0) {
				scanSuffixes = MuleLanguage.SCAN_FILE_SUFFIXES_DEFAULT_VALUE.split(",");
			}
			Iterable<InputFile> files = fs.inputFiles(new MuleFilePredicate(scanSuffixes));
			for (InputFile file : files) {
				saveCoverage(coverage, file.filename(), context, file);
			}
		} else {
			if (context.config().get(MUNIT_COVERAGE_JSON_REPORT_PATH_KEY).isPresent()
					|| context.config().getStringArray(MUNIT_COVERAGE_JSON_REPORT_PATHS_KEY).length > 0) {
				logger.warn("No MUnit coverage JSON report found using configured properties '{}'/'{}'. Falling back to 0% coverage.",
						MUNIT_COVERAGE_JSON_REPORT_PATHS_KEY, MUNIT_COVERAGE_JSON_REPORT_PATH_KEY);
			}
			// Treat missing coverage report as 0% coverage so Quality Gates can fail.
			FileSystem fs = context.fileSystem();
			String[] scanSuffixes = context.config().getStringArray(MuleLanguage.SCAN_FILE_SUFFIXES_KEY);
			if (scanSuffixes.length == 0) {
				scanSuffixes = MuleLanguage.SCAN_FILE_SUFFIXES_DEFAULT_VALUE.split(",");
			}
			Iterable<InputFile> files = fs.inputFiles(new MuleFilePredicate(scanSuffixes));
			for (InputFile file : files) {
				int lines = file.lines();
				if (lines <= 0) {
					continue;
				}
				NewCoverage newCoverage = context.newCoverage().onFile(file);
				for (int i = 1; i <= lines; i++) {
					newCoverage.lineHits(i, 0);
				}
				newCoverage.save();
			}
		}
	}

	private File resolveCoverageReport(SensorContext context) {
		FileSystem fs = context.fileSystem();
		String[] configured = context.config().getStringArray(MUNIT_COVERAGE_JSON_REPORT_PATHS_KEY);
		if (configured.length == 0) {
			String single = context.config().get(MUNIT_COVERAGE_JSON_REPORT_PATH_KEY).orElse(null);
			if (single != null && !single.trim().isEmpty()) {
				configured = new String[] { single };
			}
		}

		for (String rawPath : configured) {
			if (rawPath == null) {
				continue;
			}
			String p = rawPath.trim();
			if (p.isEmpty()) {
				continue;
			}
			// Some scanners pass multi-values as a single comma-separated string.
			if (p.contains(",")) {
				String[] parts = p.split(",");
				for (String part : parts) {
					File f = toFile(fs.baseDir(), part);
					if (f != null && f.exists()) {
						return f;
					}
				}
				continue;
			}

			File f = toFile(fs.baseDir(), p);
			if (f != null && f.exists()) {
				return f;
			}
		}

		String defaultRelative = MuleSensor.getLanguage(context).equals(MuleLanguage.LANGUAGE_MULE4_KEY) ? MUNIT_REPORT_MULE_4
				: MUNIT_REPORT_MULE_3;
		return new File(fs.baseDir() + defaultRelative);
	}

	private static File toFile(File baseDir, String rawPath) {
		if (rawPath == null) {
			return null;
		}
		String p = rawPath.trim();
		if (p.isEmpty()) {
			return null;
		}
		if (p.startsWith("file:")) {
			p = p.substring("file:".length());
		}
		File f = new File(p);
		if (!f.isAbsolute()) {
			f = new File(baseDir, p);
		}
		return f;
	}

	/**
	 * Parses a MUnit JSON report file and aggregates coverage information per configuration file.
	 *
	 * @param props language-specific properties mapping JSON field names
	 * @param munitJsonReport report file to read
	 * @return a map keyed by file name/path to the aggregated coverage counter (may be null on failure)
	 */
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
				String normalizedName = name.replace("\\", "/");
				String[] fileParts = normalizedName.split("/");
				String baseName = fileParts[fileParts.length - 1];
				// Store multiple keys to improve matching across environments and report formats.
				coverageMap.put(baseName, counter);
				coverageMap.put(normalizedName, counter);
				if (logger.isDebugEnabled()) {
					logger.debug("name :" + node.get("name") + " : coverage:" + node.get("coverage"));
				}
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return coverageMap;

	}

	/**
	 * Saves coverage to SonarQube for the given file when a matching coverage entry is available.
	 *
	 * <p>When the report contains explicit line numbers, those are used. Otherwise the sensor maps
	 * covered/uncovered message processor counts onto synthetic line numbers starting at 1.
	 *
	 * @param coverageMap parsed coverage data keyed by file name/path
	 * @param fileName file name used as a lookup key
	 * @param context sensor context used to create and save coverage
	 * @param file the SonarQube input file receiving coverage
	 */
	private void saveCoverage(Map<String, FlowCoverageCounter> coverageMap, String fileName, SensorContext context,
			InputFile file) {

		FlowCoverageCounter coverage = coverageMap.get(fileName);
		if (coverage == null) {
			coverage = coverageMap.get(file.relativePath().replace("\\", "/"));
		}
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
