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

public class NodeStrategyScope implements ScopeStrategy {
	private final Logger logger = Loggers.get(NodeStrategyScope.class);

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

	private static XmlTextRange toTextRange(Node node) {
		if (node instanceof org.w3c.dom.Element) {
			return XmlFile.nameLocation((org.w3c.dom.Element) node);
		}
		if (node instanceof Attr) {
			return XmlFile.attributeValueLocation((Attr) node);
		}
		return null;
	}

	private static String safeTrim(String s) {
		return s == null ? "" : s.trim();
	}

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

