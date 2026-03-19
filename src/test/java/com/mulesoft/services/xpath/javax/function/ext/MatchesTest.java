package com.mulesoft.services.xpath.javax.function.ext;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.junit.Test;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.w3c.dom.Attr;
import org.w3c.dom.NodeList;

/**
 * Verifies the behavior of the custom XPath function {@code f:matches} used by v1.1 rules.
 *
 * <p>The tests cover both supported signatures:
 * <ul>
 *   <li>{@code f:matches(node-set, 'regex')} → filtered node-set</li>
 *   <li>{@code f:matches('string', 'regex')} → boolean</li>
 * </ul>
 *
 * @version 1.1.0
 * @since 1.1.0
 * @see com.mulesoft.services.xpath.javax.function.ext.Matches
 */
public class MatchesTest extends BaseFunctionTest {
	/**
	 * Ensures {@code f:matches} filters a node-set predicate to only matching attributes.
	 *
	 * @throws IOException when the XML fixture cannot be read
	 * @throws XPathExpressionException when the XPath expression cannot be compiled/evaluated
	 */
	@Test
	public void testOnNodeList() throws IOException, XPathExpressionException {
		String expression = "//mule:on-error-propagate/@type[f:matches(., 'APIKIT:NOT_.*')]";

		NodeList results = (NodeList) evaluateOnFixture("ts-mcm-combined-wrapper.xml", expression,
				XPathConstants.NODESET);

		List<String> attrValues = XmlFile.asList(results).stream().map(node -> ((Attr) node).getValue())
				.collect(Collectors.toList());
		assertEquals(Arrays.asList("APIKIT:NOT_FOUND", "APIKIT:NOT_ACCEPTABLE", "APIKIT:NOT_IMPLEMENTED",
				"APIKIT:NOT_FOUND"), attrValues);
	}

	/**
	 * Ensures {@code f:matches} returns {@code true} when the input string matches the regex.
	 *
	 * @throws IOException when test setup fails to load namespaces
	 * @throws XPathExpressionException when the XPath expression cannot be compiled/evaluated
	 */
	@Test
	public void testOnStringWhenMatching() throws IOException, XPathExpressionException {
		String expression = "f:matches('${mule.api.id}', '^\\$\\{.*\\}$')";

		Boolean result = (Boolean) evaluate(expression, XPathConstants.BOOLEAN);

		assertEquals("expected string to match regex", true, result);
	}

	/**
	 * Ensures {@code f:matches} returns {@code false} when the input string does not match the regex.
	 *
	 * @throws IOException when test setup fails to load namespaces
	 * @throws XPathExpressionException when the XPath expression cannot be compiled/evaluated
	 */
	@Test
	public void testOnStringWhenNotMatching() throws IOException, XPathExpressionException {
		String expression = "f:matches('i_was_hardcoded', '^\\$\\{.*\\}$')";

		Boolean result = (Boolean) evaluate(expression, XPathConstants.BOOLEAN);

		assertEquals("expected string to not match regex", false, result);
	}
}

