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
 * Consumer to validate an InputFile against all the rules
 * 
 * @author franco.parma
 *
 */
public class SonarRuleConsumer implements Consumer<InputFile> {

	private final Logger logger = Loggers.get(SonarRuleConsumer.class);
	SensorContext context;
	Map<RuleKey, List<NewIssue>> issues;
	String language;
	XPathProcessor xpathProcessor;
	List<String> filteredCategories = new ArrayList<String>();

	private static final String FILTER_RULESET_PROPERTY = "sonar.property.ruleset.categories";

	public SonarRuleConsumer(String language, SensorContext context, Map<RuleKey, List<NewIssue>> issues) {
		this.language = language;
		this.context = context;
		this.issues = issues;
		xpathProcessor = new XPathProcessor().loadNamespaces(
				language.equals(MuleLanguage.LANGUAGE_MULE4_KEY) ? "namespace-4.properties" : "namespace-3.properties");

		String categories = (String) MuleProperties.getProperties(MuleLanguage.LANGUAGE_KEY)
				.get(FILTER_RULESET_PROPERTY);
		Optional<String> categoriesVariable = this.context.config().get(categories);
		if (categoriesVariable.isPresent()) {
			filteredCategories = Arrays.asList(categoriesVariable.get().split(","));
		}
	}

	@Override
	public void accept(InputFile t) {
		if (logger.isDebugEnabled()) {
			logger.debug("Validating mule file:" + t.filename());
		}

		Collection<ActiveRule> activeRules = this.context.activeRules().findByLanguage(MuleLanguage.LANGUAGE_KEY);

		Iterator<ActiveRule> iterator = rulesIterator(activeRules);

		while (iterator.hasNext()) {
			ActiveRule rule = iterator.next();
			if (logger.isDebugEnabled()) {
				logger.debug("Validating rule:" + rule.internalKey());
			}
			String appliesTo = rule.param(MuleRulesDefinition.PARAMS.SCOPE);

			ScopeFactory.getInstance().getStrategy(appliesTo).validate(xpathProcessor, issues, context, t, rule);
		}
	}

	private Iterator<ActiveRule> rulesIterator(Collection<ActiveRule> activeRules) {
		Iterator<ActiveRule> iterator;
		if (!filteredCategories.isEmpty()) {
			iterator = activeRules.stream().filter(new CategoryPredicate()).iterator();
		} else {
			iterator = activeRules.iterator();
		}
		return iterator;
	}

	class CategoryPredicate implements Predicate<ActiveRule> {
		@Override
		public boolean test(ActiveRule t) {
			String ruleCategory = t.param(MuleRulesDefinition.PARAMS.CATEGORY);
			return filteredCategories.contains(ruleCategory);
		}
	}

}
