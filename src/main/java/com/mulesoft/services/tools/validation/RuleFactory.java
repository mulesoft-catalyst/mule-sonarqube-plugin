package com.mulesoft.services.tools.validation;

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

	static {
		System.setProperty("java.protocol.handler.pkgs", "com.mulesoft.services.protocols");
	}

	private RuleFactory() {
		throw new IllegalStateException("Utility class");
	}

	public static Rulestore loadRulesFromXml(String spec) throws JAXBException, IOException {
		Rulestore rulestore = null;

		try (InputStream stream = new URL(spec).openConnection().getInputStream()) {
			if (stream == null) {
				throw new NoSuchFileException("Resource file not found");
			}
			rulestore = RuleFactory.loadRulesFromXml(stream);
		}

		return rulestore;
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
