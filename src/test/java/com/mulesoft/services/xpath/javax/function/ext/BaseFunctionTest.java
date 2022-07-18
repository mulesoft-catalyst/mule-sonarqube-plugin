package com.mulesoft.services.xpath.javax.function.ext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
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
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionResolver;

import com.mulesoft.services.xpath.XPathProcessor;
import com.mulesoft.services.xpath.javax.function.SonarFunctionResolver;
import com.mulesoft.services.xpath.javax.util.MappedNamespaceContext;

import org.junit.Before;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class BaseFunctionTest {
    final static String testPath = "src/test/resources/mule4";
	final static String testDirectory = System.getProperty("user.dir") + File.separator + testPath;
    XPath xpath;

    @Before
    public void setUp() throws IOException {
		Map<String, String> prefixToNamespace = new HashMap<>();
		try (InputStream stream = XPathProcessor.class.getClassLoader().getResourceAsStream("namespace-4.properties")) {
			Properties properties = new Properties();
			properties.load(stream);
			properties.forEach((key, value) -> prefixToNamespace.put((String)key, (String)value));
		} catch (IOException e) {
			throw e;
		}
		XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
		xpath = factory.newXPath();
        xpath.setNamespaceContext(new MappedNamespaceContext(prefixToNamespace));

		xpath.setXPathFunctionResolver(new SonarFunctionResolver());
    }

    Object evaluate(String xpathExpression, QName returnType) throws XPathExpressionException {
		XPathExpression expression = xpath.compile(xpathExpression);
		return expression.evaluate((Object)null, returnType);
	}

	Object evaluateOnNode(Node target, String xpathExpression, QName returnType) throws XPathExpressionException {
		XPathExpression expression = xpath.compile(xpathExpression);
		return expression.evaluate(target, returnType);
	}

	Object evaluateOnFixture(String filename, String xpathExpression, QName returnType) throws XPathExpressionException, IOException {
        XmlFile xmlFile = XmlFile.create(TestInputFileBuilder.create(testDirectory, filename).setCharset(Charset.defaultCharset()).build());
		Node root = xmlFile.getDocument().getFirstChild();

		XPathExpression expression = xpath.compile(xpathExpression);
		return expression.evaluate(root, returnType);
	}

	<T> List<T> evaluateAsNodeListOnFixture(String filename, String xpathExpression, Function<Node, T> mapper) throws XPathExpressionException, IOException {
        XmlFile xmlFile = XmlFile.create(TestInputFileBuilder.create(testDirectory, filename).setCharset(Charset.defaultCharset()).build());
		Node root = xmlFile.getDocument().getFirstChild();

		XPathExpression expression = xpath.compile(xpathExpression);
		NodeList nodeList =  (NodeList)expression.evaluate(root, XPathConstants.NODESET);

        return XmlFile.asList(nodeList).stream().map(mapper).collect(Collectors.toList());
	}
}
