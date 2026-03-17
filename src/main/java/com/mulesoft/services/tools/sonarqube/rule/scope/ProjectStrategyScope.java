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
 * Validates project-level rules using SonarQube project metadata (projectKey/projectName).
 *
 * The rule's "xpath-expression" parameter is treated as a Java regex to validate the value.
 */
public class ProjectStrategyScope implements ScopeStrategy {
	private final Logger logger = Loggers.get(ProjectStrategyScope.class);

	private static final String SONAR_PROJECT_KEY = "sonar.projectKey";
	private static final String SONAR_PROJECT_NAME = "sonar.projectName";

	// Keep state per analysis execution (scope instance is reused within a scan).
	private final Set<String> valids = ConcurrentHashMap.newKeySet();

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

	private static String getProjectIdentifier(SensorContext context) {
		Optional<String> name = context.config().get(SONAR_PROJECT_NAME);
		if (name.isPresent() && !name.get().trim().isEmpty()) {
			return name.get().trim();
		}
		return context.config().get(SONAR_PROJECT_KEY).orElse("").trim();
	}

	private static boolean matches(String value, String regex) {
		if (value == null) {
			return false;
		}
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		return p.matcher(value).matches();
	}

	private static String safeTrim(String s) {
		return s == null ? "" : s.trim();
	}

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

