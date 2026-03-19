package com.mulesoft.services.tools.validation;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;

import com.mulesoft.services.xpath.XPathProcessor;

/**
 * Integration-style tests for Mule 4 rule evaluation and XPath extensions.
 *
 * <p>These tests use representative Mule 4 XML fixtures to validate rule expressions (count/matches)
 * and verify the custom Jaxen extension function {@code is-configurable()}.
 *
 * @version 1.1.0
 * @since 1.1.0
 * @see XPathProcessor
 */
public class Mule4Test {
	Validator val = null;
	String testPath = "src/test/resources/mule4";
	XPathProcessor xpathProcessor;
	SAXBuilder saxBuilder = new SAXBuilder();
	String testDirectory = null;

	/**
	 * Initializes the Mule 4 validator and loads Mule 4 namespace mappings for XPath evaluation.
	 */
	@Before
	public void setUp() {
		val = new Validator(Constants.Ruleset.MULE4);
		xpathProcessor = new XPathProcessor().loadNamespaces("namespace-4.properties");
		testDirectory = System.getProperty("user.dir") + File.separator + testPath;
	}


	/**
	 * Asserts a rule that limits the number of Mule sub-flows in a file.
	 *
	 * @throws JDOMException when XML parsing fails
	 * @throws IOException when the fixture cannot be read
	 */
	@Test
	public void testRuleCountSubFlow() throws JDOMException, IOException {
		String rule = "not(count(//mule:mule/mule:sub-flow)>=5)";
		String fileName = testDirectory.concat(File.separator + "ts-mcm-combined-wrapper-impl.xml");
		Document document = saxBuilder.build(new File(fileName));
		Element rootElement = document.getRootElement();
		boolean valid = xpathProcessor.processXPath(rule, rootElement, Boolean.class).booleanValue();
		assertTrue("SUB FLOW MUST BE LESS THAN 5", valid);
	}

	/**
	 * Asserts a rule that requires database connection hostnames to be configured via placeholders.
	 *
	 * @throws JDOMException when XML parsing fails
	 * @throws IOException when the fixture cannot be read
	 */
	@Test
	public void testRuleDBProperties() throws JDOMException, IOException {
		String rule = "matches(//mule:mule/db:config/db:mssql-connection/@host, '^\\$\\{.*\\}$')";
		String fileName = testDirectory.concat(File.separator + "global.xml");
		Document document = saxBuilder.build(new File(fileName));
		Element rootElement = document.getRootElement();
		boolean valid = xpathProcessor.processXPath(rule, rootElement, Boolean.class).booleanValue();
		assertTrue("DB CONFIG HOST MUST HAVE A PROP PLACEHOLDER", valid);
	}
	
	/**
	 * Asserts a domain configuration rule that ensures response timeouts are configurable placeholders.
	 *
	 * @throws JDOMException when XML parsing fails
	 * @throws IOException when the fixture cannot be read
	 */
	@Test
	public void testDomain() throws JDOMException, IOException {
		String rule = "count(//domain:mule-domain/http:request-config[not(@responseTimeout) or not(matches(@responseTimeout,'^\\$\\{.*\\}$'))]) = 0";
		String fileName = testDirectory.concat(File.separator + "mule-domain-config.xml");
		Document document = saxBuilder.build(new File(fileName));
		Element rootElement = document.getRootElement();
		boolean valid = xpathProcessor.processXPath(rule, rootElement, Boolean.class).booleanValue();
		assertTrue("HTTP Requestor Configuration should have a configurable Response Timeout", valid);
	}

	/**
	 * Asserts the legacy Jaxen extension function {@code is-configurable()} returns true for placeholder values.
	 *
	 * @throws JDOMException when XML parsing fails
	 * @throws IOException when the fixture cannot be read
	 */
	@Test
	public void testIsConfigurable() throws JDOMException, IOException {
		String rule = "is-configurable(//mule:mule/secure-properties:config/@key)";
		String fileName = testDirectory.concat(File.separator + "global.xml");
		Document document = saxBuilder.build(new File(fileName));
		Element rootElement = document.getRootElement();
		boolean valid = xpathProcessor.processXPath(rule, rootElement, Boolean.class).booleanValue();
		assertTrue("MULE CREDENTIALS MUST BE CONFIGURABLE", valid);
	}
}
