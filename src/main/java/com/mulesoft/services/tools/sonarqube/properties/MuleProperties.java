package com.mulesoft.services.tools.sonarqube.properties;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mulesoft.services.tools.sonarqube.language.MuleLanguage;

public class MuleProperties {

	private static final String MULE_PROP = "mule.properties";
	private static final String MULE4_PROP = "mule4.properties";
	private static final String MULE3_PROP = "mule3.properties";

	static private Logger logger = LoggerFactory.getLogger(MuleProperties.class);

	static private Map<String, Properties> props = new HashMap<String, Properties>();

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