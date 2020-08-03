package com.mulesoft.services.tools.sonarqube.profile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import com.mulesoft.services.tools.sonarqube.language.RAMLLanguage;
import com.mulesoft.services.tools.sonarqube.rule.RAMLRulesDefinition;

/**
 * RAML Quality Profile
 * 
 * @author franco.perez
 *
 */
public class RAMLQualityProfile implements BuiltInQualityProfilesDefinition {

	Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void define(Context context) {
		if (logger.isDebugEnabled())
			logger.debug("Creating RAML Profiles");

		// Mule4
		NewBuiltInQualityProfile ramlProfile = context.createBuiltInQualityProfile("RAML Rules for RAML 1.x",
				RAMLLanguage.LANGUAGE_KEY);
		ramlProfile.setDefault(true);
		ramlProfile.activateRule(RAMLRulesDefinition.REPOSITORY_KEY, RAMLRulesDefinition.RULE_KEY);
		ramlProfile.done();

	}

}
