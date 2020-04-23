package com.mulesoft.services.tools.sonarqube.profile;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import com.mulesoft.services.tools.sonarqube.language.MuleLanguage;
import com.mulesoft.services.tools.sonarqube.rule.MuleRulesDefinition;
import com.mulesoft.services.tools.validation.RuleFactory;
import com.mulesoft.services.tools.validation.rules.Ruleset;
import com.mulesoft.services.tools.validation.rules.Rulestore;

/**
 * Mule Quality Profile
 * 
 * @author franco.perez
 *
 */
public class MuleQualityProfile implements BuiltInQualityProfilesDefinition {

	Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void define(Context context) {
		if (logger.isDebugEnabled())
			logger.debug("Creating MuleSoft Profiles");

		// Mule3
		NewBuiltInQualityProfile profile3 = context.createBuiltInQualityProfile("MuleSoft Rules for Mule 3.x",
				MuleLanguage.LANGUAGE_KEY);
		// profile3.setDefault(true);
		activeRule(profile3, MuleRulesDefinition.MULE3_REPOSITORY_KEY, "file:extensions/plugins/rules-3.xml");
		profile3.done();

		// Mule4
		NewBuiltInQualityProfile profile4 = context.createBuiltInQualityProfile("MuleSoft Rules for Mule 4.x",
				MuleLanguage.LANGUAGE_KEY);
		profile4.setDefault(true);
		activeRule(profile4, MuleRulesDefinition.MULE4_REPOSITORY_KEY, "file:extensions/plugins/rules-4.xml");
		profile4.done();
	}

	private void activeRule(NewBuiltInQualityProfile profile, String repositoryKey, String ruleFilename) {
		try {
			Rulestore rules = RuleFactory.loadRulesFromXml(ruleFilename);
			List<Ruleset> rulesetList = rules.getRuleset();

			for (Iterator<Ruleset> iterator = rulesetList.iterator(); iterator.hasNext();) {
				Ruleset ruleset = iterator.next();
				List<com.mulesoft.services.tools.validation.rules.Rule> ruleList = ruleset.getRule();
				for (Iterator<com.mulesoft.services.tools.validation.rules.Rule> ruleIterator = ruleList
						.iterator(); ruleIterator.hasNext();) {
					com.mulesoft.services.tools.validation.rules.Rule rule = ruleIterator.next();
					profile.activateRule(repositoryKey, MuleRulesDefinition.getRuleKey(ruleset, rule));
				}
			}
		} catch (JAXBException | IOException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}
