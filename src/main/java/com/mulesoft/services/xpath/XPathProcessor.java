package com.mulesoft.services.xpath;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import com.mulesoft.services.xpath.javax.function.SonarFunctionResolver;
import com.mulesoft.services.xpath.javax.util.MappedNamespaceContext;
import com.mulesoft.services.xpath.jaxen.function.ext.IsConfigurableFunction;
import com.mulesoft.services.xpath.jaxen.function.ext.MatchesFunction;

import org.jdom2.Content;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

public class XPathProcessor {
	Logger logger = LoggerFactory.getLogger(getClass());
	javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();

	private Properties namespacesProperties = new Properties();

	private List<Namespace> namespaces = new ArrayList<Namespace>();
	private Map<String, String> prefixToNamespace = new HashMap<>();

	public XPathProcessor() {
		MatchesFunction.registerSelfInSimpleContext();
		IsConfigurableFunction.registerSelfInSimpleContext();
	}

	public XPathProcessor loadNamespaces(String resourceName) {
		try (InputStream stream = XPathProcessor.class.getClassLoader().getResourceAsStream(resourceName)) {
			namespacesProperties.load(stream);
			namespacesProperties
					.forEach((Object t, Object u) -> {
						namespaces.add(Namespace.getNamespace(t.toString(), u.toString()));
						prefixToNamespace.put(t.toString(), u.toString());
					});

		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return this;
	}

	// javax.xpath
	public Object processXPathAsNodeSet(String xpathExpression, Node target, QName returnType) throws XPathExpressionException {
		if (logger.isDebugEnabled()) {
			logger.debug("Evaluating as elements: " + xpathExpression);
		}
		XPath xpath = factory.newXPath();
		xpath.setNamespaceContext(new MappedNamespaceContext(prefixToNamespace));
		xpath.setXPathFunctionResolver(new SonarFunctionResolver());
		javax.xml.xpath.XPathExpression expression = xpath.compile(xpathExpression);
		return expression.evaluate(target, returnType);
	}

	// jdom2
	@SuppressWarnings("unchecked")
	public <T> T processXPath(String xpathExpression, Content target, Class<T> type) {
		if (logger.isDebugEnabled()) {
			logger.debug("Evaluating:" + xpathExpression);
		}
		XPathFactory xpfac = XPathFactory.instance();
		@SuppressWarnings("rawtypes")
		XPathExpression xp = xpfac.compile(xpathExpression, Filters.fpassthrough(), Collections.emptyMap(), namespaces);

		return (T) xp.evaluateFirst(target);
	}
}