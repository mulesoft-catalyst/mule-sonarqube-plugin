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
import com.mulesoft.services.xpath.XPathProcessor;

public class ConfigurationFilesSensor extends AbstractMuleSensor {

	private final Logger logger = Loggers.get(ConfigurationFilesSensor.class);
	SAXBuilder saxBuilder = new SAXBuilder();

	private static final String METRIC_FLOW_PROPERTY = "mule.metric.flow";
	private static final String METRIC_SUBFLOW_PROPERTY = "mule.metric.subflow";
	private static final String METRIC_DW_PAYLOAD_PROPERTY = "mule.metric.dw.payload";
	private static final String METRIC_DW_VARIABLE_PROPERTY = "mule.metric.dw.variable";

	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.name("Compute size of configuration file");
	}

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

			// Lines of code = Lines in Mule
			saveMetric(file.lines(), CoreMetrics.NCLOC, context, file);

			saveMetric(xpathProcessor, MuleMetrics.TRANSFORMATIONS, context, file, rootElement,
					MuleProperties.getProperties(language).get(METRIC_DW_PAYLOAD_PROPERTY).toString(),
					MuleProperties.getProperties(language).get(METRIC_DW_VARIABLE_PROPERTY).toString());

		} catch (JDOMException | IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void saveMetric(XPathProcessor helper, Metric metric, SensorContext context, InputFile file,
			Element rootElement, String... xpathExpressions) {
		int result = 0;
		for (int i = 0; i < xpathExpressions.length; i++) {
			result += helper.processXPath(xpathExpressions[i], rootElement, Double.class).intValue();
		}
		saveMetric(result, metric, context, file);
	}

	private void saveMetric(int result, Metric metric, SensorContext context, InputFile file) {
		NewMeasure<Integer> metrics = context.newMeasure();
		metrics.forMetric(metric).on(file).withValue(result).save();
	}

}
