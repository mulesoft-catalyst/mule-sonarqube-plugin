package com.mulesoft.services.tools.sonarqube.rule.scope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.sonarsource.analyzer.commons.xml.XmlTextRange;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mulesoft.services.tools.sonarqube.rule.MuleRulesDefinition;
import com.mulesoft.services.xpath.XPathProcessor;

/**
 * Applies rules that are scoped to individual XML nodes.
 *
 * <p>Node-scoped rules return a node set for their XPath expression. An issue is created for each
 * matched node and anchored as precisely as possible (element name or attribute value) using
 * {@link XmlFile} location metadata.
 *
 * <p>This scope is only supported for rules with {@code plugin-version=1.1}.
 *
 * @version 1.1.0
 * @since 1.1.0
 */
public class NodeStrategyScope implements ScopeStrategy {
	private final Logger logger = Loggers.get(NodeStrategyScope.class);

	/**
	 * Evaluates the rule XPath as a node set and records an issue for each returned node.
	 *
	 * @param xpathValidator XPath processor configured with namespaces
	 * @param issues mutable issue collection keyed by rule
	 * @param context active SonarQube sensor context
	 * @param t the file being validated
	 * @param rule the active rule being applied
	 * @throws IllegalArgumentException when the rule is not marked as {@code plugin-version=1.1}
	 */
	@Override
	public void validate(XPathProcessor xpathValidator, Map<RuleKey, List<NewIssue>> issues, SensorContext context,
			InputFile t, ActiveRule rule) {
		String pluginVersion = safeTrim(rule.param(MuleRulesDefinition.PARAMS.PLUGIN_VERSION));
		if (!"1.1".equalsIgnoreCase(pluginVersion)) {
			throw new IllegalArgumentException("Node scope is only supported for rules with pluginVersion=1.1");
		}

		try {
			XmlFile xmlFile = XmlFile.create(t);
			Node root = xmlFile.getDocument().getFirstChild();
			String subjectsXPath = safeTrim(rule.param(MuleRulesDefinition.PARAMS.XPATH));
			NodeList nodeList = (NodeList) xpathValidator.processXPathAsNodeSet(subjectsXPath, root,
					XPathConstants.NODESET);

			for (Node node : XmlFile.asList(nodeList)) {
				NewIssue newIssue = context.newIssue().forRule(rule.ruleKey());

				XmlTextRange textRange = toTextRange(node);
				NewIssueLocation primaryLocation = newIssue.newLocation().on(t);
				if (textRange != null) {
					primaryLocation = primaryLocation.at(t.newRange(textRange.getStartLine(), textRange.getStartColumn(),
							textRange.getEndLine(), textRange.getEndColumn()));
				} else if (t.lines() > 0) {
					org.sonar.api.batch.fs.TextRange range = IssueLocations.primaryRange(t);
					if (range != null) {
						primaryLocation = primaryLocation.at(range);
					}
				}
				newIssue.at(primaryLocation);
				addIssue(issues, rule, newIssue);
			}
		} catch (IOException | XPathExpressionException e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Converts a W3C DOM node into a Sonar XML text range (when supported).
	 *
	 * @param node DOM node returned by XPath evaluation
	 * @return a text range for anchoring an issue, or null when the node type is not supported
	 */
	private static XmlTextRange toTextRange(Node node) {
		if (node instanceof org.w3c.dom.Element) {
			return XmlFile.nameLocation((org.w3c.dom.Element) node);
		}
		if (node instanceof Attr) {
			return XmlFile.attributeValueLocation((Attr) node);
		}
		return null;
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
			List<NewIssue> issuesList = new ArrayList<>();
			issuesList.add(issue);
			issues.put(rule.ruleKey(), issuesList);
		}
	}
}

