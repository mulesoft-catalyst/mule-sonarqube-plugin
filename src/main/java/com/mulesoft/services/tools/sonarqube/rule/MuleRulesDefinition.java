package com.mulesoft.services.tools.sonarqube.rule;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Arrays;

import javax.xml.bind.JAXBException;

import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.mulesoft.services.tools.sonarqube.language.MuleLanguage;
import com.mulesoft.services.tools.sonarqube.properties.MuleProperties;
import com.mulesoft.services.tools.validation.Constants;
import com.mulesoft.services.tools.validation.Constants.Severities;
import com.mulesoft.services.tools.validation.Constants.Types;
import com.mulesoft.services.tools.validation.RuleFactory;
import com.mulesoft.services.tools.validation.rules.Ruleset;
import com.mulesoft.services.tools.validation.rules.Rulestore;

public class MuleRulesDefinition implements RulesDefinition {

	private final Logger logger = Loggers.get(MuleRulesDefinition.class);

	public static String MULE3_REPOSITORY_KEY = "mule3-repository";
	public static String MULE4_REPOSITORY_KEY = "mule4-repository";
	public static final String RULE_TEMPLATE_KEY = "mule-rule-template";

	public interface PARAMS {
		String CATEGORY = "category";
		String SCOPE = "scope";
		String XPATH = "xpath-expression";
		String XPATH_LOCATION_HINT = "xpath-location-hint";
		String PLUGIN_VERSION = "plugin-version";
	}

	@Override
	public void define(Context context) {
		if (logger.isDebugEnabled())
			logger.debug("Defining MuleSoft Rule - Based on Mule-Validation-XPath-Core rules");

		logger.info("Working Directory = {}", System.getProperty("user.dir"));

		createRepository(context, MULE3_REPOSITORY_KEY, MuleLanguage.LANGUAGE_KEY, "Mule3 Analyzer",
				Arrays.asList("file:extensions/plugins/rules-3.xml"),
				Arrays.asList("classpath:rules-3.xml"));
		createRepository(context, MULE4_REPOSITORY_KEY, MuleLanguage.LANGUAGE_KEY, "Mule4 Analyzer",
				Arrays.asList("file:extensions/plugins/rules-4.xml", "file:extensions/plugins/rules-4-custom.xml"),
				Arrays.asList("classpath:rules-4.xml", "classpath:rules-4-custom.xml"));

	}

	private void createRepository(Context context, String repositoryKey, String language, String repositoryName,
			List<String> primaryRulesSpecs, List<String> fallbackRulesSpecs) {
		NewRepository repository = context.createRepository(repositoryKey, language).setName(repositoryName);
		try {
			for (int i = 0; i < primaryRulesSpecs.size(); i++) {
				String primaryRulesSpec = primaryRulesSpecs.get(i);
				String fallbackRulesSpec = (fallbackRulesSpecs != null && fallbackRulesSpecs.size() > i)
						? fallbackRulesSpecs.get(i)
						: null;
				Rulestore rulestore = loadRules(primaryRulesSpec, fallbackRulesSpec);
				if (rulestore == null) {
					continue;
				}

				List<Ruleset> rulesetList = rulestore.getRuleset();
				for (Iterator<Ruleset> iterator = rulesetList.iterator(); iterator.hasNext();) {
					Ruleset ruleset = iterator.next();
					logger.debug("Rule Category :  " + ruleset.getCategory());
					List<com.mulesoft.services.tools.validation.rules.Rule> ruleList = ruleset.getRule();
					for (Iterator<com.mulesoft.services.tools.validation.rules.Rule> ruleIterator = ruleList
							.iterator(); ruleIterator.hasNext();) {
						com.mulesoft.services.tools.validation.rules.Rule rule = ruleIterator.next();
						logger.debug("Rule Id :  " + rule.getId());
						addRule(repository, ruleset, rule, language);
					}
				}
			}

		} catch (JAXBException | IOException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage(), e);
		}

