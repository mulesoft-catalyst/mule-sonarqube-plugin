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
 */
public class SonarFunctionResolver implements XPathFunctionResolver {
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

