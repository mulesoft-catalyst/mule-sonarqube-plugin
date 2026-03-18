package com.mulesoft.services.tools.sonarqube.sensor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom2.input.SAXBuilder;
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
 * SonarQube sensor that scans Mule configuration files and raises issues based on the
 * active Mule rule repository (Mule 3 vs Mule 4).
 *
 * <p>The sensor:
 * <ul>
 *   <li>selects candidate files via {@link MuleFilePredicate}</li>
 *   <li>evaluates rules by delegating to {@link SonarRuleConsumer}</li>
 *   <li>collects generated {@link NewIssue}s and persists them into the analysis context</li>
 * </ul>
 *
 * @author franco.perez
 * @version 1.1.0
 * @since 1.1.0
 * @see MuleRulesDefinition
 */
public class MuleSensor implements Sensor {

	private final Logger logger = Loggers.get(MuleSensor.class);

	SAXBuilder saxBuilder = new SAXBuilder();

	/**
	 * Declares which rule repositories this sensor can create issues for.
	 *
	 * @param descriptor sensor metadata descriptor provided by SonarQube
	 */
	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.createIssuesForRuleRepositories(MuleRulesDefinition.MULE3_REPOSITORY_KEY,
				MuleRulesDefinition.MULE4_REPOSITORY_KEY);
	}

	/**
	 * Executes analysis by scanning Mule files and saving the produced issues.
	 *
	 * <p>Files are selected using {@link MuleLanguage#SCAN_FILE_SUFFIXES_KEY} (defaulting to
	 * {@link MuleLanguage#SCAN_FILE_SUFFIXES_DEFAULT_VALUE}) and then filtered/validated as
	 * Mule configuration files by {@link MuleFilePredicate}.
	 *
	 * @param context SonarQube sensor context used to access filesystem, configuration and rules
	 */
	@Override
	public void execute(SensorContext context) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing Mule Sensor");
		}

		FileSystem fs = context.fileSystem();

		Map<RuleKey, List<NewIssue>> issues = new HashMap<RuleKey, List<NewIssue>>();
		String[] scanSuffixes = context.config().getStringArray(MuleLanguage.SCAN_FILE_SUFFIXES_KEY);
		if (scanSuffixes.length == 0) {
			scanSuffixes = MuleLanguage.SCAN_FILE_SUFFIXES_DEFAULT_VALUE.split(",");
		}
		fs.inputFiles(new MuleFilePredicate(scanSuffixes))
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

	/**
	 * Determines which Mule rule set should be applied for the current analysis.
	 *
	 * <p>The sensor infers Mule 3 vs Mule 4 based on which rule repository is active:
	 * when there are no active rules in the Mule 3 repository, Mule 4 is assumed.
	 *
	 * @param context SonarQube sensor context
	 * @return {@link MuleLanguage#LANGUAGE_MULE4_KEY} when Mule 4 rules are active; otherwise
	 *         {@link MuleLanguage#LANGUAGE_MULE3_KEY}
	 * @see MuleRulesDefinition#MULE3_REPOSITORY_KEY
	 * @see MuleRulesDefinition#MULE4_REPOSITORY_KEY
	 */
	public static String getLanguage(SensorContext context) {
		boolean mule4 = context.activeRules().findByRepository(MuleRulesDefinition.MULE3_REPOSITORY_KEY).isEmpty();
		return mule4 ? MuleLanguage.LANGUAGE_MULE4_KEY : MuleLanguage.LANGUAGE_MULE3_KEY;
	}
}
