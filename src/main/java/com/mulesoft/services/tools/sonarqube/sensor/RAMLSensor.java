package com.mulesoft.services.tools.sonarqube.sensor;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.mulesoft.services.tools.sonarqube.language.RAMLLanguage;
import com.mulesoft.services.tools.sonarqube.rule.RAMLRulesDefinition;

import amf.MessageStyles;
import amf.ProfileNames;
import amf.client.AMF;
import amf.client.model.document.BaseUnit;
import amf.client.parse.Raml10Parser;
import amf.client.validate.ValidationReport;
import amf.client.validate.ValidationResult;

/**
 * RAML Sensor
 * 
 * @author franco.perez
 *
 */
public class RAMLSensor implements Sensor {

	private final Logger logger = Loggers.get(RAMLSensor.class);

	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.onlyOnLanguage(RAMLLanguage.LANGUAGE_KEY);
		descriptor.createIssuesForRuleRepositories(RAMLRulesDefinition.REPOSITORY_KEY);

	}

	@Override
	public void execute(SensorContext context) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing RAML Sensor");
		}

		try {
			FileSystem fs = context.fileSystem();

			FilePredicates p = fs.predicates();

			Iterable<InputFile> files = fs.inputFiles(p.and(p.hasLanguage(RAMLLanguage.LANGUAGE_KEY)));

			for (InputFile file : files) {
				Collection<ActiveRule> activeRules = context.activeRules().findByLanguage(RAMLLanguage.LANGUAGE_KEY);
				for (Iterator<ActiveRule> iterator = activeRules.iterator(); iterator.hasNext();) {
					ActiveRule activeRule = iterator.next();

					validateFile(file);

					NewIssue newIssue = context.newIssue().forRule(activeRule.ruleKey());
					NewIssueLocation primaryLocation = newIssue.newLocation().on(file);
					newIssue.at(primaryLocation);
					newIssue.save();
				}

			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void validateFile(InputFile file) throws InterruptedException, ExecutionException, IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Validate RAML File: " + file.filename());
		}
		AMF.init().get();

		Raml10Parser parser = new Raml10Parser();
//		AMF.loadValidationProfile("file:extensions/plugins/profile.raml").get();

		// Load File
		String url = "file:///" + file.file().getCanonicalPath();

		BaseUnit doc = (BaseUnit) parser.parseFileAsync(url).get();

//		ProfileName name = new ProfileName("Custom");
//		ValidationReport report = AMF.validate(doc, name, MessageStyles.RAML()).get();
		ValidationReport report = AMF.validate(doc, ProfileNames.RAML10(), MessageStyles.RAML()).get();

		System.out.print("Validates RAML?:");
		System.out.println(report.conforms());
		if (!report.conforms()) {
			System.out.println("Errors:");
			for (ValidationResult result : report.results()) {
				System.out.println(" - " + result.message() + " => " + result.targetNode());
			}
		}
	}
}
