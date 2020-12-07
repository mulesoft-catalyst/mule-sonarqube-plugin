package com.mulesoft.services.tools.sonarqube.rule.scope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import com.mulesoft.services.tools.sonarqube.rule.MuleRulesDefinition;
import com.mulesoft.services.xpath.XPathProcessor;

public class ApplicationStrategyScope implements ScopeStrategy {
	private final Logger logger = Loggers.get(ApplicationStrategyScope.class);
	SAXBuilder saxBuilder = new SAXBuilder();
	Set<String> valids = new HashSet<String>();

	@Override
	public void validate(XPathProcessor xpathValidator, Map<RuleKey, List<NewIssue>> issues, SensorContext context,
			InputFile t, ActiveRule rule) {
		try {
			Document document = saxBuilder.build(t.inputStream());
			Element rootElement = document.getRootElement();
			String ruleId = rule.ruleKey().toString();
			boolean valid = xpathValidator.processXPath(rule.param(MuleRulesDefinition.PARAMS.XPATH).trim(),
					rootElement, Boolean.class).booleanValue();
			logger.info("Validation Result: " + valid + " : File: " + t.filename() + " :Rule:" + rule.ruleKey()+" internalKey="+rule.internalKey());
			if (!valid && !valids.contains(ruleId) && !issues.containsKey(rule.ruleKey())) {
				NewIssue newIssue = context.newIssue().forRule(rule.ruleKey());
				NewIssueLocation primaryLocation = newIssue.newLocation().on(t);
				newIssue.at(primaryLocation);
				addIssue(issues, rule, newIssue);
			} else {
				if (valid && !valids.contains(ruleId)) {
					valids.add(ruleId);
					issues.remove(rule.ruleKey());
				}
			}

		} catch (JDOMException | IOException e) {
			logger.error(e.getMessage(), e);
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
