package com.mulesoft.services.tools.validation;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.NoSuchFileException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mulesoft.services.tools.validation.rules.Rulestore;

/**
 * Rulestore factory class
 * 
 * @type Factory
 * @author franco.parma
 *
 */
public class RuleFactory {
	static Logger logger = LoggerFactory.getLogger(RuleFactory.class);
	private static final String CLASSPATH_PREFIX = "classpath:";

	static {
		System.setProperty("java.protocol.handler.pkgs", "com.mulesoft.services.protocols");
	}

	private RuleFactory() {
		throw new IllegalStateException("Utility class");
	}

	public static Rulestore loadRulesFromXml(String spec) throws JAXBException, IOException {
		Rulestore rulestore = null;

		try (InputStream stream = openSpec(spec)) {
			rulestore = RuleFactory.loadRulesFromXml(stream);
		} catch (NoSuchFileException e) {
			logger.error("Rules resource not found for spec={}", spec);
			throw e;
		} catch (IOException e) {
			logger.error("Failed to load rules for spec={}", spec, e);
			throw e;
		}

		return rulestore;
	}

	private static InputStream openSpec(String spec) throws IOException {
		if (spec == null || spec.trim().isEmpty()) {
			throw new NoSuchFileException("Rules spec is empty");
		}
		String trimmed = spec.trim();
		if (trimmed.startsWith(CLASSPATH_PREFIX)) {
			String resourceName = trimmed.substring(CLASSPATH_PREFIX.length());
			if (resourceName.startsWith("/")) {
				resourceName = resourceName.substring(1);
			}
			InputStream stream = RuleFactory.class.getClassLoader().getResourceAsStream(resourceName);
			if (stream == null) {
				throw new NoSuchFileException("Classpath resource not found: " + resourceName);
			}
			return stream;
		}

		// Convenience: allow plain filesystem paths without "file:" prefix
		if (!trimmed.contains(":")) {
			return new FileInputStream(trimmed);
		}

		InputStream stream = new URL(trimmed).openConnection().getInputStream();
		if (stream == null) {
			throw new NoSuchFileException("Resource file not found");
		}
		return stream;
	}

	public static Rulestore loadRulesFromXml(InputStream stream) throws JAXBException {
		ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
		Unmarshaller jaxbUnmarshaller = null;
		try {
			Thread.currentThread().setContextClassLoader(Rulestore.class.getClassLoader());

			JAXBContext jaxbContext = JAXBContext.newInstance(Rulestore.class);
			jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		} finally {
			Thread.currentThread().setContextClassLoader(threadClassLoader);
		}
		return (Rulestore) jaxbUnmarshaller.unmarshal(stream);

	}
}
