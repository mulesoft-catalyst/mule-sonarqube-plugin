package com.mulesoft.services.tools.sonarqube.xml;

import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a hardened SAXBuilder to prevent XXE / external entity processing.
 *
 * This is critical because the plugin parses project XML files which may be untrusted in PR analyses.
 *
 * @version 1.1.0
 * @since 1.1.0
 * @see SecureJaxp
 */
public final class SecureSaxBuilder {
	private static final Logger logger = LoggerFactory.getLogger(SecureSaxBuilder.class);

	private SecureSaxBuilder() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Creates a {@link SAXBuilder} with entity expansion disabled and common XXE-related features
	 * set to a secure configuration.
	 *
	 * <p>If any required feature cannot be applied, the method fails closed by throwing an exception.
	 *
	 * @return a hardened SAX builder
	 */
	public static SAXBuilder create() {
		SAXBuilder builder = new SAXBuilder();

		// Prevent expansion of internal entities.
		builder.setExpandEntities(false);

		// Disable DTDs and external entities (XXE).
		// These features are supported by the common Xerces implementation; if any cannot be set we fail closed.
		setFeatureOrThrow(builder, "http://apache.org/xml/features/disallow-doctype-decl", true);
		setFeatureOrThrow(builder, "http://xml.org/sax/features/external-general-entities", false);
		setFeatureOrThrow(builder, "http://xml.org/sax/features/external-parameter-entities", false);
		setFeatureOrThrow(builder, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

		return builder;
	}

	/**
	 * Sets a parser feature on the provided builder or throws an exception when unsupported.
	 *
	 * @param builder SAX builder to configure
	 * @param feature SAX/Xerces feature URI
	 * @param value feature value to set
	 * @throws IllegalStateException when the underlying parser does not support the feature
	 */
	private static void setFeatureOrThrow(SAXBuilder builder, String feature, boolean value) {
		try {
			builder.setFeature(feature, value);
		} catch (Exception e) {
			logger.error("Failed to set XML parser feature {}={}", feature, value, e);
			throw new IllegalStateException("Cannot harden XML parser (feature unsupported): " + feature, e);
		}
	}
}