		addRuleTemplate(repository, language);
		repository.rule(RULE_TEMPLATE_KEY).setTemplate(true);
		// don't forget to call done() to finalize the definition
		repository.done();

	}

	private Rulestore loadRules(String primaryRulesSpec, String fallbackRulesSpec) throws JAXBException, IOException {
		try {
			return RuleFactory.loadRulesFromXml(primaryRulesSpec);
		} catch (IOException | JAXBException primaryFailure) {
			if (fallbackRulesSpec == null) {
				logger.warn("Failed to load rules from {} and no fallback configured.", primaryRulesSpec, primaryFailure);
				return null;
			}
			logger.warn("Failed to load rules from {}. Falling back to {}.", primaryRulesSpec, fallbackRulesSpec,
					primaryFailure);
			return RuleFactory.loadRulesFromXml(fallbackRulesSpec);
		}
	}

	private void addRuleTemplate(NewRepository repository, String language) {

		Properties prop = (repository.key().equals(MULE3_REPOSITORY_KEY)
				? MuleProperties.getProperties(MuleLanguage.LANGUAGE_MULE3_KEY)
				: MuleProperties.getProperties(MuleLanguage.LANGUAGE_MULE4_KEY));
		NewRule x1Rule = repository.createRule(RULE_TEMPLATE_KEY).setName(prop.getProperty("rule.template.name"))
				.setHtmlDescription(prop.getProperty("rule.template.description"));

		x1Rule.addTags(language);
		x1Rule.createParam(PARAMS.CATEGORY).setDescription(prop.getProperty("rule.template.parameter.category"))
				.setType(RuleParamType.STRING);
		x1Rule.createParam(PARAMS.XPATH).setDescription(prop.getProperty("rule.template.parameter.xpath"))
				.setType(RuleParamType.STRING);
		x1Rule.createParam(PARAMS.XPATH_LOCATION_HINT).setDescription(prop.getProperty("rule.template.parameter.xpathlocationhint"))
		.setType(RuleParamType.STRING);
		x1Rule.createParam(PARAMS.SCOPE).setDescription(prop.getProperty("rule.template.parameter.scope"))
				.setType(RuleParamType.STRING);
		x1Rule.createParam(PARAMS.PLUGIN_VERSION).setDescription(prop.getProperty("rule.template.parameter.pluginversion"))
				.setType(RuleParamType.STRING);

		logger.info("addRuleTemplate x1Rule="+x1Rule);

	}

	private void addRule(NewRepository repository, Ruleset ruleset,
			com.mulesoft.services.tools.validation.rules.Rule rule, String language) {

		NewRule x1Rule = repository.createRule(MuleRulesDefinition.getRuleKey(ruleset, rule)).setName(rule.getName())
				.setHtmlDescription(rule.getDescription()).setActivatedByDefault(true)
				.setHtmlDescription(rule.getDescription()).setStatus(RuleStatus.READY);

		x1Rule.setSeverity(getSeverity(rule));
		x1Rule.setType(getType(rule));
		x1Rule.addTags(language);
		x1Rule.createParam(PARAMS.CATEGORY).setDefaultValue(ruleset.getCategory()).setType(RuleParamType.STRING);
		x1Rule.createParam(PARAMS.XPATH).setDefaultValue(rule.getValue()).setType(RuleParamType.STRING);
		String locationHint = rule.getLocationHint() == null ? "" : rule.getLocationHint();
		logger.debug("LocationHint={} for {}", locationHint, rule.getName());
		x1Rule.createParam(PARAMS.XPATH_LOCATION_HINT).setDefaultValue(locationHint).setType(RuleParamType.STRING);
		if (rule.getApplies() != null) {
			x1Rule.createParam(PARAMS.SCOPE).setDefaultValue(rule.getApplies()).setType(RuleParamType.STRING);
		}
		String pluginVersion = rule.getPluginVersion();
		if (pluginVersion == null || pluginVersion.isEmpty()) {
			pluginVersion = ruleset.getPluginVersion();
		}
		if (pluginVersion == null || pluginVersion.isEmpty()) {
			pluginVersion = "1.0";
		}
		x1Rule.createParam(PARAMS.PLUGIN_VERSION).setDefaultValue(pluginVersion).setType(RuleParamType.STRING);

	}

	private String getSeverity(com.mulesoft.services.tools.validation.rules.Rule rule) {
		Severities sev = Severities.valueOf(rule.getSeverity());
		String severity;
		switch (sev) {
		case BLOCKER:
			severity = Severity.BLOCKER;
			break;
		case CRITICAL:
			severity = Severity.CRITICAL;
			break;
		case MAJOR:
			severity = Severity.MAJOR;
			break;
		case MINOR:
			severity = Severity.MINOR;
			break;
		case INFO:
			severity = Severity.INFO;
			break;
		default:
			severity = Severity.INFO;
			break;
		}
		return severity;
	}

	private RuleType getType(com.mulesoft.services.tools.validation.rules.Rule rule) {
		Types types = Types
				.valueOf((rule.getType() != null ? rule.getType().toUpperCase() : Constants.Type.BUG.toUpperCase()));
		RuleType type;
		switch (types) {
		case BUG:
			type = RuleType.BUG;
			break;
		case CODE_SMELL:
			type = RuleType.CODE_SMELL;
			break;
		case VULNERABILITY:
			type = RuleType.VULNERABILITY;
			break;
		default:
			type = RuleType.BUG;
			break;
		}
		return type;
	}

	public static String getRuleKey(Ruleset ruleset, com.mulesoft.services.tools.validation.rules.Rule rule) {
		return ruleset.getCategory() + "." + rule.getId();

	}

}
