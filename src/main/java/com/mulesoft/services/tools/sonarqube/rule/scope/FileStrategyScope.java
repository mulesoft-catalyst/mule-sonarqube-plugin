package com.mulesoft.services.tools.sonarqube.rule.scope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.mulesoft.services.tools.sonarqube.rule.MuleRulesDefinition;
import com.mulesoft.services.xpath.XPathProcessor;

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
import org.sonarsource.analyzer.commons.xml.XmlTextRange;
import org.w3c.dom.Node;

public class FileStrategyScope implements ScopeStrategy {
	private final Logger logger = Loggers.get(FileStrategyScope.class);
	SAXBuilder saxBuilder = new SAXBuilder();

	@Override
	public void validate(XPathProcessor xpathValidator, Map<RuleKey, List<NewIssue>> issues, SensorContext context,
			InputFile t, ActiveRule rule) {
		if (rule.param(MuleRulesDefinition.PARAMS.PLUGIN_VERSION).trim().equalsIgnoreCase("1.1")) {
			try {
				logger.info("Rule v1.1, File scope, File: " + t.filename() + " Rule:" + rule.ruleKey());
				XmlFile xmlFile = XmlFile.create(t);
				Node root = xmlFile.getDocument().getFirstChild();

				String xpath = rule.param(MuleRulesDefinition.PARAMS.XPATH).trim();
				Boolean valid = (Boolean)xpathValidator.processXPathAsNodeSet(xpath, root, XPathConstants.BOOLEAN);

				if (!valid) {
					NewIssue newIssue = context.newIssue().forRule(rule.ruleKey());
					NewIssueLocation primaryLocation = newIssue.newLocation().on(t);
					newIssue.at(primaryLocation);
					addIssue(issues, rule, newIssue);
				}
			} catch (IOException | XPathExpressionException e ) {
				logger.error(e.getMessage(), e);
			}
		} else {
			try {
				logger.info("Rule v1.0: File: " + t.filename() + " Rule:" + rule.ruleKey());
				Document document = saxBuilder.build(t.inputStream());
				Element rootElement = document.getRootElement();	
				boolean valid = xpathValidator.processXPath(rule.param(MuleRulesDefinition.PARAMS.XPATH).trim(),
						rootElement, Boolean.class);
				logger.info("Validation Result: " + valid + " : File: " + t.filename() + " :Rule:" + rule.ruleKey());
				if (!valid) {
					
					XmlTextRange textRange;
					try {
						// see org.sonarsource.analyzer.commons.xml.checks.SonarXmlCheck.reportIssue(Node, String)
						SonarXmlCheckHelper tempSonarXmlCheckHelper = new SonarXmlCheckHelper();
						XmlFile xmlFile = XmlFile.create(t);
						tempSonarXmlCheckHelper.scanFile(context, rule.ruleKey(), xmlFile); // just to fill the properties
						String locationFindingXPath = rule.param(MuleRulesDefinition.PARAMS.XPATH_LOCATION_HINT).trim();
						if (locationFindingXPath == null || locationFindingXPath.length()==0) {
							logger.info("No locationFindingXPath:params="+rule.params());
							textRange = null;
						} else {
							Node locationElement = (Node)XPathFactory.newInstance().newXPath().compile(locationFindingXPath).evaluate(xmlFile.getDocument().getFirstChild(),  XPathConstants.NODE);
							if (locationElement == null) {
								textRange= null;
								logger.warn("Did not find node for "+locationFindingXPath);
							} else {
								textRange = XmlFile.nodeLocation(locationElement);
								logger.info("Found textRange="+textRange);
							}
						}
					} catch (RuntimeException | XPathExpressionException e) {
						logger.error("Ignore",e);
						textRange= null;
					}

					NewIssue newIssue = context.newIssue().forRule(rule.ruleKey());
					NewIssueLocation primaryLocation;
					if (textRange == null) {
						primaryLocation = newIssue.newLocation().on(t);
					} else {
						primaryLocation = newIssue.newLocation().on(t) .at(t.newRange(
								textRange.getStartLine(),
								textRange.getStartColumn(),
								textRange.getEndLine(),
								textRange.getEndColumn()));
					}
					
					newIssue.at(primaryLocation);
					addIssue(issues, rule, newIssue);
				}
			} catch (JDOMException | IOException e ) {
				logger.error(e.getMessage(), e);
			}
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
