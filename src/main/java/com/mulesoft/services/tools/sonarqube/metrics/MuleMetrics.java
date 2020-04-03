package com.mulesoft.services.tools.sonarqube.metrics;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

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
	@Override
	public List<Metric> getMetrics() {
		return Arrays.asList(FLOWS, SUBFLOWS, CONFIGURATION_FILES_COMP_RATING, TRANSFORMATIONS, CONFIGURATION_FILES);
	}

}
