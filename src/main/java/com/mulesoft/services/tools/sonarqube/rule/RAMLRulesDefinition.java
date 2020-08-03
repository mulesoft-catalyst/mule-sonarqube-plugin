package com.mulesoft.services.tools.sonarqube.rule;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;

import org.apache.commons.io.IOUtils;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;

import com.mulesoft.services.tools.sonarqube.language.RAMLLanguage;

/**
 * RAML Rules Definition
 * 
 * @author franco.perez
 *
 */
public class RAMLRulesDefinition implements RulesDefinition {

	public static String REPOSITORY_KEY = "raml-repository";
	public static String RULE_KEY = "raml-1";

	@Override
	public void define(Context context) {
		NewRepository repository = context.createRepository(REPOSITORY_KEY, RAMLLanguage.LANGUAGE_KEY)
				.setName("RAML Analyzer");

		try {
			String profile = readRAMLProfile("file:extensions/plugins/profile.raml");

			NewRule x1Rule = repository.createRule(RULE_KEY).setName("profile")
					.setHtmlDescription("RAML Validation Profile").setActivatedByDefault(true)
					.setStatus(RuleStatus.READY);

			x1Rule.setSeverity(Severity.MAJOR);
			x1Rule.setType(RuleType.CODE_SMELL);
			x1Rule.addTags(RAMLLanguage.LANGUAGE_KEY);
			x1Rule.createParam("profile").setDefaultValue(profile).setType(RuleParamType.STRING);

			// don't forget to call done() to finalize the definition
			repository.done();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private String readRAMLProfile(String url) throws NoSuchFileException, IOException, MalformedURLException {
		try (InputStream stream = new URL(url).openConnection().getInputStream()) {
			if (stream == null) {
				throw new NoSuchFileException("Resource file not found");
			}
			return IOUtils.toString(stream, StandardCharsets.UTF_8);
		}
	}

}
