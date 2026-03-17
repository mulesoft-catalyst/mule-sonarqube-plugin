package com.mulesoft.services.xpath;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.jdom2.Content;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.mulesoft.services.xpath.javax.function.SonarFunctionResolver;
import com.mulesoft.services.xpath.javax.util.MappedNamespaceContext;
import com.mulesoft.services.xpath.jaxen.function.ext.MatchesFunction;
import com.mulesoft.services.xpath.jaxen.function.ext.IsConfigurableFunction;

public class XPathProcessor {
	Logger logger = LoggerFactory.getLogger(getClass());

	private static final String CLASSPATH_PREFIX = "classpath:";

	private final javax.xml.xpath.XPathFactory jaxpFactory = javax.xml.xpath.XPathFactory.newInstance();

	private Properties namespacesProperties = new Properties();
	private Map<String, String> namespacesByPrefix = new TreeMap<>();

	private List<Namespace> namespace = new ArrayList<Namespace>();

	public XPathProcessor() {
		MatchesFunction.registerSelfInSimpleContext();
		IsConfigurableFunction.registerSelfInSimpleContext();
	}

	public XPathProcessor loadNamespaces(String resourceName) {
		try (InputStream stream = XPathProcessor.class.getClassLoader().getResourceAsStream(resourceName)) {
			if (stream == null) {
				logger.warn("Namespace resource not found: {}", resourceName);
				return this;
			}
			return loadNamespaces(stream);

		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return this;
	}

	public XPathProcessor loadNamespacesSpec(String spec) {
		if (spec == null || spec.trim().isEmpty()) {
			return this;
		}
		String trimmed = spec.trim();
		try (InputStream stream = openSpec(trimmed)) {
			return loadNamespaces(stream);
		} catch (IOException e) {
			logger.error("Failed to load namespaces from spec={}", trimmed, e);
			return this;
		}
	}

	private XPathProcessor loadNamespaces(InputStream stream) throws IOException {
		Properties p = new Properties();
		p.load(stream);
		p.forEach((Object t, Object u) -> {
			String prefix = t.toString();
			String uri = u.toString();
			namespacesByPrefix.put(prefix, uri);
			namespace.add(Namespace.getNamespace(prefix, uri));
		});
		return this;
	}

	private static InputStream openSpec(String spec) throws IOException {
		if (spec.startsWith(CLASSPATH_PREFIX)) {
			String resourceName = spec.substring(CLASSPATH_PREFIX.length());
			if (resourceName.startsWith("/")) {
				resourceName = resourceName.substring(1);
			}
			InputStream stream = XPathProcessor.class.getClassLoader().getResourceAsStream(resourceName);
			if (stream == null) {
				throw new IOException("Classpath resource not found: " + resourceName);
			}
			return stream;
		}
		if (!spec.contains(":")) {
			return new FileInputStream(spec);
		}
		if (spec.startsWith("file:")) {
			return new URL(spec).openConnection().getInputStream();
		}
		// Namespace extension specs must be local-only to prevent SSRF.
		throw new IOException("Remote URL schemes are not allowed for namespace properties: " + spec);
	}

	@SuppressWarnings("unchecked")
	public <T> T processXPath(String xpathExpression, Content target, Class<T> type) {
		if (logger.isDebugEnabled()) {
			logger.debug("Evaluating:" + xpathExpression);
		}
		XPathFactory xpfac = XPathFactory.instance();
		if (Boolean.class.equals(type)) {
			XPathExpression<Boolean> xp = xpfac.compile(xpathExpression, Filters.fboolean(), Collections.emptyMap(),
					namespace);
			return (T) xp.evaluateFirst(target);
		}
		if (Double.class.equals(type) || Number.class.equals(type)) {
			XPathExpression<Object> xp = xpfac.compile(xpathExpression, Filters.fpassthrough(), Collections.emptyMap(),
					namespace);
			Object v = xp.evaluateFirst(target);
			if (v instanceof Number) {
				return (T) Double.valueOf(((Number) v).doubleValue());
			}
			if (v instanceof String) {
				try {
					return (T) Double.valueOf(Double.parseDouble((String) v));
				} catch (NumberFormatException e) {
					return null;
				}
			}
			return null;
		}
		if (String.class.equals(type)) {
			XPathExpression<String> xp = xpfac.compile(xpathExpression, Filters.fstring(), Collections.emptyMap(),
					namespace);
			if(logger.isDebugEnabled()) {
				logger.debug("Evaluating Result: " + xp.evaluateFirst(target));
			}
			return (T) xp.evaluateFirst(target);
		}

		XPathExpression<Object> xp = xpfac.compile(xpathExpression, Filters.fpassthrough(), Collections.emptyMap(),
				namespace);
		return (T) xp.evaluateFirst(target);
	}

	public Map<String, String> getNamespacesByPrefix() {
		return Collections.unmodifiableMap(namespacesByPrefix);
	}

	public Object processXPathAsNodeSet(String xpathExpression, Node target, QName returnType)
			throws XPathExpressionException {
		if (logger.isDebugEnabled()) {
			logger.debug("Evaluating (javax.xml.xpath): {}", xpathExpression);
		}
		XPath xpath = jaxpFactory.newXPath();
		xpath.setNamespaceContext(new MappedNamespaceContext(namespacesByPrefix));
		xpath.setXPathFunctionResolver(new SonarFunctionResolver());
		javax.xml.xpath.XPathExpression expression = xpath.compile(xpathExpression);
		return expression.evaluate(target, returnType);
	}

}