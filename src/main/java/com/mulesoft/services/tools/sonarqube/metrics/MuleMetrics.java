package com.mulesoft.services.tools.sonarqube.metrics;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

/**
 * Declares custom SonarQube metrics produced by the Mule plugin.
 *
 * <p>These metrics are used by sensors/measures to report counts (flows, subflows, transformations,
 * configuration files) and an overall complexity rating for configuration files.
 *
 * @version 1.1.0
 * @since 1.1.0
 * @see com.mulesoft.services.tools.sonarqube.measures.MuleFlowCount
 * @see com.mulesoft.services.tools.sonarqube.measures.MuleSubFlowCount
 * @see com.mulesoft.services.tools.sonarqube.measures.MuleTransformationCount
 * @see com.mulesoft.services.tools.sonarqube.measures.MuleSizeRating
 */
public class MuleMetrics implements Metrics {

	public static final Metric FLOWS = new Metric.Builder("mule_flows", "Flows", Metric.ValueType.INT)
			.setDescription("Number of flows").setDirection(Metric.DIRECTION_WORST).setQualitative(false)
			.setDomain(org.sonar.api.measures.CoreMetrics.DOMAIN_SIZE).create();

	public static final Metric<Integer> SUBFLOWS = new Metric.Builder("mule_subflows", "SubFlows", Metric.ValueType.INT)
			.setDescription("Number of subflows").setDirection(Metric.DIRECTION_WORST).setQualitative(false)
			.setDomain(org.sonar.api.measures.CoreMetrics.DOMAIN_SIZE).create();

	public static final Metric<Integer> TRANSFORMATIONS = new Metric.Builder("mule_dw", "DW Transformations",
			Metric.ValueType.INT).setDescription("Number of subflows").setDirection(Metric.DIRECTION_WORST)
					.setQualitative(false).setDomain(org.sonar.api.measures.CoreMetrics.DOMAIN_SIZE).create();

	public static final Metric<Integer> CONFIGURATION_FILES_COMP_RATING = new Metric.Builder(
			"mule_configuration_complexity_rating", "Configuration File Complexity", Metric.ValueType.RATING)
					.setDescription("Rating based on complexity of configuration file")
					.setDirection(Metric.DIRECTION_BETTER).setQualitative(true).setDomain(CoreMetrics.DOMAIN_COMPLEXITY)
					.create();

	public static final Metric<Integer> CONFIGURATION_FILES = new Metric.Builder("mule_configuration_files",
			"Configuration Files", Metric.ValueType.INT).setDescription("Number of configuration files")
					.setDirection(Metric.DIRECTION_WORST).setQualitative(false)
					.setDomain(org.sonar.api.measures.CoreMetrics.DOMAIN_SIZE).create();

	@SuppressWarnings("rawtypes")
	/**
	 * Returns the list of metrics exposed by this plugin.
	 *
	 * @return list of SonarQube metrics to register
	 */
	@Override
	public List<Metric> getMetrics() {
		return Arrays.asList(FLOWS, SUBFLOWS, CONFIGURATION_FILES_COMP_RATING, TRANSFORMATIONS, CONFIGURATION_FILES);
	}

}
