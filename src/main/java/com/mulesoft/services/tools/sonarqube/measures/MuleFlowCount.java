package com.mulesoft.services.tools.sonarqube.measures;

import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.mulesoft.services.tools.sonarqube.metrics.MuleMetrics;

/**
 * Aggregates the {@link MuleMetrics#FLOWS} metric across components.
 *
 * <p>{@link com.mulesoft.services.tools.sonarqube.metrics.ConfigurationFilesSensor} records flow counts at the
 * file level. This {@link MeasureComputer} sums child measures to compute the total number of flows for
 * higher-level components (directories/modules/project).
 *
 * @author franco.perez
 * @version 1.1.0
 * @since 1.1.0
 */
public class MuleFlowCount implements MeasureComputer {

	private final Logger logger = Loggers.get(this.getClass());

	/**
	 * Declares that this measure computer outputs the {@code mule_flows} metric.
	 *
	 * @param defContext definition context provided by SonarQube CE
	 * @return a measure computer definition
	 */
	@Override
	public MeasureComputerDefinition define(MeasureComputerDefinitionContext defContext) {
		return defContext.newDefinitionBuilder().setOutputMetrics(MuleMetrics.FLOWS.key()).build();
	}

	/**
	 * Sums all child file measures to compute the aggregated flow count.
	 *
	 * @param context compute context providing access to component type and child measures
	 */
	@Override
	public void compute(MeasureComputerContext context) {
		if(logger.isDebugEnabled()) {
			logger.debug("Computing Mule Flow Size");
		}

		if (context.getComponent().getType() != Component.Type.FILE) {
			int sumFlows = 0;
			for (Measure child : context.getChildrenMeasures(MuleMetrics.FLOWS.key())) {
				sumFlows += child.getIntValue();
			}
			context.addMeasure(MuleMetrics.FLOWS.key(), sumFlows);

		}
	}

}
