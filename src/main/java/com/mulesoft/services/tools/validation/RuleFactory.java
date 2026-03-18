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
 * Loads {@link Rulestore} definitions from XML specifications.
 *
 * <p>The factory supports rule store specifications in multiple forms:
 * <ul>
 *   <li>{@code classpath:...} resources</li>
 *   <li>{@code file:...} URLs (and other URL schemes supported by {@link URL})</li>
 *   <li>plain filesystem paths (as a convenience)</li>
 * </ul>
 *
 * <p>The loaded XML is unmarshalled via JAXB into the generated {@link Rulestore} model.
 *
 * @author franco.parma
 * @version 1.1.0
 * @since 1.1.0
 * @see Rulestore
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

	/**
	 * Loads a rule store from the provided spec string.
	 *
	 * @param spec rule store spec (classpath URL, file URL, or filesystem path)
	 * @return the loaded rule store
	 * @throws JAXBException when unmarshalling fails
	 * @throws IOException when the spec cannot be opened or read
	 */
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

	/**
	 * Opens an {@link InputStream} for the given rules spec.
	 *
	 * @param spec spec string to resolve
	 * @return an input stream for the resolved resource
	 * @throws IOException when the resource cannot be opened
	 */
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

	/**
	 * Unmarshals a {@link Rulestore} from an XML stream using JAXB.
	 *
	 * <p>The thread context class loader is temporarily switched to ensure JAXB can resolve the
	 * generated JAXB model classes when running inside SonarQube/plugin classloaders.
	 *
	 * @param stream input stream containing the XML rule store
	 * @return the unmarshalled rule store
	 * @throws JAXBException when unmarshalling fails
	 */
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
