package com.mulesoft.services.tools.sonarqube.rule.scope;

import java.util.List;
import java.util.Map;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;

import com.mulesoft.services.xpath.XPathProcessor;

/**
 * Strategy interface for applying a rule at a particular “scope”.
 *
 * <p>The same XPath rule may be evaluated differently depending on whether it should apply
 * to an entire file, a specific node, an application, or the whole project. Implementations
 * are responsible for evaluating rule XPath expressions and recording {@link NewIssue}s.
 *
 * @version 1.1.0
 * @since 1.1.0
 */
public interface ScopeStrategy {

	String FILE = "file";
	String APPLICATION = "application";
	String NODE = "node";
	String PROJECT = "project";

	/**
	 * Validates a file against a rule, recording any issues into the provided issue map.
	 *
	 * @param xpathValidator XPath processor configured with namespaces
	 * @param issues mutable issue collection keyed by rule
	 * @param context active SonarQube sensor context
	 * @param t the file being validated
	 * @param rule the active rule being applied
	 */
	public void validate(XPathProcessor xpathValidator, Map<RuleKey, List<NewIssue>> issues, SensorContext context,
			InputFile t, ActiveRule rule);
}
