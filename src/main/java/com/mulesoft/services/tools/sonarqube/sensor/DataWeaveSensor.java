package com.mulesoft.services.tools.sonarqube.sensor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.mulesoft.services.tools.sonarqube.dataweave.DataWeaveCommentedOutCodeDetector;
import com.mulesoft.services.tools.sonarqube.dataweave.DataWeaveTextUtils;
import com.mulesoft.services.tools.sonarqube.filter.DataWeaveFilePredicate;
import com.mulesoft.services.tools.sonarqube.rule.DataWeaveRulesDefinition;

/**
 * SonarQube sensor that scans DataWeave (DWL) files.
 *
 * <p>Rules are declared in {@code rules-dataweave.xml} (or an external override at
 * {@code $SONARQUBE_HOME/extensions/plugins/rules-dataweave.xml}) and implemented here.
 *
 * <p>Note: DataWeave files are indexed under the Mule language key for display, but their rules must be
 * executed by this sensor (not by the XML/XPath-based Mule sensor).
 */
public class DataWeaveSensor implements Sensor {

	private static final Logger logger = Loggers.get(DataWeaveSensor.class);

	/**
	 * Suffixes scanned by the DataWeave sensor.
	 */
	public static final String DATAWEAVE_FILE_SUFFIXES_KEY = "sonar.mule.dataweave.file.suffixes";
	public static final String DATAWEAVE_FILE_SUFFIXES_DEFAULT_VALUE = ".dwl";

	/**
	 * Fallback threshold used only if the rule parameter cannot be resolved.
	 *
	 * <p>The default threshold is loaded from {@code rules-dataweave.xml} into the rule parameter
	 * {@link com.mulesoft.services.tools.sonarqube.rule.DataWeaveRulesDefinition#TOO_LARGE_FILE_MAX_LINES_PARAM}.
	 */
	private static final int DEFAULT_TOO_LARGE_FILE_MAX_LINES = 5000;

	private final DataWeaveCommentedOutCodeDetector detector = new DataWeaveCommentedOutCodeDetector();

	/**
	 * Declares which rule repositories this sensor can raise issues for.
	 */
	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.createIssuesForRuleRepositories(DataWeaveRulesDefinition.REPOSITORY_KEY);
	}

	/**
	 * Executes DataWeave analysis by scanning DWL files and raising issues for active rules.
	 *
	 * <p>The sensor short-circuits when there are no active rules in the DataWeave repository.
	 */
	@Override
	public void execute(SensorContext context) {
		Collection<ActiveRule> activeRules = context.activeRules().findByRepository(DataWeaveRulesDefinition.REPOSITORY_KEY);
		if (activeRules.isEmpty()) {
			return;
		}

		Set<String> activeRuleKeys = new HashSet<>();
		Integer tooLargeFileMaxLines = null;
		for (ActiveRule r : activeRules) {
			activeRuleKeys.add(r.ruleKey().rule());
			if (DataWeaveRulesDefinition.TOO_LARGE_FILE_RULE_KEY.equals(r.ruleKey().rule())) {
				tooLargeFileMaxLines = parsePositiveIntOrNull(
						r.param(DataWeaveRulesDefinition.TOO_LARGE_FILE_MAX_LINES_PARAM));
			}
		}
		if (tooLargeFileMaxLines == null) {
			tooLargeFileMaxLines = DEFAULT_TOO_LARGE_FILE_MAX_LINES;
		}

		FileSystem fs = context.fileSystem();
		String[] suffixes = context.config().getStringArray(DATAWEAVE_FILE_SUFFIXES_KEY);
		if (suffixes.length == 0) {
			suffixes = DATAWEAVE_FILE_SUFFIXES_DEFAULT_VALUE.split(",");
		}

		Set<String> keys = java.util.Collections.unmodifiableSet(activeRuleKeys);
		int maxLines = tooLargeFileMaxLines;
		fs.inputFiles(new DataWeaveFilePredicate(suffixes)).forEach(inputFile -> scanFile(context, inputFile, keys, maxLines));
	}

	/**
	 * Scans a single DataWeave file and persists any resulting issues.
	 *
	 * @param context SonarQube sensor context
	 * @param inputFile DataWeave file being analyzed
	 * @param activeRuleKeys set of active DataWeave rule keys
	 * @param tooLargeFileMaxLines configured threshold for the "too-large-file" rule
	 */
	private void scanFile(SensorContext context, InputFile inputFile, Set<String> activeRuleKeys, int tooLargeFileMaxLines) {
		String content;
		try {
			content = readToString(inputFile);
		} catch (IOException e) {
			logger.warn("Failed to read DataWeave file: {}", inputFile.uri(), e);
			return;
		}

		if (activeRuleKeys.contains(DataWeaveRulesDefinition.COMMENTED_OUT_CODE_RULE_KEY)) {
			List<DataWeaveCommentedOutCodeDetector.Finding> findings = detector.findFindings(content);
			if (!findings.isEmpty()) {
				RuleKey ruleKey = RuleKey.of(DataWeaveRulesDefinition.REPOSITORY_KEY,
						DataWeaveRulesDefinition.COMMENTED_OUT_CODE_RULE_KEY);

				for (DataWeaveCommentedOutCodeDetector.Finding finding : findings) {
					NewIssue issue = context.newIssue().forRule(ruleKey);
					NewIssueLocation location = issue.newLocation()
							.on(inputFile)
							.at(inputFile.selectLine(finding.line))
							.message("Remove commented-out DataWeave code.");
					issue.at(location);
					issue.save();
				}
			}
		}

		if (activeRuleKeys.contains(DataWeaveRulesDefinition.TOO_LARGE_FILE_RULE_KEY)) {
			int lines = DataWeaveTextUtils.countLines(content);
			if (lines > tooLargeFileMaxLines) {
				RuleKey ruleKey = RuleKey.of(DataWeaveRulesDefinition.REPOSITORY_KEY,
						DataWeaveRulesDefinition.TOO_LARGE_FILE_RULE_KEY);
				NewIssue issue = context.newIssue().forRule(ruleKey);
				NewIssueLocation location = issue.newLocation()
						.on(inputFile)
						.at(inputFile.selectLine(1))
						.message("DataWeave file has " + lines + " lines (max " + tooLargeFileMaxLines
								+ "). Consider splitting/refactoring.");
				issue.at(location);
				issue.save();
			}
		}
	}

	/**
	 * Parses a strictly positive integer from a string.
	 *
	 * @param raw raw string value (may be null/blank/non-numeric)
	 * @return parsed integer when valid and {@code > 0}; otherwise {@code null}
	 */
	private static Integer parsePositiveIntOrNull(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return null;
		}
		try {
			int v = Integer.parseInt(t);
			return v > 0 ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Reads the full {@link InputFile} content into a string using Sonar's detected charset.
	 *
	 * @param inputFile file to read
	 * @return file contents as a string
	 * @throws IOException if the underlying stream cannot be read
	 */
	private static String readToString(InputFile inputFile) throws IOException {
		Charset charset = inputFile.charset();
		try (InputStream in = inputFile.inputStream()) {
			byte[] bytes = readAllBytes(in);
			return new String(bytes, charset);
		}
	}

	/**
	 * Reads all bytes from a stream.
	 *
	 * <p>Kept local to avoid depending on Java 9+ {@code InputStream#readAllBytes()}.
	 */
	private static byte[] readAllBytes(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		int r;
		while ((r = in.read(buf)) >= 0) {
			out.write(buf, 0, r);
		}
		return out.toByteArray();
	}
}

