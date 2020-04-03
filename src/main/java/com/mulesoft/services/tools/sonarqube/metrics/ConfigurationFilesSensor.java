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
import com.mulesoft.services.xpath.XPathProcessor;

public class ConfigurationFilesSensor extends AbstractMuleSensor {

	private final Logger logger = Loggers.get(ConfigurationFilesSensor.class);
	SAXBuilder saxBuilder = new SAXBuilder();

	private static final String FLOWS_XPATH_EXPRESSION = "count(//mule:mule/mule:flow)";
	private static final String SUBFLOWS_XPATH_EXPRESSION = "count(//mule:mule/mule:sub-flow)";

	private static final String DW_TRANSFORMATION_FLOW_PAYLOAD_XPATH_EXPRESSION_3 = "count(//mule:mule/mule:flow/dw:transform-message/dw:set-payload)";
	private static final String DW_TRANSFORMATION_FLOW_VARIABLE_XPATH_EXPRESSION_3 = "count(//mule:mule/mule:flow/dw:transform-message/dw:set-variable)";

	private static final String DW_TRANSFORMATION_SUBFLOW_PAYLOAD_XPATH_EXPRESSION_3 = "count(//mule:mule/mule:sub-flow/dw:transform-message/dw:set-payload)";
	private static final String DW_TRANSFORMATION_SUBFLOW_VARIABLE_XPATH_EXPRESSION_3 = "count(//mule:mule/mule:sub-flow/dw:transform-message/dw:set-variable)";

	private static final String DW_TRANSFORMATION_FLOW_PAYLOAD_XPATH_EXPRESSION_4 = "count(//mule:mule/mule:flow/ee:transform/ee:message/ee:set-payload)";
	private static final String DW_TRANSFORMATION_FLOW_VARIABLE_XPATH_EXPRESSION_4 = "count(//mule:mule/mule:flow/ee:transform/ee:message/ee:set-variable)";

	private static final String DW_TRANSFORMATION_SUBFLOW_PAYLOAD_XPATH_EXPRESSION_4 = "count(//mule:mule/mule:sub-flow/ee:transform/ee:message/ee:set-payload)";
	private static final String DW_TRANSFORMATION_SUBFLOW_VARIABLE_XPATH_EXPRESSION_4 = "count(//mule:mule/mule:sub-flow/ee:transform/ee:message/ee:set-variable)";

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

			saveMetric(xpathProcessor, MuleMetrics.FLOWS, context, file, rootElement, FLOWS_XPATH_EXPRESSION);

			saveMetric(xpathProcessor, MuleMetrics.SUBFLOWS, context, file, rootElement, SUBFLOWS_XPATH_EXPRESSION);

			// Lines of code = Lines in Mule
			saveMetric(file.lines(), CoreMetrics.NCLOC, context, file);

			if (MuleLanguage.LANGUAGE_MULE4_KEY.equals(language)) {
				saveMetric(xpathProcessor, MuleMetrics.TRANSFORMATIONS, context, file, rootElement,
						DW_TRANSFORMATION_FLOW_PAYLOAD_XPATH_EXPRESSION_4,
						DW_TRANSFORMATION_FLOW_VARIABLE_XPATH_EXPRESSION_4,
						DW_TRANSFORMATION_SUBFLOW_PAYLOAD_XPATH_EXPRESSION_4,
						DW_TRANSFORMATION_SUBFLOW_VARIABLE_XPATH_EXPRESSION_4);
			} else {
				saveMetric(xpathProcessor, MuleMetrics.TRANSFORMATIONS, context, file, rootElement,
						DW_TRANSFORMATION_FLOW_PAYLOAD_XPATH_EXPRESSION_3,
						DW_TRANSFORMATION_FLOW_VARIABLE_XPATH_EXPRESSION_3,
						DW_TRANSFORMATION_SUBFLOW_PAYLOAD_XPATH_EXPRESSION_3,
						DW_TRANSFORMATION_SUBFLOW_VARIABLE_XPATH_EXPRESSION_3);

			}
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
