package com.mulesoft.services.tools.sonarqube.sensor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom2.input.SAXBuilder;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.mulesoft.services.tools.sonarqube.filter.MuleFilePredicate;
import com.mulesoft.services.tools.sonarqube.language.MuleLanguage;
import com.mulesoft.services.tools.sonarqube.rule.MuleRulesDefinition;

/**
 * Mule Sensor Iterates over all mule files and applies the corresponding rules
 * 
 * @author franco.perez
 *
 */
public class MuleSensor implements Sensor {

	private final Logger logger = Loggers.get(MuleSensor.class);

	SAXBuilder saxBuilder = new SAXBuilder();

	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.onlyOnLanguage(MuleLanguage.LANGUAGE_KEY);
		descriptor.createIssuesForRuleRepositories(MuleRulesDefinition.MULE3_REPOSITORY_KEY,
				MuleRulesDefinition.MULE4_REPOSITORY_KEY);
	}

	@Override
	public void execute(SensorContext context) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing Mule Sensor");
		}

		FileSystem fs = context.fileSystem();

		FilePredicates p = fs.predicates();
		Map<RuleKey, List<NewIssue>> issues = new HashMap<RuleKey, List<NewIssue>>();
		fs.inputFiles(p.and(p.hasLanguage(MuleLanguage.LANGUAGE_KEY), new MuleFilePredicate(new MuleLanguage(context.config()).getFileSuffixes())))
				.forEach(new SonarRuleConsumer(getLanguage(context), context, issues));

		// Iterate and save all the issues
		for (Iterator<List<NewIssue>> newIssueListIterator = issues.values().iterator(); newIssueListIterator
				.hasNext();) {
			List<NewIssue> newIssueList = newIssueListIterator.next();
			for (Iterator<NewIssue> newIssueIterator = newIssueList.iterator(); newIssueIterator.hasNext();) {
				NewIssue newIssue = newIssueIterator.next();
				newIssue.save();
			}
		}
	}

	public static String getLanguage(SensorContext context) {
		boolean mule4 = context.activeRules().findByRepository(MuleRulesDefinition.MULE3_REPOSITORY_KEY).isEmpty();
		return mule4 ? MuleLanguage.LANGUAGE_MULE4_KEY : MuleLanguage.LANGUAGE_MULE3_KEY;
	}
}
