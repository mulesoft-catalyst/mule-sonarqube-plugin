package com.mulesoft.services.tools.sonarqube.language;

import java.util.Arrays;

import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

/*
 * 
 */
public class RAMLLanguage extends AbstractLanguage {

	protected Configuration config = null;

	public static final String LANGUAGE_NAME = "RAML";
	public static final String LANGUAGE_KEY = "raml";

	public static final String FILE_SUFFIXES_KEY = "sonar.raml.file.suffixes";
	public static final String FILE_SUFFIXES_DEFAULT_VALUE = ".raml";

	public RAMLLanguage(Configuration config) {
		super(LANGUAGE_KEY, LANGUAGE_NAME);
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
