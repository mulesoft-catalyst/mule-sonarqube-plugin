package com.mulesoft.services.tools.sonarqube.measures;

import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.mulesoft.services.tools.sonarqube.metrics.MuleMetrics;

/**
 * Computes a configuration complexity rating based on flow/subflow counts.
 *
 * <p>The plugin uses a simple threshold-based approach: the total number of flows
 * (flows + subflows) maps to a rating value compatible with SonarQube’s rating metric
 * representation (A=1, B=2, C=3).
 *
 * @author franco.perez
 * @version 1.1.0
 * @since 1.1.0
 * @see MuleMetrics#CONFIGURATION_FILES_COMP_RATING
 */
public class MuleSizeRating implements MeasureComputer {

	private final Logger logger = Loggers.get(this.getClass());
	private static final int THRESHOLD_SIMPLE = 7;
	private static final int THRESHOLD_MEDIUM = 15;
	private static final int RATING_A = 1;
	private static final int RATING_B = 2;
	private static final int RATING_C = 3;

	/**
	 * Declares required input metrics and the output rating metric.
	 *
	 * @param def definition context
	 * @return a measure computer definition
	 */
	@Override
	public MeasureComputerDefinition define(MeasureComputerDefinitionContext def) {
		return def.newDefinitionBuilder().setInputMetrics(MuleMetrics.FLOWS.key(), MuleMetrics.SUBFLOWS.key())
				.setOutputMetrics(MuleMetrics.CONFIGURATION_FILES_COMP_RATING.key()).build();
	}

	/**
	 * Computes the rating based on the summed number of flows and subflows.
	 *
	 * @param context compute context providing access to measures
	 */
	@Override
	public void compute(MeasureComputerContext context) {
		if(logger.isDebugEnabled()) {
			logger.debug("Computing MuleSizeRating");
		}
		Measure flows = context.getMeasure(MuleMetrics.FLOWS.key());
		Measure subflows = context.getMeasure(MuleMetrics.SUBFLOWS.key());
		int totalNumberOfFlows = 0;
		if (flows != null) {
			totalNumberOfFlows += flows.getIntValue();
		}
		if (subflows != null) {
			totalNumberOfFlows += subflows.getIntValue();
		}

		// rating values are currently implemented as integers in API
		int rating = RATING_A;
		if (totalNumberOfFlows > THRESHOLD_SIMPLE) {
			rating = RATING_B;
		}
		if (totalNumberOfFlows > THRESHOLD_MEDIUM) {
			rating = RATING_C;
		}
		context.addMeasure(MuleMetrics.CONFIGURATION_FILES_COMP_RATING.key(), rating);
	}
}
