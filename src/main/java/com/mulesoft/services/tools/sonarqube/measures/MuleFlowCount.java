package com.mulesoft.services.tools.sonarqube.measures;

import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.mulesoft.services.tools.sonarqube.metrics.MuleMetrics;

/**
 * Computes the total number of Flows
 * @author franco.perez
 *
 */
public class MuleFlowCount implements MeasureComputer {

	private final Logger logger = Loggers.get(this.getClass());

	@Override
	public MeasureComputerDefinition define(MeasureComputerDefinitionContext defContext) {
		return defContext.newDefinitionBuilder().setOutputMetrics(MuleMetrics.FLOWS.key()).build();
	}

	@Override
	public void compute(MeasureComputerContext context) {
		logger.info("Computing Mule Flow Size");

		if (context.getComponent().getType() != Component.Type.FILE) {
			int sumFlows = 0;
			for (Measure child : context.getChildrenMeasures(MuleMetrics.FLOWS.key())) {
				sumFlows += child.getIntValue();
			}
			context.addMeasure(MuleMetrics.FLOWS.key(), sumFlows);

		}
	}

}
