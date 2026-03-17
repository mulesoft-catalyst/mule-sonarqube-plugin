package com.mulesoft.services.tools.sonarqube;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import com.mulesoft.services.tools.sonarqube.language.MuleLanguage;
import com.mulesoft.services.tools.sonarqube.measures.MuleFlowCount;
import com.mulesoft.services.tools.sonarqube.measures.MuleSizeRating;
import com.mulesoft.services.tools.sonarqube.measures.MuleSubFlowCount;
import com.mulesoft.services.tools.sonarqube.measures.MuleTransformationCount;
import com.mulesoft.services.tools.sonarqube.metrics.ConfigurationFilesSensor;
import com.mulesoft.services.tools.sonarqube.metrics.CoverageSensor;
import com.mulesoft.services.tools.sonarqube.metrics.MUnitSensor;
import com.mulesoft.services.tools.sonarqube.metrics.MuleMetrics;
import com.mulesoft.services.tools.sonarqube.profile.MuleQualityProfile;
import com.mulesoft.services.tools.sonarqube.rule.MuleRulesDefinition;
import com.mulesoft.services.tools.sonarqube.sensor.MuleSensor;
import com.mulesoft.services.tools.sonarqube.xml.SecureJaxp;

public class MulePlugin implements Plugin {

	// public static final String LANGUAGE_NAME = "Mule";
	private static final String GENERAL = "General";

	Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void define(Context context) {
		if (logger.isDebugEnabled())
			logger.debug("Configuring Mule Plugin");

		SecureJaxp.harden();

		// Added Language
		context.addExtensions(MuleLanguage.class, MuleSensor.class);
		// context.addExtension(getProperties());

		// Added Rules
		context.addExtension(MuleRulesDefinition.class);

		// Added Profile
		context.addExtension(MuleQualityProfile.class);

		context.addExtension(PropertyDefinition.builder(MuleLanguage.FILE_SUFFIXES_KEY)
				.defaultValue(MuleLanguage.FILE_SUFFIXES_DEFAULT_VALUE).name("File Suffixes")
				.description("List of suffixes for files to analyze.").subCategory(GENERAL)
				.category(MuleLanguage.LANGUAGE_NAME).multiValues(true).onQualifiers(Qualifiers.PROJECT).build());

		context.addExtension(PropertyDefinition.builder(MuleLanguage.SCAN_FILE_SUFFIXES_KEY)
				.defaultValue(MuleLanguage.SCAN_FILE_SUFFIXES_DEFAULT_VALUE).name("Scan File Suffixes")
				.description("List of suffixes to scan for Mule configuration files (independent of language detection).")
				.subCategory(GENERAL).category(MuleLanguage.LANGUAGE_NAME).multiValues(true)
				.onQualifiers(Qualifiers.PROJECT).build());

		context.addExtension(PropertyDefinition.builder("sonar.mule.namespace.properties")
				.name("Extra namespace properties")
				.description("Optional URL/file/classpath spec to a .properties file containing additional XML namespaces "
						+ "(format: prefix=namespaceURI). Example: file:/path/to/namespaces.properties or classpath:namespace-extra.properties")
				.subCategory(GENERAL).category(MuleLanguage.LANGUAGE_NAME).onQualifiers(Qualifiers.PROJECT).build());

		context.addExtensions(MuleMetrics.class, ConfigurationFilesSensor.class, MuleSizeRating.class,
				MuleFlowCount.class, MuleSubFlowCount.class, MuleTransformationCount.class, CoverageSensor.class,
				MUnitSensor.class);
	}

}
