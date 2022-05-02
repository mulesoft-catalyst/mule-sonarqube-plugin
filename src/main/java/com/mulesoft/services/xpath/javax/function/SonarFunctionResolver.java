package com.mulesoft.services.xpath.javax.function;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionResolver;

import com.mulesoft.services.xpath.javax.function.ext.IsConfigurable;
import com.mulesoft.services.xpath.javax.function.ext.Matches;

public class SonarFunctionResolver implements XPathFunctionResolver {
    /**
     * Custom functions must be in a namespace in order to be recognized.
     * It is not clear if this is part of the standard or just a consequence
     * of Java's built-in XPath implementation.
     * 
     * For convienence, the namespace <code>f</code> has been defined and can
     * be used by rules, for example:
     * <pre>
     * //mule:mule/api-gateway:autodiscovery/@apiId[f:isNotConfigurable(.)]
     * </pre>
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