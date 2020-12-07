package com.mulesoft.services.xpath;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.jdom2.Content;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mulesoft.services.xpath.jaxen.function.ext.MatchesFunction;
import com.mulesoft.services.xpath.jaxen.function.ext.IsConfigurableFunction;

public class XPathProcessor {
	Logger logger = LoggerFactory.getLogger(getClass());

	private Properties namespacesProperties = new Properties();

	private List<Namespace> namespace = new ArrayList<Namespace>();

	public XPathProcessor() {
		MatchesFunction.registerSelfInSimpleContext();
		IsConfigurableFunction.registerSelfInSimpleContext();
	}

	public XPathProcessor loadNamespaces(String resourceName) {
		try (InputStream stream = XPathProcessor.class.getClassLoader().getResourceAsStream(resourceName)) {
			namespacesProperties.load(stream);
			namespacesProperties
					.forEach((Object t, Object u) -> namespace.add(Namespace.getNamespace(t.toString(), u.toString())));

		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public <T> T processXPath(String xpathExpression, Content target, Class<T> type) {
		if (logger.isDebugEnabled()) {
			logger.debug("Evaluating:" + xpathExpression);
		}
		XPathFactory xpfac = XPathFactory.instance();
		@SuppressWarnings("rawtypes")
		XPathExpression xp = xpfac.compile(xpathExpression, Filters.fpassthrough(), Collections.emptyMap(), namespace);

		return (T) xp.evaluateFirst(target);
	}

}