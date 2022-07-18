package com.mulesoft.services.xpath.javax.function.ext;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.junit.Test;
import org.w3c.dom.Attr;

public class IsConfigurableTest extends BaseFunctionTest {
	@Test
	public void testInPredicate() throws IOException, XPathExpressionException {
		String expression = "//salesforce:cached-basic-connection//@*[f:isConfigurable(.)]";

		List<String> results = evaluateAsNodeListOnFixture("global.xml", expression, node -> ((Attr)node).getValue());

		assertEquals(Arrays.asList("${secure::sfdc.password}", "${secure::sfdc.token}", "${secure::sfdc.user}"), results);
	}
	
	@Test
	public void testInvertedInPredicate() throws IOException, XPathExpressionException {
		String expression = "//salesforce:cached-basic-connection//@*[f:isNotConfigurable(.)]";

		List<String> results = evaluateAsNodeListOnFixture("global.xml", expression, node -> ((Attr)node).getValue());

		assertEquals(Arrays.asList("https://test.salesforce.com/services/Soap/u/46.0"), results);
	}

	@Test
	public void testOnNodeSet() throws IOException, XPathExpressionException {
		String expression = "f:isConfigurable(//salesforce:cached-basic-connection//@*)";

		List<String> results = evaluateAsNodeListOnFixture("global.xml", expression, node -> ((Attr)node).getValue());
		
		assertEquals(Arrays.asList("${secure::sfdc.password}", "${secure::sfdc.token}", "${secure::sfdc.user}"), results);
	}

	@Test
	public void testInvertedOnNodeSet() throws IOException, XPathExpressionException {
		String expression = "f:isNotConfigurable(//salesforce:cached-basic-connection//@*)";

		List<String> results = evaluateAsNodeListOnFixture("global.xml", expression, node -> ((Attr)node).getValue());

		assertEquals(Arrays.asList("https://test.salesforce.com/services/Soap/u/46.0"), results);
	}

	@Test
	public void testOnStringWhenMatching() throws IOException, XPathExpressionException {
		String expression = "f:isConfigurable('${mule.api.id}')";

		Boolean result = (Boolean)evaluate(expression, XPathConstants.BOOLEAN);

		assertEquals("expected string to be configurable", true, result);
	}

	@Test
	public void testInvertedOnStringWhenMatching() throws IOException, XPathExpressionException {
		String expression = "f:isNotConfigurable('${mule.api.id}')";

		Boolean result = (Boolean)evaluate(expression, XPathConstants.BOOLEAN);

		assertEquals("expected string to not be not configurable", false, result);
	}

	@Test
	public void testOnStringWhenNotMatching() throws IOException, XPathExpressionException {
		String expression = "f:isConfigurable('i_was_hardcoded')";

		Boolean result = (Boolean)evaluate(expression, XPathConstants.BOOLEAN);

		assertEquals("expected string to not be configurable", false, result);
	}

	@Test
	public void testInvertedOnStringWhenNotMatching() throws IOException, XPathExpressionException {
		String expression = "f:isNotConfigurable('i_was_hardcoded')";

		Boolean result = (Boolean)evaluate(expression, XPathConstants.BOOLEAN);

		assertEquals("expected string to be not configurable", true, result);
	}
}