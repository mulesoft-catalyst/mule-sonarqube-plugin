package com.mulesoft.services.xpath.javax.function.ext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.Before;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mulesoft.services.testutils.TestInputFile;
import com.mulesoft.services.xpath.XPathProcessor;
import com.mulesoft.services.xpath.javax.function.SonarFunctionResolver;
import com.mulesoft.services.xpath.javax.util.MappedNamespaceContext;

/**
 * Base test fixture for validating custom JAXP XPath extension functions.
 *
 * <p>This class wires a namespace-aware {@link XPath} instance with the plugin's
 * {@link SonarFunctionResolver} so tests can evaluate expressions that use the {@code f:...}
 * function namespace (for example {@code f:matches} and {@code f:isConfigurable}).
 *
 * <p>It also provides helpers to evaluate XPath expressions against XML fixture files in
 * {@code src/test/resources/mule4}.
 *
 * @version 1.1.0
 * @since 1.1.0
 * @see MatchesTest
 * @see IsConfigurableTest
 */
public class BaseFunctionTest {
	private static final String testPath = "src/test/resources/mule4";
	private static final String testDirectory = System.getProperty("user.dir") + File.separator + testPath;

	XPath xpath;

	/**
	 * Initializes the namespace context and registers custom XPath functions for each test.
	 *
	 * @throws IOException when the namespace properties fixture cannot be loaded
	 */
	@Before
	public void setUp() throws IOException {
		Map<String, String> prefixToNamespace = new HashMap<>();
		try (InputStream stream = XPathProcessor.class.getClassLoader().getResourceAsStream("namespace-4.properties")) {
			Properties properties = new Properties();
			properties.load(stream);
			properties.forEach((key, value) -> prefixToNamespace.put((String) key, (String) value));
		}

		XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
		xpath = factory.newXPath();
		xpath.setNamespaceContext(new MappedNamespaceContext(prefixToNamespace));
		xpath.setXPathFunctionResolver(new SonarFunctionResolver());
	}

	/**
	 * Evaluates an XPath expression without a context node.
	 *
	 * <p>This is used for expressions like {@code f:matches('value', 'regex')}.
	 *
	 * @param xpathExpression XPath expression to evaluate
	 * @param returnType desired return type (for example {@link XPathConstants#BOOLEAN})
	 * @return the evaluation result (type depends on {@code returnType})
	 * @throws XPathExpressionException when compilation/evaluation fails
	 */
	Object evaluate(String xpathExpression, QName returnType) throws XPathExpressionException {
		XPathExpression expression = xpath.compile(xpathExpression);
		return expression.evaluate((Object) null, returnType);
	}

	/**
	 * Evaluates an XPath expression on a provided context node.
	 *
	 * @param target context node
	 * @param xpathExpression XPath expression to evaluate
	 * @param returnType desired return type (for example {@link XPathConstants#NODESET})
	 * @return the evaluation result (type depends on {@code returnType})
	 * @throws XPathExpressionException when compilation/evaluation fails
	 */
	Object evaluateOnNode(Node target, String xpathExpression, QName returnType) throws XPathExpressionException {
		XPathExpression expression = xpath.compile(xpathExpression);
		return expression.evaluate(target, returnType);
	}

	/**
	 * Loads an XML fixture file and evaluates an XPath expression against its document root.
	 *
	 * @param filename fixture filename under {@code src/test/resources/mule4}
	 * @param xpathExpression XPath expression to evaluate
	 * @param returnType desired return type (for example {@link XPathConstants#NODESET})
	 * @return the evaluation result (type depends on {@code returnType})
	 * @throws XPathExpressionException when compilation/evaluation fails
	 * @throws IOException when the fixture cannot be read
	 */
	Object evaluateOnFixture(String filename, String xpathExpression, QName returnType)
			throws XPathExpressionException, IOException {
		Path fixturePath = Paths.get(testDirectory, filename);
		TestInputFile inputFile = new TestInputFile(fixturePath, testPath + "/" + filename, Charset.defaultCharset());
		XmlFile xmlFile = XmlFile.create(inputFile);
		Node root = xmlFile.getDocument().getFirstChild();

		XPathExpression expression = xpath.compile(xpathExpression);
		return expression.evaluate(root, returnType);
	}

	/**
	 * Loads an XML fixture file, evaluates an XPath expression as a node set, and maps each node.
	 *
	 * @param filename fixture filename under {@code src/test/resources/mule4}
	 * @param xpathExpression XPath expression to evaluate (must return a node set)
	 * @param mapper mapper from matched {@link Node} to a value
	 * @param <T> mapped value type
	 * @return mapped values for the node set, in document order
	 * @throws XPathExpressionException when compilation/evaluation fails
	 * @throws IOException when the fixture cannot be read
	 */
	<T> List<T> evaluateAsNodeListOnFixture(String filename, String xpathExpression, Function<Node, T> mapper)
			throws XPathExpressionException, IOException {
		Path fixturePath = Paths.get(testDirectory, filename);
		TestInputFile inputFile = new TestInputFile(fixturePath, testPath + "/" + filename, Charset.defaultCharset());
		XmlFile xmlFile = XmlFile.create(inputFile);
		Node root = xmlFile.getDocument().getFirstChild();

		XPathExpression expression = xpath.compile(xpathExpression);
		NodeList nodeList = (NodeList) expression.evaluate(root, XPathConstants.NODESET);

		return XmlFile.asList(nodeList).stream().map(mapper).collect(Collectors.toList());
	}
}

