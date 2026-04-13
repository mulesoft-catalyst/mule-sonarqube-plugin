package com.mulesoft.services.xpath.javax.function;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionResolver;

import com.mulesoft.services.xpath.javax.function.ext.IsConfigurable;
import com.mulesoft.services.xpath.javax.function.ext.Matches;

/**
 * XPath function resolver for Java's built-in XPath implementation.
 *
 * Rules use these functions in the namespace prefix {@code f}, e.g. {@code f:matches(...)}.
 *
 * @version 1.1.0
 * @since 1.1.0
 */
public class SonarFunctionResolver implements XPathFunctionResolver {
	/**
	 * Resolves a custom XPath function by name and arity.
	 *
	 * @param functionName qualified function name
	 * @param arity number of arguments
	 * @return an {@link XPathFunction} implementation, or null when the function is not supported
	 */
	@Override
	public XPathFunction resolveFunction(QName functionName, int arity) {
		switch (functionName.getLocalPart()) {
		case "matches":
			return new Matches();
		case "isConfigurable":
			return new IsConfigurable();
		case "isNotConfigurable":
			return new IsConfigurable(true);
		default:
			return null;
		}
	}
}

