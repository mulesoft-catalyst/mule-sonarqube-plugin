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
 * Registers built-in quality profiles for Mule projects.
 *
 * <p>The plugin provides separate profiles for Mule 3.x and Mule 4.x by activating all rules
 * defined in the corresponding rule repositories. Mule 4 is set as the default profile.
 *
 * @author franco.perez
 * @version 1.1.0
 * @since 1.1.0
 * @see MuleRulesDefinition
 */
public class MuleQualityProfile implements BuiltInQualityProfilesDefinition {

	Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Defines the built-in quality profiles and activates rules within them.
	 *
	 * @param context SonarQube quality profiles definition context
	 */
	@Override
	public void define(Context context) {
		if (logger.isDebugEnabled())
			logger.debug("Creating MuleSoft Profiles");

		// Mule3
		NewBuiltInQualityProfile profile3 = context.createBuiltInQualityProfile("MuleSoft Rules for Mule 3.x",
				MuleLanguage.LANGUAGE_KEY);
		// profile3.setDefault(true);
		activeRule(profile3, MuleRulesDefinition.MULE3_REPOSITORY_KEY, "file:extensions/plugins/rules-3.xml",
				"classpath:rules-3.xml");
		profile3.done();

		// Mule4
		NewBuiltInQualityProfile profile4 = context.createBuiltInQualityProfile("MuleSoft Rules for Mule 4.x",
				MuleLanguage.LANGUAGE_KEY);
		profile4.setDefault(true);
		activeRule(profile4, MuleRulesDefinition.MULE4_REPOSITORY_KEY, "file:extensions/plugins/rules-4.xml",
				"classpath:rules-4.xml");
		activeRule(profile4, MuleRulesDefinition.MULE4_REPOSITORY_KEY, "file:extensions/plugins/rules-4-custom.xml",
				"classpath:rules-4-custom.xml");
		profile4.done();
	}

	/**
	 * Activates every rule contained in the provided rule store into the given profile.
	 *
	 * <p>The method tries to load rules from a file-based spec first and falls back to a classpath
	 * resource when the primary spec cannot be loaded.
	 *
	 * @param profile the profile to activate rules in
	 * @param repositoryKey the SonarQube repository key owning the rules
	 * @param primaryRulesSpec primary rule store spec (typically {@code file:...})
	 * @param fallbackRulesSpec fallback rule store spec (typically {@code classpath:...})
	 */
	private void activeRule(NewBuiltInQualityProfile profile, String repositoryKey, String primaryRulesSpec,
			String fallbackRulesSpec) {
		try {
			Rulestore rules;
			try {
				rules = RuleFactory.loadRulesFromXml(primaryRulesSpec);
			} catch (IOException | JAXBException primaryFailure) {
				logger.warn("Failed to load rules from {}. Falling back to {}.", primaryRulesSpec, fallbackRulesSpec,
						primaryFailure);
				rules = RuleFactory.loadRulesFromXml(fallbackRulesSpec);
			}
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
