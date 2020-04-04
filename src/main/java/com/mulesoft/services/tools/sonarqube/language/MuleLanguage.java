package com.mulesoft.services.tools.sonarqube.language;

import java.util.Arrays;

import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

/**
 * Mule Language Definition
 * 
 * @author franco.parma
 *
 */
public class MuleLanguage extends AbstractLanguage {

	protected Configuration config = null;

	public static final String LANGUAGE_NAME = "Mule";
	public static final String LANGUAGE_KEY = "mule";

	public static final String LANGUAGE_MULE4_KEY = "mule4";
	public static final String LANGUAGE_MULE3_KEY = "mule3";

	public static final String FILE_SUFFIXES_KEY = "sonar.mule.file.suffixes";
	public static final String FILE_SUFFIXES_DEFAULT_VALUE = ".xml";

	public MuleLanguage(Configuration config) {
		super("mule", LANGUAGE_NAME);
		this.config = config;
	}

	@Override
	public String[] getFileSuffixes() {
		String[] suffixes = config.getStringArray(FILE_SUFFIXES_KEY);
		if (suffixes.length == 0) {
			suffixes = Arrays.asList(FILE_SUFFIXES_DEFAULT_VALUE.split(",")).toArray(suffixes);
		}
		return suffixes;
	}

}
