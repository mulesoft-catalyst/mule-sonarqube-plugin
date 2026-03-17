package com.mulesoft.services.tools.sonarqube.rule.scope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.sonarsource.analyzer.commons.xml.XmlTextRange;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mulesoft.services.tools.sonarqube.rule.MuleRulesDefinition;
import com.mulesoft.services.tools.sonarqube.xml.SecureSaxBuilder;
import com.mulesoft.services.xpath.XPathProcessor;

public class FileStrategyScope implements ScopeStrategy {
	private final Logger logger = Loggers.get(FileStrategyScope.class);
	SAXBuilder saxBuilder = SecureSaxBuilder.create();
	private static final Pattern MATCHES_CALL = Pattern.compile("^\\s*matches\\((.+),\\s*'([^']*)'\\s*\\)\\s*$");

	@Override
	public void validate(XPathProcessor xpathValidator, Map<RuleKey, List<NewIssue>> issues, SensorContext context,
			InputFile t, ActiveRule rule) {
		String pluginVersion = safeTrim(rule.param(MuleRulesDefinition.PARAMS.PLUGIN_VERSION));
		if ("1.1".equalsIgnoreCase(pluginVersion)) {
			validateV11(xpathValidator, issues, context, t, rule);
			return;
		}
		try {
			Document document = saxBuilder.build(t.inputStream());
			Element rootElement = document.getRootElement();
			String ruleXPath = safeTrim(rule.param(MuleRulesDefinition.PARAMS.XPATH));
			Boolean validResult = xpathValidator.processXPath(ruleXPath, rootElement, Boolean.class);
			boolean valid = Boolean.TRUE.equals(validResult);
			logger.debug("Validation Result: {} : File: {} :Rule: {}", valid, t.filename(), rule.ruleKey());
			if (!valid) {
				
				XmlTextRange textRange;
				try {
					// see org.sonarsource.analyzer.commons.xml.checks.SonarXmlCheck.reportIssue(Node, String)
					SonarXmlCheckHelper tempSonarXmlCheckHelper = new SonarXmlCheckHelper();
					XmlFile xmlFile = XmlFile.create(t);
					tempSonarXmlCheckHelper.scanFile(context, rule.ruleKey(), xmlFile); // just to fill the properties
					String locationFindingXPath = safeTrim(rule.param(MuleRulesDefinition.PARAMS.XPATH_LOCATION_HINT));
					if (locationFindingXPath.isEmpty()) {
						logger.debug("No locationFindingXPath: params={}", rule.params());
						textRange = null;
					} else {
						Node locationElement = findLocationNode(xmlFile, locationFindingXPath,
								xpathValidator.getNamespacesByPrefix());
						textRange = locationElement == null ? null : XmlFile.nodeLocation(locationElement);
					}
				} catch (RuntimeException | XPathExpressionException e) {
					logger.debug("Ignoring location hint failure", e);
					textRange= null;
				}

				NewIssue newIssue = context.newIssue().forRule(rule.ruleKey());
				NewIssueLocation primaryLocation;
				if (textRange == null) {
					primaryLocation = newIssue.newLocation().on(t);
					if (t.lines() > 0) {
						org.sonar.api.batch.fs.TextRange range = IssueLocations.primaryRange(t);
						if (range != null) {
							primaryLocation = primaryLocation.at(range);
						}
					}
				} else {
					primaryLocation = newIssue.newLocation().on(t) .at(t.newRange(
							textRange.getStartLine(),
							textRange.getStartColumn(),
							textRange.getEndLine(),
							textRange.getEndColumn()));
				}
				
				newIssue.at(primaryLocation);
				addIssue(issues, rule, newIssue);
			}

		} catch (JDOMException | IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void validateV11(XPathProcessor xpathValidator, Map<RuleKey, List<NewIssue>> issues, SensorContext context,
			InputFile t, ActiveRule rule) {
		try {
			XmlFile xmlFile = XmlFile.create(t);
			Node root = xmlFile.getDocument().getFirstChild();
			String xpath = safeTrim(rule.param(MuleRulesDefinition.PARAMS.XPATH));
			Boolean valid = (Boolean) xpathValidator.processXPathAsNodeSet(xpath, root, XPathConstants.BOOLEAN);
			if (!Boolean.TRUE.equals(valid)) {
				NewIssue newIssue = context.newIssue().forRule(rule.ruleKey());
				NewIssueLocation primaryLocation = newIssue.newLocation().on(t);
				if (t.lines() > 0) {
					org.sonar.api.batch.fs.TextRange range = IssueLocations.primaryRange(t);
					if (range != null) {
						primaryLocation = primaryLocation.at(range);
					}
				}
				newIssue.at(primaryLocation);
				addIssue(issues, rule, newIssue);
			}
		} catch (IOException | XPathExpressionException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private static String safeTrim(String s) {
		return s == null ? "" : s.trim();
	}

	private static Node findLocationNode(XmlFile xmlFile, String locationFindingXPath,
			Map<String, String> namespacesByPrefix) throws XPathExpressionException {
		javax.xml.xpath.XPath xpath = XPathFactory.newInstance().newXPath();
		xpath.setNamespaceContext(new MapNamespaceContext(namespacesByPrefix));
		try {
			return (Node) xpath.compile(locationFindingXPath).evaluate(xmlFile.getDocument().getFirstChild(),
					XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			Matcher m = MATCHES_CALL.matcher(locationFindingXPath);
			if (!m.matches()) {
				throw e;
			}
			String inner = m.group(1).trim();
			String regex = m.group(2);
			NodeList nodes = (NodeList) xpath.compile(inner).evaluate(xmlFile.getDocument().getFirstChild(),
					XPathConstants.NODESET);
			Pattern p = Pattern.compile(regex);
			for (int i = 0; i < nodes.getLength(); i++) {
				Node n = nodes.item(i);
				String value = n.getNodeType() == Node.ATTRIBUTE_NODE ? n.getNodeValue() : n.getTextContent();
				if (value != null && p.matcher(value).find()) {
					return n;
				}
			}
			return null;
		}
	}

	private static final class MapNamespaceContext implements NamespaceContext {
		private final Map<String, String> namespacesByPrefix;

		private MapNamespaceContext(Map<String, String> namespacesByPrefix) {
			this.namespacesByPrefix = namespacesByPrefix;
		}

		@Override
		public String getNamespaceURI(String prefix) {
			if (prefix == null) {
				return XMLConstants.NULL_NS_URI;
			}
			if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
				return XMLConstants.XML_NS_URI;
			}
			if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
				return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
			}
			return namespacesByPrefix.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
		}

		@Override
		public String getPrefix(String namespaceURI) {
			if (namespaceURI == null) {
				return null;
			}
			for (Map.Entry<String, String> e : namespacesByPrefix.entrySet()) {
				if (namespaceURI.equals(e.getValue())) {
					return e.getKey();
				}
			}
			return null;
		}

		@Override
		public java.util.Iterator<String> getPrefixes(String namespaceURI) {
			List<String> prefixes = new ArrayList<>();
			if (namespaceURI != null) {
				for (Map.Entry<String, String> e : namespacesByPrefix.entrySet()) {
					if (namespaceURI.equals(e.getValue())) {
						prefixes.add(e.getKey());
					}
				}
			}
			return prefixes.iterator();
		}
	}

	private void addIssue(Map<RuleKey, List<NewIssue>> issues, ActiveRule rule, NewIssue issue) {

		if (issues.containsKey(rule.ruleKey())) {
			issues.get(rule.ruleKey()).add(issue);
		} else {
			List<NewIssue> issuesList = new ArrayList<NewIssue>();
			issuesList.add(issue);
			issues.put(rule.ruleKey(), issuesList);
		}

	}
}
