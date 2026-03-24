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
import com.mulesoft.services.tools.sonarqube.rule.DataWeaveRulesDefinition;
import com.mulesoft.services.tools.sonarqube.rule.MuleRulesDefinition;
import com.mulesoft.services.tools.sonarqube.sensor.DataWeaveSensor;
import com.mulesoft.services.tools.sonarqube.sensor.MuleSensor;
import com.mulesoft.services.tools.sonarqube.xml.SecureJaxp;

/**
 * SonarQube plugin entry point that wires Mule analysis into the Sonar runtime.
 *
 * <p>This class registers:
 * <ul>
 *   <li>the Mule language definition and sensor</li>
 *   <li>rule definitions and the default quality profile</li>
 *   <li>custom measures/metrics and sensors (coverage, MUnit, etc.)</li>
 *   <li>plugin configuration properties (file suffixes, namespace properties)</li>
 * </ul>
 *
 * <p>It also hardens XML processing via {@link SecureJaxp} to reduce exposure to XXE and
 * related XML parser attacks during analysis.
 *
 * @version 1.1.0
 * @since 1.1.0
 * @see MuleLanguage
 * @see MuleSensor
 */
public class MulePlugin implements Plugin {

	// public static final String LANGUAGE_NAME = "Mule";
	private static final String GENERAL = "General";

	Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Registers all plugin extensions and configuration properties with SonarQube.
	 *
	 * @param context plugin context provided by SonarQube for extension registration
	 */
	@Override
	public void define(Context context) {
		if (logger.isDebugEnabled())
			logger.debug("Configuring Mule Plugin");

		SecureJaxp.harden();

		// Added Language
		context.addExtensions(MuleLanguage.class, MuleSensor.class, DataWeaveSensor.class);
		// context.addExtension(getProperties());

		// Added Rules
		context.addExtension(MuleRulesDefinition.class);
		context.addExtension(DataWeaveRulesDefinition.class);

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

		context.addExtension(PropertyDefinition.builder(DataWeaveSensor.DATAWEAVE_FILE_SUFFIXES_KEY)
				.defaultValue(DataWeaveSensor.DATAWEAVE_FILE_SUFFIXES_DEFAULT_VALUE).name("DataWeave File Suffixes")
				.description("List of suffixes to scan for DataWeave source files. These files are analyzed by the "
						+ "DataWeave sensor (independent of Mule XML scanning).")
				.subCategory(GENERAL).category(MuleLanguage.LANGUAGE_NAME).multiValues(true)
				.onQualifiers(Qualifiers.PROJECT).build());

		context.addExtension(PropertyDefinition.builder("sonar.mule.namespace.properties")
				.name("Extra namespace properties")
				.description("Optional URL/file/classpath spec to a .properties file containing additional XML namespaces "
						+ "(format: prefix=namespaceURI). Example: file:/path/to/namespaces.properties or classpath:namespace-extra.properties")
				.subCategory(GENERAL).category(MuleLanguage.LANGUAGE_NAME).onQualifiers(Qualifiers.PROJECT).build());

		context.addExtension(PropertyDefinition.builder(CoverageSensor.MUNIT_COVERAGE_JSON_REPORT_PATHS_KEY)
				.name("MUnit coverage JSON report paths")
				.description("Optional list of paths to MUnit coverage JSON report files. "
						+ "Paths may be absolute or relative to the module base directory. "
						+ "If unset, defaults are used: Mule 3 -> target/munit-reports/coverage-json/report.json, "
						+ "Mule 4 -> target/site/munit/coverage/munit-coverage.json.")
				.subCategory(GENERAL).category(MuleLanguage.LANGUAGE_NAME).multiValues(true)
				.onQualifiers(Qualifiers.PROJECT).build());

		context.addExtension(PropertyDefinition.builder(CoverageSensor.MUNIT_COVERAGE_JSON_REPORT_PATH_KEY)
				.name("MUnit coverage JSON report path (deprecated)")
				.description("Deprecated single-value alternative to '" + CoverageSensor.MUNIT_COVERAGE_JSON_REPORT_PATHS_KEY
						+ "'. Prefer the multi-value property.")
				.subCategory(GENERAL).category(MuleLanguage.LANGUAGE_NAME).onQualifiers(Qualifiers.PROJECT).build());

		context.addExtensions(MuleMetrics.class, ConfigurationFilesSensor.class, MuleSizeRating.class,
				MuleFlowCount.class, MuleSubFlowCount.class, MuleTransformationCount.class, CoverageSensor.class,
				MUnitSensor.class);
	}

}
