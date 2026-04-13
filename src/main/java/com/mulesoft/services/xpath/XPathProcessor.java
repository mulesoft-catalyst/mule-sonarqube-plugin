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

/**
 * Evaluates XPath expressions against Mule XML using either JDOM (Jaxen) or JAXP (W3C DOM).
 *
 * <p>This class centralizes namespace handling and custom XPath functions used by Mule validation
 * rules. It supports two evaluation paths:
 * <ul>
 *   <li><b>JDOM/Jaxen</b> via {@link #processXPath(String, Content, Class)} (used by legacy strategies)</li>
 *   <li><b>JAXP</b> via {@link #processXPathAsNodeSet(String, Node, QName)} (used by plugin version {@code 1.1})</li>
 * </ul>
 *
 * <p>Namespaces are loaded from properties files (for example {@code namespace-3.properties}) and can
 * be extended by users via {@link #loadNamespacesSpec(String)}. To reduce SSRF risk, namespace extension
 * specs only allow local file/classpath sources.
 *
 * @version 1.1.0
 * @since 1.1.0
 * @see com.mulesoft.services.tools.sonarqube.sensor.SonarRuleConsumer
 */
public class XPathProcessor {
	Logger logger = LoggerFactory.getLogger(getClass());

	private static final String CLASSPATH_PREFIX = "classpath:";

	private final javax.xml.xpath.XPathFactory jaxpFactory = javax.xml.xpath.XPathFactory.newInstance();

	private Properties namespacesProperties = new Properties();
	private Map<String, String> namespacesByPrefix = new TreeMap<>();

	private List<Namespace> namespace = new ArrayList<Namespace>();

	/**
	 * Creates a processor and registers custom XPath extension functions for Jaxen evaluation.
	 */
	public XPathProcessor() {
		MatchesFunction.registerSelfInSimpleContext();
		IsConfigurableFunction.registerSelfInSimpleContext();
	}

	/**
	 * Loads namespace mappings from a classpath resource and makes them available for XPath evaluation.
	 *
	 * @param resourceName classpath resource name (for example {@code "namespace-4.properties"})
	 * @return this processor for chaining
	 */
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

	/**
	 * Loads additional namespace mappings from a spec.
	 *
	 * <p>Supported formats:
	 * <ul>
	 *   <li>{@code classpath:...}</li>
	 *   <li>{@code file:...}</li>
	 *   <li>plain filesystem path</li>
	 * </ul>
	 *
	 * @param spec location of a properties file containing namespace mappings (prefix=namespaceURI)
	 * @return this processor for chaining
	 */
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

	/**
	 * Loads namespaces from the given properties stream.
	 *
	 * @param stream input stream to read properties from
	 * @return this processor for chaining
	 * @throws IOException when the stream cannot be read
	 */
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

	/**
	 * Opens an input stream for a namespace properties spec.
	 *
	 * <p>Remote URL schemes are explicitly disallowed to prevent SSRF during analysis.
	 *
	 * @param spec spec string (classpath/file/path)
	 * @return an input stream
	 * @throws IOException when the resource cannot be opened or the scheme is not allowed
	 */
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
	/**
	 * Evaluates an XPath expression against a JDOM target using Jaxen.
	 *
	 * <p>Supported return types:
	 * <ul>
	 *   <li>{@link Boolean}</li>
	 *   <li>{@link Double} / {@link Number}</li>
	 *   <li>{@link String}</li>
	 * </ul>
	 *
	 * @param xpathExpression XPath expression to evaluate
	 * @param target JDOM target node/content
	 * @param type desired result type
	 * @param <T> result type
	 * @return the evaluated value (may be null for non-numeric parses)
	 */
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

	/**
	 * Returns an immutable mapping of loaded namespace prefixes to namespace URIs.
	 *
	 * @return prefix-to-namespace map
	 */
	public Map<String, String> getNamespacesByPrefix() {
		return Collections.unmodifiableMap(namespacesByPrefix);
	}

	/**
	 * Evaluates an XPath expression against a W3C DOM target using JAXP.
	 *
	 * <p>This path supports custom functions via {@link SonarFunctionResolver} and uses the
	 * loaded namespace mappings.
	 *
	 * @param xpathExpression XPath expression to compile and evaluate
	 * @param target W3C DOM node to evaluate against (typically the document root)
	 * @param returnType desired JAXP return type (for example {@link XPathConstants#NODESET})
	 * @return the evaluated result as per {@code returnType}
	 * @throws XPathExpressionException when compilation or evaluation fails
	 */
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