package com.mulesoft.services.tools.validation;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;

import com.mulesoft.services.xpath.XPathProcessor;

public class Mule4Test {
	Validator val = null;
	String testPath = "src/test/resources/mule4";
	XPathProcessor xpathProcessor;
	SAXBuilder saxBuilder = new SAXBuilder();
	String testDirectory = null;

	@Before
	public void setUp() {
		val = new Validator(Constants.Ruleset.MULE4);
		xpathProcessor = new XPathProcessor().loadNamespaces("namespace-4.properties");
		testDirectory = System.getProperty("user.dir") + File.separator + testPath;
	}

	@Test
	public void testValidateDirectory() throws JAXBException, IOException {
		System.out.println("Working Directory = " + System.getProperty("user.dir"));
		val.validate(new File(testDirectory));
	}

	@Test
	public void testRuleCountSubFlow() throws JDOMException, IOException {
		String rule = "not(count(//mule:mule/mule:sub-flow)>=5)";
		String fileName = testDirectory.concat(File.separator + "ts-mcm-combined-wrapper-impl.xml");
		Document document = saxBuilder.build(new File(fileName));
		Element rootElement = document.getRootElement();
		boolean valid = xpathProcessor.processXPath(rule, rootElement, Boolean.class).booleanValue();
		assertTrue("SUB FLOW MUST BE LESS THAN 5", valid);
	}

	@Test
	public void testRuleDBProperties() throws JDOMException, IOException {
		String rule = "matches(//mule:mule/db:config/db:mssql-connection/@host, '^\\$\\{.*\\}$')";
		String fileName = testDirectory.concat(File.separator + "global.xml");
		Document document = saxBuilder.build(new File(fileName));
		Element rootElement = document.getRootElement();
		boolean valid = xpathProcessor.processXPath(rule, rootElement, Boolean.class).booleanValue();
		assertTrue("DB CONFIG HOST MUST HAVE A PROP PLACEHOLDER", valid);
	}
}
