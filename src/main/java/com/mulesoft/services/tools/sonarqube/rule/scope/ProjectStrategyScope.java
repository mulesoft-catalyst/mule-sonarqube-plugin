package com.mulesoft.services.tools.sonarqube.rule.scope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.mulesoft.services.tools.sonarqube.rule.MuleRulesDefinition;
import com.mulesoft.services.xpath.XPathProcessor;

/**
 * Applies rules that are scoped to the “project” level using SonarQube project metadata.
 *
 * <p>Project-scoped rules do not evaluate XML. Instead, the rule's {@code xpath-expression}
 * parameter is treated as a Java regex used to validate either {@code sonar.projectName} or
 * {@code sonar.projectKey} (in that order).
 *
 * <p>To avoid flooding results, this strategy tracks which project-level rules have been satisfied
 * and ensures at most one issue is reported per failing rule.
 *
 * @version 1.1.0
 * @since 1.1.0
 */
public class ProjectStrategyScope implements ScopeStrategy {
	private final Logger logger = Loggers.get(ProjectStrategyScope.class);

	private static final String SONAR_PROJECT_KEY = "sonar.projectKey";
	private static final String SONAR_PROJECT_NAME = "sonar.projectName";

	// Keep state per analysis execution (scope instance is reused within a scan).
	private final Set<String> valids = ConcurrentHashMap.newKeySet();

	/**
	 * Validates the configured project identifier against the rule regex and records a single issue
	 * when it does not match.
	 *
	 * @param xpathValidator unused for project scope (kept for interface consistency)
	 * @param issues mutable issue collection keyed by rule
	 * @param context active SonarQube sensor context
	 * @param t any input file used as an anchor for the issue location
	 * @param rule the active rule being applied
	 */
	@Override
	public void validate(XPathProcessor xpathValidator, Map<RuleKey, List<NewIssue>> issues, SensorContext context,
			InputFile t, ActiveRule rule) {
		String ruleId = rule.ruleKey().toString();

		String patternText = safeTrim(rule.param(MuleRulesDefinition.PARAMS.XPATH));
		if (patternText.isEmpty()) {
			logger.debug("Project rule has empty pattern for {}", rule.ruleKey());
			return;
		}

		String projectValue = getProjectIdentifier(context);
		boolean valid = matches(projectValue, patternText);
		logger.debug("Project naming validation: valid={} value={} rule={}", valid, projectValue, rule.ruleKey());

		if (!valid && !valids.contains(ruleId) && !issues.containsKey(rule.ruleKey())) {
			NewIssue newIssue = context.newIssue().forRule(rule.ruleKey());
			NewIssueLocation primaryLocation = newIssue.newLocation().on(t);
			if (t.lines() > 0) {
				primaryLocation = primaryLocation.at(t.newRange(1, 0, 1, 0));
			}
			newIssue.at(primaryLocation);
			addIssue(issues, rule, newIssue);
		} else if (valid && !valids.contains(ruleId)) {
			valids.add(ruleId);
			issues.remove(rule.ruleKey());
		}
	}

	/**
	 * Returns the best available project identifier for validation.
	 *
	 * @param context sensor context providing access to analysis configuration
	 * @return {@code sonar.projectName} when present; otherwise {@code sonar.projectKey} (may be empty)
	 */
	private static String getProjectIdentifier(SensorContext context) {
		Optional<String> name = context.config().get(SONAR_PROJECT_NAME);
		if (name.isPresent() && !name.get().trim().isEmpty()) {
			return name.get().trim();
		}
		return context.config().get(SONAR_PROJECT_KEY).orElse("").trim();
	}

	/**
	 * Performs a case-insensitive full match of the value against the provided regex.
	 *
	 * @param value value to validate
	 * @param regex regex pattern text
	 * @return {@code true} when {@code value} matches; otherwise {@code false}
	 */
	private static boolean matches(String value, String regex) {
		if (value == null) {
			return false;
		}
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		return p.matcher(value).matches();
	}

	/**
	 * Trims the input string, returning an empty string when the value is null.
	 *
	 * @param s the input string, may be null
	 * @return the trimmed string, or {@code ""} when {@code s} is null
	 */
	private static String safeTrim(String s) {
		return s == null ? "" : s.trim();
	}

	/**
	 * Adds the issue to the per-rule issue map.
	 *
	 * @param issues issue map to update
	 * @param rule the rule key under which the issue should be stored
	 * @param issue the issue to add
	 */
	private static void addIssue(Map<RuleKey, List<NewIssue>> issues, ActiveRule rule, NewIssue issue) {
		if (issues.containsKey(rule.ruleKey())) {
			issues.get(rule.ruleKey()).add(issue);
		} else {
			List<NewIssue> issuesList = new ArrayList<>();
			issuesList.add(issue);
			issues.put(rule.ruleKey(), issuesList);
		}
	}
}

