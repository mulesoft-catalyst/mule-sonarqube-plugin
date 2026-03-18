package com.mulesoft.services.tools.sonarqube.rule.scope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.w3c.dom.Node;

import com.mulesoft.services.tools.sonarqube.rule.MuleRulesDefinition;
import com.mulesoft.services.tools.sonarqube.xml.SecureSaxBuilder;
import com.mulesoft.services.xpath.XPathProcessor;

/**
 * Applies rules that are scoped to the “application” level.
 *
 * <p>Application-scoped rules are evaluated once per application (across files) and are meant to
 * express constraints like “the application must have X configured”. To avoid flooding results,
 * this strategy tracks rule ids that have already been satisfied and ensures at most one issue is
 * reported for a failing rule.
 *
 * @version 1.1.0
 * @since 1.1.0
 */
public class ApplicationStrategyScope implements ScopeStrategy {
	private final Logger logger = Loggers.get(ApplicationStrategyScope.class);
	SAXBuilder saxBuilder = SecureSaxBuilder.create();
	Set<String> valids = new HashSet<String>();

	/**
	 * Validates the given file for an application-scoped rule and records a single issue when the
	 * rule is not satisfied.
	 *
	 * <p>For plugin version {@code 1.1}, evaluation is performed using {@link XmlFile} and W3C DOM
	 * nodes; earlier behavior uses JDOM parsing and evaluates against the JDOM root element.
	 *
	 * @param xpathValidator XPath processor configured with namespaces
	 * @param issues mutable issue collection keyed by rule
	 * @param context active SonarQube sensor context
	 * @param t the file being validated
	 * @param rule the active rule being applied
	 */
	@Override
	public void validate(XPathProcessor xpathValidator, Map<RuleKey, List<NewIssue>> issues, SensorContext context,
			InputFile t, ActiveRule rule) {
		try {
			String ruleId = rule.ruleKey().toString();
			String ruleXPath = safeTrim(rule.param(MuleRulesDefinition.PARAMS.XPATH));
			String pluginVersion = safeTrim(rule.param(MuleRulesDefinition.PARAMS.PLUGIN_VERSION));
			boolean valid;
			if ("1.1".equalsIgnoreCase(pluginVersion)) {
				XmlFile xmlFile = XmlFile.create(t);
				Node root = xmlFile.getDocument().getFirstChild();
				Boolean v = (Boolean) xpathValidator.processXPathAsNodeSet(ruleXPath, root, XPathConstants.BOOLEAN);
				valid = Boolean.TRUE.equals(v);
			} else {
				Document document = saxBuilder.build(t.inputStream());
				Element rootElement = document.getRootElement();
				Boolean validResult = xpathValidator.processXPath(ruleXPath, rootElement, Boolean.class);
				valid = Boolean.TRUE.equals(validResult);
			}
			logger.debug("Validation Result: {} : File: {} :Rule:{} internalKey={}", valid, t.filename(), rule.ruleKey(),
					rule.internalKey());
			if (!valid && !valids.contains(ruleId) && !issues.containsKey(rule.ruleKey())) {
				NewIssue newIssue = context.newIssue().forRule(rule.ruleKey());
				NewIssueLocation primaryLocation = newIssue.newLocation().on(t);
				if (t.lines() > 0) {
					org.sonar.api.batch.fs.TextRange range = IssueLocations.primaryRange(t);
					if (range != null) {
						primaryLocation = primaryLocation.at(range);
					}
				}
				newIssue.at(primaryLocation);
				addIssue(issues, rule, newIssue);
			} else {
				if (valid && !valids.contains(ruleId)) {
					valids.add(ruleId);
					issues.remove(rule.ruleKey());
				}
			}

		} catch (JDOMException | IOException | XPathExpressionException e) {
			logger.error(e.getMessage(), e);
		}
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
	private void addIssue(Map<RuleKey, List<NewIssue>> issues, ActiveRule rule, NewIssue issue) {

		if (issues.containsKey(rule.ruleKey())) {
			issues.get(rule.ruleKey()).add(issue);
		} else {
			List<NewIssue> issuesList = new ArrayList<NewIssue>();
			issuesList.add(issue);
			issues.put(rule.ruleKey(), issuesList);
		}

	}

}
