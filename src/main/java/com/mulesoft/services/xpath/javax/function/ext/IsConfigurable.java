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

public class IsConfigurable implements XPathFunction {
	private static final Logger logger = LoggerFactory.getLogger(IsConfigurable.class);

	private static final String IS_CONFIGURABLE_REGEX = "^\\$\\{.*\\}$";
	private static final Pattern IS_CONFIGURABLE_PATTERN = Pattern.compile(IS_CONFIGURABLE_REGEX);

	private final boolean invert;

	public IsConfigurable() {
		this(false);
	}

	public IsConfigurable(boolean invert) {
		this.invert = invert;
	}

	@Override
	public Object evaluate(List<?> args) throws XPathFunctionException {
		Object arg0 = args.get(0);
		if (arg0 instanceof NodeList) {
			NodeList nodeList = (NodeList) arg0;
			logger.debug("isConfigurable: nodeset size={}, invert={}", nodeList.getLength(), invert);
			return NodeLists.asNodeList(XmlFile.asList(nodeList).stream().filter(node -> {
				String textContent = node.getTextContent();
				return IS_CONFIGURABLE_PATTERN.matcher(textContent).matches() ^ invert;
			}).collect(Collectors.toList()));
		} else if (arg0 instanceof String) {
			String string = (String) arg0;
			return IS_CONFIGURABLE_PATTERN.matcher(string).matches() ^ invert;
		} else {
			throw new XPathFunctionException(
					String.format("f:isConfigurable expected (NodeSet) or (String), got (%s)", arg0.getClass().getSimpleName()));
		}
	}
}

