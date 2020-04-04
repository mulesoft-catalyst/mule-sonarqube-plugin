package com.mulesoft.services.tools.sonarqube.rule.scope;

import java.util.List;
import java.util.Map;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;

import com.mulesoft.services.xpath.XPathProcessor;

public interface ScopeStrategy {

	String FILE = "file";
	String APPLICATION = "application";

	public void validate(XPathProcessor xpathValidator, Map<RuleKey, List<NewIssue>> issues, SensorContext context, InputFile t, ActiveRule rule);
}
