package com.mulesoft.services.tools.sonarqube.language;

import java.util.Arrays;

import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

/**
 * Declares the Mule “language” for SonarQube so the plugin can register configuration
 * properties and (optionally) participate in language detection.
 *
 * <p>This plugin intentionally defaults {@link #FILE_SUFFIXES_DEFAULT_VALUE} to an empty
 * value to avoid colliding with SonarQube’s built-in XML analyzer on {@code .xml}. Mule
 * XML configuration files are still analyzed by the plugin via its own scanning logic
 * (see {@link #SCAN_FILE_SUFFIXES_KEY}).
 *
 * @author franco.parma
 * @version 1.1.0
 * @since 1.1.0
 * @see com.mulesoft.services.tools.sonarqube.MulePlugin
 */
public class MuleLanguage extends AbstractLanguage {

	protected Configuration config = null;

	public static final String LANGUAGE_NAME = "Mule";
	public static final String LANGUAGE_KEY = "mule";

	public static final String LANGUAGE_MULE4_KEY = "mule4";
	public static final String LANGUAGE_MULE3_KEY = "mule3";

	public static final String FILE_SUFFIXES_KEY = "sonar.mule.file.suffixes";
	/**
	 * File suffixes used for SonarQube language detection.
	 *
	 * Default is ".dwl" so DataWeave sources are shown under the Mule language in SonarQube,
	 * while avoiding conflicts with SonarQube's built-in XML analyzer on ".xml".
	 *
	 * Mule XML configuration files are still analyzed by this plugin via its own scanning logic
	 * (see {@link #SCAN_FILE_SUFFIXES_KEY}).
	 */
	public static final String FILE_SUFFIXES_DEFAULT_VALUE = ".dwl";

	/**
	 * File suffixes used by this plugin to scan Mule config XML files.
	 */
	public static final String SCAN_FILE_SUFFIXES_KEY = "sonar.mule.scan.file.suffixes";
	public static final String SCAN_FILE_SUFFIXES_DEFAULT_VALUE = ".xml";

	/**
	 * Creates a language descriptor backed by SonarQube configuration.
	 *
	 * @param config SonarQube configuration used to resolve file suffixes
	 */
	public MuleLanguage(Configuration config) {
		super("mule", LANGUAGE_NAME);
		this.config = config;
	}

	/**
	 * Returns the file suffixes used by SonarQube to detect Mule language files.
	 *
	 * <p>This is driven by {@link #FILE_SUFFIXES_KEY}. When no suffixes are configured,
	 * the method falls back to {@link #FILE_SUFFIXES_DEFAULT_VALUE}, which is empty by
	 * default to prevent conflicts with the XML analyzer.
	 *
	 * @return an array of configured suffixes, or an empty array when language detection
	 *         should not rely on suffixes
	 */
	@Override
	public String[] getFileSuffixes() {
		String[] suffixes = config.getStringArray(FILE_SUFFIXES_KEY);
		if (suffixes.length == 0) {
			if (FILE_SUFFIXES_DEFAULT_VALUE.trim().isEmpty()) {
				return new String[0];
			}
			suffixes = Arrays.asList(FILE_SUFFIXES_DEFAULT_VALUE.split(",")).toArray(suffixes);
		}
		return suffixes;
	}

}
