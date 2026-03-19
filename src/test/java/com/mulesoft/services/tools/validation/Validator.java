package com.mulesoft.services.tools.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mulesoft.services.tools.validation.rules.Rulestore;

/**
 * Test harness that loads a rule store and validates all files under a fixture directory.
 *
 * <p>This class is used by unit/integration tests to execute the same rule evaluation pipeline
 * used by the plugin, but outside of SonarQube. It loads a {@link Rulestore} XML, walks the target
 * directory, and applies rules via {@link RuleConsumer}.
 *
 * @author franco.parma
 * @version 1.1.0
 * @since 1.1.0
 * @see RuleConsumer
 */
public class Validator {
	private Properties prop = new Properties();
	Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Creates a validator configured by a classpath properties file for the given language.
	 *
	 * <p>The properties file is expected to be named {@code app-<language>.properties} and must
	 * include a {@code rules} entry pointing to a rule store spec.
	 *
	 * @param language ruleset identifier (for example {@code mule3} or {@code mule4})
	 */
	public Validator(String language) {
		logger.info("Language: {}", language);
		String fileName = "app-" + language + ".properties";
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(fileName)) {
			prop.load(in);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Creates a validator configured by an external properties file.
	 *
	 * @param properties properties file containing a {@code rules} entry
	 */
	public Validator(File properties) {
		logger.info("Property File {}", properties);
		try (InputStream in = new FileInputStream(properties)) {
			prop.load(in);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Validates all files under the project directory by applying every rule from the given store.
	 *
	 * @param store rule store to apply
	 * @param proyectDir root directory to scan
	 */
	private void validate(Rulestore store, File proyectDir) {
		Consumer<Path> ruleConsumer = new RuleConsumer(store);
		try (Stream<Path> stream = Files.walk(proyectDir.toPath())) {
			stream.forEach(ruleConsumer);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

	}

	/**
	 * Loads the rule store spec from the configured properties and validates a project directory.
	 *
	 * @param proyectDir root directory to scan
	 * @throws JAXBException when rule store parsing fails
	 * @throws IOException when the rule store spec cannot be opened
	 */
	public void validate(File proyectDir) throws JAXBException, IOException {
		this.validate(RuleFactory.loadRulesFromXml(prop.getProperty("rules")), proyectDir);
	}

}
