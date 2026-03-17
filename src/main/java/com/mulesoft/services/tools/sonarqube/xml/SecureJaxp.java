package com.mulesoft.services.tools.sonarqube.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Best-effort hardening for JAXP-based XML processing (DOM/XPath/etc).
 *
 * This complements {@link SecureSaxBuilder} (JDOM) by restricting external entity access in JAXP.
 */
public final class SecureJaxp {
	private static final Logger logger = LoggerFactory.getLogger(SecureJaxp.class);

	private SecureJaxp() {
		throw new IllegalStateException("Utility class");
	}

	public static void harden() {
		// Restrict external entity / schema access for JAXP parsers (helps prevent XXE/SSRF).
		setIfAbsent("javax.xml.accessExternalDTD", "");
		setIfAbsent("javax.xml.accessExternalSchema", "");
		setIfAbsent("javax.xml.accessExternalStylesheet", "");

		// Limit entity expansion to reduce DoS impact. Keep conservative defaults if user has set them.
		setIfAbsent("jdk.xml.entityExpansionLimit", "1000");
		setIfAbsent("jdk.xml.totalEntitySizeLimit", "5000000");
		setIfAbsent("jdk.xml.maxGeneralEntitySizeLimit", "0");
		setIfAbsent("jdk.xml.maxParameterEntitySizeLimit", "0");
	}

	private static void setIfAbsent(String key, String value) {
		String existing = System.getProperty(key);
		if (existing == null) {
			System.setProperty(key, value);
			logger.debug("Set system property {}={}", key, value);
		}
	}
}

