package com.mulesoft.services.tools.sonarqube.rule.scope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import com.mulesoft.services.tools.sonarqube.rule.MuleRulesDefinition;
import com.mulesoft.services.xpath.XPathProcessor;

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
import org.sonarsource.analyzer.commons.xml.XmlTextRange;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeStrategyScope implements ScopeStrategy {
	private final Logger logger = Loggers.get(FileStrategyScope.class);
	SAXBuilder saxBuilder = new SAXBuilder();

	@Override
	public void validate(XPathProcessor xpathValidator, Map<RuleKey, List<NewIssue>> issues, SensorContext context,
			InputFile t, ActiveRule rule) {
		if (rule.param(MuleRulesDefinition.PARAMS.PLUGIN_VERSION).trim().equalsIgnoreCase("1.1")) {
			try {
				logger.info("Rule v1.1, Node scope, File: " + t.filename() + " Rule:" + rule.ruleKey());
				XmlFile xmlFile = XmlFile.create(t);
				Node root = xmlFile.getDocument().getFirstChild();

				String subjectsXPath = rule.param(MuleRulesDefinition.PARAMS.XPATH).trim();
				NodeList nodeList = (NodeList)xpathValidator.processXPathAsNodeSet(subjectsXPath, root, XPathConstants.NODESET);
				for (Node node : XmlFile.asList(nodeList)) {
					NewIssue newIssue = context.newIssue().forRule(rule.ruleKey());
					XmlTextRange textRange;
					if (node instanceof org.w3c.dom.Element) {
						textRange = XmlFile.nameLocation((org.w3c.dom.Element)node);
					} else if (node instanceof Attr) {
						textRange = XmlFile.attributeValueLocation((Attr)node);
					} else {
						textRange = null;
					}
					if (textRange != null) {
						NewIssueLocation primaryLocation = newIssue.newLocation().on(t).at(t.newRange(
							textRange.getStartLine(),
							textRange.getStartColumn(),
							textRange.getEndLine(),
							textRange.getEndColumn()));
						newIssue.at(primaryLocation);
					}
					addIssue(issues, rule, newIssue);
				}
			} catch (IOException | XPathExpressionException e ) {
				logger.error(e.getMessage(), e);
			}
		} else {
            logger.info("Rule v1.0: File: " + t.filename() + " Rule:" + rule.ruleKey());
            throw new RuntimeException("Node scope is only supported for rules with plugin version >= 1.1");
		}
	}

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
