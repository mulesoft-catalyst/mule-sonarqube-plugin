package com.mulesoft.services.xpath.javax.function.ext;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.junit.Test;
import org.w3c.dom.Attr;

/**
 * Verifies the behavior of the custom XPath functions {@code f:isConfigurable} and
 * {@code f:isNotConfigurable} used by v1.1 rules.
 *
 * <p>The tests cover predicate usage and node-set usage against representative Mule 4 XML fixtures.
 *
 * @version 1.1.0
 * @since 1.1.0
 * @see com.mulesoft.services.xpath.javax.function.ext.IsConfigurable
 */
public class IsConfigurableTest extends BaseFunctionTest {
	/**
	 * Verifies {@code f:isConfigurable} can be used inside an XPath predicate to filter attributes.
	 *
	 * @throws IOException when the XML fixture cannot be read
	 * @throws XPathExpressionException when the XPath expression cannot be compiled/evaluated
	 */
	@Test
	public void testInPredicate() throws IOException, XPathExpressionException {
		String expression = "//salesforce:cached-basic-connection//@*[f:isConfigurable(.)]";

		List<String> results = evaluateAsNodeListOnFixture("global.xml", expression, node -> ((Attr) node).getValue());

		assertEquals(Arrays.asList("${secure::sfdc.password}", "${secure::sfdc.token}", "${secure::sfdc.user}"),
				results);
	}

	/**
	 * Verifies {@code f:isNotConfigurable} in predicate form returns only hardcoded values.
	 *
	 * @throws IOException when the XML fixture cannot be read
	 * @throws XPathExpressionException when the XPath expression cannot be compiled/evaluated
	 */
	@Test
	public void testInvertedInPredicate() throws IOException, XPathExpressionException {
		String expression = "//salesforce:cached-basic-connection//@*[f:isNotConfigurable(.)]";

		List<String> results = evaluateAsNodeListOnFixture("global.xml", expression, node -> ((Attr) node).getValue());

		assertEquals(Arrays.asList("https://test.salesforce.com/services/Soap/u/46.0"), results);
	}

	/**
	 * Verifies {@code f:isConfigurable(node-set)} returns a filtered node-set.
	 *
	 * @throws IOException when the XML fixture cannot be read
	 * @throws XPathExpressionException when the XPath expression cannot be compiled/evaluated
	 */
	@Test
	public void testOnNodeSet() throws IOException, XPathExpressionException {
		String expression = "f:isConfigurable(//salesforce:cached-basic-connection//@*)";

		List<String> results = evaluateAsNodeListOnFixture("global.xml", expression, node -> ((Attr) node).getValue());

		assertEquals(Arrays.asList("${secure::sfdc.password}", "${secure::sfdc.token}", "${secure::sfdc.user}"),
				results);
	}

	/**
	 * Verifies {@code f:isNotConfigurable(node-set)} returns only hardcoded values.
	 *
	 * @throws IOException when the XML fixture cannot be read
	 * @throws XPathExpressionException when the XPath expression cannot be compiled/evaluated
	 */
	@Test
	public void testInvertedOnNodeSet() throws IOException, XPathExpressionException {
		String expression = "f:isNotConfigurable(//salesforce:cached-basic-connection//@*)";

		List<String> results = evaluateAsNodeListOnFixture("global.xml", expression, node -> ((Attr) node).getValue());

		assertEquals(Arrays.asList("https://test.salesforce.com/services/Soap/u/46.0"), results);
	}

	/**
	 * Verifies {@code f:isConfigurable('string')} returns {@code true} for placeholder values.
	 *
	 * @throws IOException when test setup fails to load namespaces
	 * @throws XPathExpressionException when the XPath expression cannot be compiled/evaluated
	 */
	@Test
	public void testOnStringWhenMatching() throws IOException, XPathExpressionException {
		String expression = "f:isConfigurable('${mule.api.id}')";

		Boolean result = (Boolean) evaluate(expression, XPathConstants.BOOLEAN);

		assertEquals("expected string to be configurable", true, result);
	}

	/**
	 * Verifies {@code f:isNotConfigurable('string')} inverts the placeholder predicate.
	 *
	 * @throws IOException when test setup fails to load namespaces
	 * @throws XPathExpressionException when the XPath expression cannot be compiled/evaluated
	 */
	@Test
	public void testInvertedOnStringWhenMatching() throws IOException, XPathExpressionException {
		String expression = "f:isNotConfigurable('${mule.api.id}')";

		Boolean result = (Boolean) evaluate(expression, XPathConstants.BOOLEAN);

		assertEquals("expected string to not be not configurable", false, result);
	}

	/**
	 * Verifies {@code f:isConfigurable('string')} returns {@code false} for hardcoded values.
	 *
	 * @throws IOException when test setup fails to load namespaces
	 * @throws XPathExpressionException when the XPath expression cannot be compiled/evaluated
	 */
	@Test
	public void testOnStringWhenNotMatching() throws IOException, XPathExpressionException {
		String expression = "f:isConfigurable('i_was_hardcoded')";

		Boolean result = (Boolean) evaluate(expression, XPathConstants.BOOLEAN);

		assertEquals("expected string to not be configurable", false, result);
	}

	/**
	 * Verifies {@code f:isNotConfigurable('string')} returns {@code true} for hardcoded values.
	 *
	 * @throws IOException when test setup fails to load namespaces
	 * @throws XPathExpressionException when the XPath expression cannot be compiled/evaluated
	 */
	@Test
	public void testInvertedOnStringWhenNotMatching() throws IOException, XPathExpressionException {
		String expression = "f:isNotConfigurable('i_was_hardcoded')";

		Boolean result = (Boolean) evaluate(expression, XPathConstants.BOOLEAN);

		assertEquals("expected string to be not configurable", true, result);
	}
}

