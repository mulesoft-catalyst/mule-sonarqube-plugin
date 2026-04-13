package com.mulesoft.services.tools.sonarqube.rule.scope;

import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.sonarsource.analyzer.commons.xml.checks.SonarXmlCheck;

/**
 * Minimal {@link SonarXmlCheck} implementation used to bootstrap Sonar XML check infrastructure.
 *
 * <p>This helper is used in places where the plugin wants to reuse Sonar XML parsing and location
 * utilities (for example, to initialize internal state needed to compute issue locations) without
 * actually performing a check.
 *
 * @version 1.1.0
 * @since 1.1.0
 * @see FileStrategyScope
 */
public class SonarXmlCheckHelper extends SonarXmlCheck {

	/**
	 * No-op scan implementation.
	 *
	 * @param aFile the XML file being scanned
	 */
	@Override
	public void scanFile(XmlFile aFile) {
		// nothing to do
	}

}
