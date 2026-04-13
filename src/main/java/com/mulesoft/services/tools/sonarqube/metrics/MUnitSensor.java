package com.mulesoft.services.tools.sonarqube.metrics;

import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.mulesoft.services.tools.sonarqube.language.MuleLanguage;
import com.mulesoft.services.tools.sonarqube.properties.MuleProperties;
import com.mulesoft.services.tools.sonarqube.xml.SecureSaxBuilder;
import com.mulesoft.services.xpath.XPathProcessor;

/**
 * Computes unit test metrics (number of MUnit test cases) for Mule projects.
 *
 * <p>The sensor evaluates a language-specific XPath expression (configured in {@link MuleProperties})
 * to count test cases within MUnit XML files and saves the result into {@link CoreMetrics#TESTS}.
 *
 * @author franco.perez
 * @version 1.1.0
 * @since 1.1.0
 */
public class MUnitSensor extends AbstractMuleSensor {

	private final Logger logger = Loggers.get(MUnitSensor.class);

	SAXBuilder saxBuilder = SecureSaxBuilder.create();

	private static final String METRIC_UNIT_TESTS_PROPERTY = "mule.metric.test";

	/**
	 * Describes this sensor for SonarQube.
	 *
	 * @param descriptor the sensor descriptor
	 */
	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.name("Compute number of unit test cases");
	}

	/**
	 * Parses the file and saves the unit test count metric as a file-level measure.
	 *
	 * @param context sensor execution context
	 * @param file input file being processed
	 * @param language Mule variant key (for example {@code mule3} or {@code mule4})
	 */
	@Override
	protected void process(SensorContext context, InputFile file, String language) {
		try {
			Document document = saxBuilder.build(file.inputStream());
			Element rootElement = document.getRootElement();
			XPathProcessor xpathProcessor = new XPathProcessor()
					.loadNamespaces(language.equals(MuleLanguage.LANGUAGE_MULE4_KEY) ? "namespace-4.properties"
							: "namespace-3.properties");
			saveMetric(xpathProcessor, CoreMetrics.TESTS, context, file, rootElement,
					MuleProperties.getProperties(language).get(METRIC_UNIT_TESTS_PROPERTY).toString());
		} catch (JDOMException | IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Evaluates one or more XPath expressions and saves their summed result to the given metric.
	 *
	 * @param helper XPath processor configured with namespaces
	 * @param metric target metric
	 * @param context sensor execution context
	 * @param file SonarQube input file that owns the measure
	 * @param rootElement JDOM root element of the XML file
	 * @param xpathExpressions one or more XPath expressions whose numeric results will be summed
	 */
	private void saveMetric(XPathProcessor helper, Metric metric, SensorContext context, InputFile file,
			Element rootElement, String... xpathExpressions) {
		int result = 0;
		for (int i = 0; i < xpathExpressions.length; i++) {
			result += helper.processXPath(xpathExpressions[i], rootElement, Double.class).intValue();
		}
		NewMeasure<Integer> metrics = context.newMeasure();
		metrics.forMetric(metric).on(file).withValue(result).save();
	}

}
