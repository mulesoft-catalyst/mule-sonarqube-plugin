package com.mulesoft.services.tools.sonarqube.rule;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.mulesoft.services.tools.sonarqube.language.MuleLanguage;
import com.mulesoft.services.tools.validation.Constants;
import com.mulesoft.services.tools.validation.Constants.Severities;
import com.mulesoft.services.tools.validation.Constants.Types;
import com.mulesoft.services.tools.validation.RuleFactory;
import com.mulesoft.services.tools.validation.rules.Ruleset;
import com.mulesoft.services.tools.validation.rules.Rulestore;

/**
 * Defines DataWeave rules for SonarQube.
 *
 * <p>This definition loads DataWeave rule metadata from {@code rules-dataweave.xml} and registers them
 * in a dedicated repository.
 *
 * <p>Rules are registered under the Mule language key so `.dwl` files are indexed as "Mule" in SonarQube,
 * but they are executed by {@link com.mulesoft.services.tools.sonarqube.sensor.DataWeaveSensor} (not the
 * Mule XML/XPath sensor).
 */
public class DataWeaveRulesDefinition implements RulesDefinition {

	private static final Logger logger = Loggers.get(DataWeaveRulesDefinition.class);

	public static final String REPOSITORY_KEY = "mule-dataweave-repository";
	public static final String REPOSITORY_NAME = "Mule DataWeave Analyzer";

	public static final String COMMENTED_OUT_CODE_RULE_KEY = "commented-out-code";
	public static final String TOO_LARGE_FILE_RULE_KEY = "too-large-file";
	/**
	 * Rule parameter name for the maximum allowed line count of a DataWeave file.
	 */
	public static final String TOO_LARGE_FILE_MAX_LINES_PARAM = "maxLines";

	/**
	 * Creates the DataWeave rule repository and registers its rules.
	 *
	 * <p>Rule metadata is loaded from disk first (to allow runtime customization) and falls back to
	 * the embedded classpath resource.
	 */
	@Override
	public void define(Context context) {
		NewRepository repository = context.createRepository(REPOSITORY_KEY, MuleLanguage.LANGUAGE_KEY)
				.setName(REPOSITORY_NAME);

		Rulestore store = loadRules("file:extensions/plugins/rules-dataweave.xml", "classpath:rules-dataweave.xml");
		for (Ruleset ruleset : store.getRuleset()) {
			List<com.mulesoft.services.tools.validation.rules.Rule> rules = ruleset.getRule();
			for (com.mulesoft.services.tools.validation.rules.Rule rule : rules) {
				addRule(repository, rule);
			}
		}

		repository.done();
	}

	/**
	 * Loads the DataWeave rules store, preferring an external rules file when present.
	 *
	 * @param primarySpec file-based spec, typically {@code file:extensions/plugins/rules-dataweave.xml}
	 * @param fallbackSpec classpath spec, typically {@code classpath:rules-dataweave.xml}
	 * @return loaded rule store
	 */
	private static Rulestore loadRules(String primarySpec, String fallbackSpec) {
		try {
			return RuleFactory.loadRulesFromXml(primarySpec);
		} catch (IOException | JAXBException primaryFailure) {
			logger.warn("Failed to load DataWeave rules from {}. Falling back to {}.", primarySpec, fallbackSpec,
					primaryFailure);
			try {
				return RuleFactory.loadRulesFromXml(fallbackSpec);
			} catch (IOException | JAXBException fallbackFailure) {
				throw new RuntimeException("Failed to load DataWeave rules from " + primarySpec + " and " + fallbackSpec,
						fallbackFailure);
			}
		}
	}

	/**
	 * Registers a single DataWeave rule in SonarQube.
	 *
	 * <p>The SonarQube rule key is taken from the XML {@code id} attribute.
	 *
	 * <p>Some rules define additional parameters. For example {@link #TOO_LARGE_FILE_RULE_KEY} exposes a
	 * {@link #TOO_LARGE_FILE_MAX_LINES_PARAM} integer parameter whose default value is sourced from the
	 * rule's XML body (value).
	 */
	private static void addRule(NewRepository repository, com.mulesoft.services.tools.validation.rules.Rule rule) {
		NewRule sonarRule = repository.createRule(rule.getId())
				.setName(rule.getName())
				.setHtmlDescription(rule.getDescription())
				.setActivatedByDefault(true)
				.setStatus(RuleStatus.READY);

		sonarRule.setSeverity(getSeverity(rule));
		sonarRule.setType(getType(rule));
		sonarRule.addTags("dataweave", "mule");

		if (TOO_LARGE_FILE_RULE_KEY.equals(rule.getId())) {
			String maxLinesDefault = parsePositiveIntOrDefault(rule.getValue(), "5000");
			sonarRule.createParam(TOO_LARGE_FILE_MAX_LINES_PARAM)
					.setType(RuleParamType.INTEGER)
					.setDefaultValue(maxLinesDefault)
					.setDescription("Maximum allowed line count for a DataWeave (.dwl) file.");
		}
	}

	/**
	 * Attempts to parse a strictly positive integer string.
	 *
	 * @param raw raw string value (may be null/blank/non-numeric)
	 * @param defaultValue value to return when parsing fails
	 * @return {@code raw} normalized as a positive integer string, or {@code defaultValue}
	 */
	private static String parsePositiveIntOrDefault(String raw, String defaultValue) {
		if (raw == null) {
			return defaultValue;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return defaultValue;
		}
		try {
			int v = Integer.parseInt(t);
			return v > 0 ? Integer.toString(v) : defaultValue;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * Maps the rules XML severity string to a SonarQube severity constant.
	 */
	private static String getSeverity(com.mulesoft.services.tools.validation.rules.Rule rule) {
		Severities sev = Severities.valueOf(rule.getSeverity());
		switch (sev) {
		case BLOCKER:
			return Severity.BLOCKER;
		case CRITICAL:
			return Severity.CRITICAL;
		case MAJOR:
			return Severity.MAJOR;
		case MINOR:
			return Severity.MINOR;
		case INFO:
		default:
			return Severity.INFO;
		}
	}

	/**
	 * Maps the rules XML type string to a SonarQube {@link RuleType}.
	 */
	private static RuleType getType(com.mulesoft.services.tools.validation.rules.Rule rule) {
		Types types = Types
				.valueOf((rule.getType() != null ? rule.getType().toUpperCase() : Constants.Type.BUG.toUpperCase()));
		switch (types) {
		case BUG:
			return RuleType.BUG;
		case CODE_SMELL:
			return RuleType.CODE_SMELL;
		case VULNERABILITY:
			return RuleType.VULNERABILITY;
		default:
			return RuleType.BUG;
		}
	}
}

