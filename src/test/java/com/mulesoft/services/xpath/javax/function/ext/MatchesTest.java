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

public class MatchesTest extends BaseFunctionTest {
	@Test
	public void testOnNodeList() throws IOException, XPathExpressionException {
		String expression = "//mule:on-error-propagate/@type[f:matches(., 'APIKIT:NOT_.*')]";

		NodeList results = (NodeList)evaluateOnFixture("ts-mcm-combined-wrapper.xml", expression, XPathConstants.NODESET);

		List<String> attrValues = XmlFile.asList(results).stream().map(node -> ((Attr)node).getValue()).collect(Collectors.toList());
		assertEquals(Arrays.asList("APIKIT:NOT_FOUND", "APIKIT:NOT_ACCEPTABLE", "APIKIT:NOT_IMPLEMENTED", "APIKIT:NOT_FOUND"), attrValues);
	}

	@Test
	public void testOnStringWhenMatching() throws IOException, XPathExpressionException {
		String expression = "f:matches('${mule.api.id}', '^\\$\\{.*\\}$')";

		Boolean result = (Boolean)evaluate(expression, XPathConstants.BOOLEAN);

		assertEquals("expected string to match regex", true, result);
	}

	@Test
	public void testOnStringWhenNotMatching() throws IOException, XPathExpressionException {
		String expression = "f:matches('i_was_hardcoded', '^\\$\\{.*\\}$')";

		Boolean result = (Boolean)evaluate(expression, XPathConstants.BOOLEAN);

		assertEquals("expected string to not match regex", false, result);
	}
}