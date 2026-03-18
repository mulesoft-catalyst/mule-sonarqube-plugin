package com.mulesoft.services.tools.sonarqube.properties;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mulesoft.services.tools.sonarqube.language.MuleLanguage;

/**
 * Loads and caches plugin property bundles used to externalize strings and defaults.
 *
 * <p>The plugin uses different property bundles for Mule 3 and Mule 4 so templates and
 * parameter descriptions can vary by runtime/version.
 *
 * @version 1.1.0
 * @since 1.1.0
 */
public class MuleProperties {

	private static final String MULE_PROP = "mule.properties";
	private static final String MULE4_PROP = "mule4.properties";
	private static final String MULE3_PROP = "mule3.properties";

	static private Logger logger = LoggerFactory.getLogger(MuleProperties.class);

	static private Map<String, Properties> props = new HashMap<String, Properties>();

	/**
	 * Loads a properties file from the classpath and stores it in the internal cache.
	 *
	 * @param language cache key (typically a Mule language variant key)
	 * @param propName classpath resource name (for example {@code "mule4.properties"})
	 * @return loaded properties (may be empty when loading fails)
	 */
	private static Properties loadProp(String language, String propName) {
		Properties properties = new Properties();
		try (InputStream input = MuleProperties.class.getClassLoader().getResourceAsStream(propName)) {
			properties.load(input);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		props.put(language, properties);
		return properties;
	}

	/**
	 * Returns the properties for the given Mule language variant, loading them on first use.
	 *
	 * @param language language/variant key (for example {@link MuleLanguage#LANGUAGE_MULE4_KEY})
	 * @return properties for the requested variant
	 */
	public static Properties getProperties(String language) {
		if (props.containsKey(language)) {
			return props.get(language);
		} else {
			switch (language) {
			case MuleLanguage.LANGUAGE_MULE4_KEY:
				return loadProp(language, MULE4_PROP);
			case MuleLanguage.LANGUAGE_MULE3_KEY:
				return loadProp(language, MULE3_PROP);
			default:
				return loadProp(language, MULE_PROP);
			}
		}
	}
}