package com.mulesoft.services.tools.sonarqube.sensor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.mulesoft.services.tools.sonarqube.language.MuleLanguage;
import com.mulesoft.services.tools.sonarqube.properties.MuleProperties;
import com.mulesoft.services.tools.sonarqube.rule.MuleRulesDefinition;
import com.mulesoft.services.tools.sonarqube.rule.scope.ScopeFactory;
import com.mulesoft.services.xpath.XPathProcessor;

/**
 * Applies the active Mule validation rules to a single {@link InputFile}.
 *
 * <p>This consumer is used by {@link MuleSensor} to evaluate all active rules for each Mule
 * configuration file found during analysis. It:
 * <ul>
 *   <li>loads XPath namespaces appropriate for Mule 3 or Mule 4</li>
 *   <li>optionally loads extra namespaces from a user-provided properties spec</li>
 *   <li>optionally filters rules by configured categories</li>
 *   <li>dispatches validation to a {@link com.mulesoft.services.tools.sonarqube.rule.scope.ScopeStrategy}</li>
 * </ul>
 *
 * @author franco.parma
 * @version 1.1.0
 * @since 1.1.0
 * @see MuleSensor
 */
public class SonarRuleConsumer implements Consumer<InputFile> {

	private final Logger logger = Loggers.get(SonarRuleConsumer.class);
	SensorContext context;
	Map<RuleKey, List<NewIssue>> issues;
	String language;
	XPathProcessor xpathProcessor;
	List<String> filteredCategories = new ArrayList<String>();
	String repositoryKey;

	private static final String FILTER_RULESET_PROPERTY = "sonar.property.ruleset.categories";
	private static final String EXTRA_NAMESPACES_SPEC_KEY = "sonar.mule.namespace.properties";

	/**
	 * Creates a new consumer for validating files in the given context.
	 *
	 * @param language the Mule variant key, typically {@link MuleLanguage#LANGUAGE_MULE3_KEY} or
	 *                 {@link MuleLanguage#LANGUAGE_MULE4_KEY}
	 * @param context the active SonarQube sensor context
	 * @param issues a mutable issue map used to collect issues by {@link RuleKey}
	 */
	public SonarRuleConsumer(String language, SensorContext context, Map<RuleKey, List<NewIssue>> issues) {
		this.language = language;
		this.context = context;
		this.issues = issues;
		this.repositoryKey = language != null && language.equals(MuleLanguage.LANGUAGE_MULE3_KEY)
				? MuleRulesDefinition.MULE3_REPOSITORY_KEY
				: MuleRulesDefinition.MULE4_REPOSITORY_KEY;
		xpathProcessor = new XPathProcessor().loadNamespaces(
				language.equals(MuleLanguage.LANGUAGE_MULE4_KEY) ? "namespace-4.properties" : "namespace-3.properties");

		this.context.config().get(EXTRA_NAMESPACES_SPEC_KEY).ifPresent(xpathProcessor::loadNamespacesSpec);

		String categories = (String) MuleProperties.getProperties(MuleLanguage.LANGUAGE_KEY)
				.get(FILTER_RULESET_PROPERTY);
		Optional<String> categoriesVariable = this.context.config().get(categories);
		if (categoriesVariable.isPresent()) {
			filteredCategories = Arrays.asList(categoriesVariable.get().split(","));
		}
	}

	/**
	 * Validates the given file by iterating over all active Mule rules and delegating validation
	 * to the appropriate scope strategy.
	 *
	 * @param t the input file to validate
	 */
	@Override
	public void accept(InputFile t) {
		if (logger.isDebugEnabled()) {
			logger.debug("Validating mule file:" + t.filename());
		}

		// Important: only apply Mule XML/XPath rules here. DataWeave rules share the same "mule" language
		// key for indexing, but they must be executed by the DataWeave sensor.
		Collection<ActiveRule> activeRules = this.context.activeRules().findByRepository(repositoryKey);

		Iterator<ActiveRule> iterator = rulesIterator(activeRules);

		while (iterator.hasNext()) {
			ActiveRule rule = iterator.next();
			if (logger.isDebugEnabled()) {
				logger.debug("Validating rule:" + rule.internalKey());
			}
			String appliesTo = rule.param(MuleRulesDefinition.PARAMS.SCOPE);
			String pluginVersion = safeTrim(rule.param(MuleRulesDefinition.PARAMS.PLUGIN_VERSION));
			if (pluginVersion.isEmpty()) {
				pluginVersion = "1.0";
			}
			ScopeFactory.getInstance().getStrategy(appliesTo, pluginVersion).validate(xpathProcessor, issues, context, t,
					rule);
		}
	}

	/**
	 * Trims the input string, returning an empty string when the value is null.
	 *
	 * @param s the input string, may be null
	 * @return the trimmed string, or {@code ""} when {@code s} is null
	 */
	private static String safeTrim(String s) {
		return s == null ? "" : s.trim();
	}

	/**
	 * Returns an iterator over the active rules, optionally filtered by category.
	 *
	 * @param activeRules the collection of active rules for the current analysis
	 * @return an iterator over rules to apply to each file
	 */
	private Iterator<ActiveRule> rulesIterator(Collection<ActiveRule> activeRules) {
		Iterator<ActiveRule> iterator;
		if (!filteredCategories.isEmpty()) {
			iterator = activeRules.stream().filter(new CategoryPredicate()).iterator();
		} else {
			iterator = activeRules.iterator();
		}
		return iterator;
	}

	/**
	 * Predicate that keeps only rules whose configured category is present in {@link #filteredCategories}.
	 */
	class CategoryPredicate implements Predicate<ActiveRule> {
		/**
		 * Tests whether the rule's configured category is enabled.
		 *
		 * @param t the active rule to test
		 * @return {@code true} when the rule category is in {@link #filteredCategories}; otherwise {@code false}
		 */
		@Override
		public boolean test(ActiveRule t) {
			String ruleCategory = t.param(MuleRulesDefinition.PARAMS.CATEGORY);
			return filteredCategories.contains(ruleCategory);
		}
	}

}
