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
 * Iterates over all the mule files a evaluates all the rules
 * 
 * @author franco.parma
 *
 */
public class Validator {
	private Properties prop = new Properties();
	Logger logger = LoggerFactory.getLogger(getClass());

	public Validator(String language) {
		logger.info("Language: {}", language);
		String fileName = "app-" + language + ".properties";
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(fileName)) {
			prop.load(in);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public Validator(File properties) {
		logger.info("Property File {}", properties);
		try (InputStream in = new FileInputStream(properties)) {
			prop.load(in);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void validate(Rulestore store, File proyectDir) {
		Consumer<Path> ruleConsumer = new RuleConsumer(store);
		try (Stream<Path> stream = Files.walk(proyectDir.toPath())) {
			stream.forEach(ruleConsumer);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

	}

	public void validate(File proyectDir) throws JAXBException, IOException {
		this.validate(RuleFactory.loadRulesFromXml(prop.getProperty("rules")), proyectDir);
	}

}
