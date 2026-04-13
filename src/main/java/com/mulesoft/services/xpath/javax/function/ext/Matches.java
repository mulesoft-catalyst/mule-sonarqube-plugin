package com.mulesoft.services.xpath.javax.function.ext;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.w3c.dom.NodeList;

import com.mulesoft.services.xpath.javax.util.NodeLists;

/**
 * XPath extension function {@code f:matches} for JAXP-based XPath evaluation.
 *
 * <p>Supported signatures:
 * <ul>
 *   <li>{@code f:matches(node-set, 'regex')} → returns a filtered node-set whose text content matches</li>
 *   <li>{@code f:matches('string', 'regex')} → returns a boolean</li>
 * </ul>
 *
 * @version 1.1.0
 * @since 1.1.0
 * @see com.mulesoft.services.xpath.javax.function.SonarFunctionResolver
 */
public class Matches implements XPathFunction {
	private static final Logger logger = LoggerFactory.getLogger(Matches.class);

	/**
	 * Evaluates the function against the provided arguments.
	 *
	 * @param args function arguments (see supported signatures in class Javadoc)
	 * @return filtered node-set or boolean, depending on argument types
	 * @throws XPathFunctionException when called with unsupported argument types
	 */
	@Override
	public Object evaluate(List<?> args) throws XPathFunctionException {
		Object arg0 = args.get(0);
		Object arg1 = args.get(1);
		if (arg0 instanceof NodeList && arg1 instanceof String) {
			NodeList nodeList = (NodeList) arg0;
			String regex = (String) arg1;
			logger.debug("matches: input nodeset size={}", nodeList.getLength());
			return NodeLists.asNodeList(XmlFile.asList(nodeList).stream().filter(node -> {
				String textContent = node.getTextContent();
				return Pattern.matches(regex, textContent);
			}).collect(Collectors.toList()));
		} else if (arg0 instanceof String && arg1 instanceof String) {
			String string = (String) arg0;
			String regex = (String) arg1;
			return Pattern.matches(regex, string);
		} else {
			throw new XPathFunctionException(String.format("f:matches expected (NodeSet, String) or (String, String), got (%s, %s)",
					arg0.getClass().getSimpleName(), arg1.getClass().getSimpleName()));
		}
	}
}

