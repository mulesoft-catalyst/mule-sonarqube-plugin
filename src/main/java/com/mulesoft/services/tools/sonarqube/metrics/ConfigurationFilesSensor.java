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
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.mulesoft.services.tools.sonarqube.language.MuleLanguage;
import com.mulesoft.services.tools.sonarqube.properties.MuleProperties;
import com.mulesoft.services.tools.sonarqube.xml.SecureSaxBuilder;
import com.mulesoft.services.xpath.XPathProcessor;

/**
 * Computes per-file Mule size metrics (flows, subflows, DataWeave transformations) from Mule XML.
 *
 * <p>The sensor runs on Mule configuration files and evaluates XPath expressions (configured via
 * {@link MuleProperties}) to count:
 * <ul>
 *   <li>flows</li>
 *   <li>subflows</li>
 *   <li>DataWeave transformations (payload and variable variants)</li>
 * </ul>
 *
 * <p>Values are saved as file-level measures that can later be aggregated by {@code MeasureComputer}s.
 *
 * @version 1.1.0
 * @since 1.1.0
 * @see MuleMetrics
 */
public class ConfigurationFilesSensor extends AbstractMuleSensor {

	private final Logger logger = Loggers.get(ConfigurationFilesSensor.class);
	SAXBuilder saxBuilder = SecureSaxBuilder.create();

	private static final String METRIC_FLOW_PROPERTY = "mule.metric.flow";
	private static final String METRIC_SUBFLOW_PROPERTY = "mule.metric.subflow";
	private static final String METRIC_DW_PAYLOAD_PROPERTY = "mule.metric.dw.payload";
	private static final String METRIC_DW_VARIABLE_PROPERTY = "mule.metric.dw.variable";

	/**
	 * Describes this sensor for SonarQube.
	 *
	 * @param descriptor the sensor descriptor
	 */
	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.name("Compute size of configuration file");
	}

	/**
	 * Parses the file and saves the configured metrics as file-level measures.
	 *
	 * @param context sensor execution context
	 * @param file Mule configuration file being processed
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

			saveMetric(xpathProcessor, MuleMetrics.FLOWS, context, file, rootElement,
					MuleProperties.getProperties(language).get(METRIC_FLOW_PROPERTY).toString());

			saveMetric(xpathProcessor, MuleMetrics.SUBFLOWS, context, file, rootElement,
					MuleProperties.getProperties(language).get(METRIC_SUBFLOW_PROPERTY).toString());

			saveMetric(xpathProcessor, MuleMetrics.TRANSFORMATIONS, context, file, rootElement,
					MuleProperties.getProperties(language).get(METRIC_DW_PAYLOAD_PROPERTY).toString(),
					MuleProperties.getProperties(language).get(METRIC_DW_VARIABLE_PROPERTY).toString());

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
		saveMetric(result, metric, context, file);
	}

	/**
	 * Saves an integer metric value on the provided file.
	 *
	 * @param result metric value to save
	 * @param metric target metric
	 * @param context sensor execution context
	 * @param file SonarQube input file that owns the measure
	 */
	private void saveMetric(int result, Metric metric, SensorContext context, InputFile file) {
		NewMeasure<Integer> metrics = context.newMeasure();
		metrics.forMetric(metric).on(file).withValue(result).save();
	}

}
