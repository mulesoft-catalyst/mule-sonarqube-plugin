package com.mulesoft.services.tools.sonarqube.measures;

import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.mulesoft.services.tools.sonarqube.metrics.MuleMetrics;

/**
 * Computes the total number of transformations
 * @author franco.perez
 *
 */
public class MuleTransformationCount implements MeasureComputer {
	private final Logger logger = Loggers.get(this.getClass());

	@Override
	public MeasureComputerDefinition define(MeasureComputerDefinitionContext defContext) {
		return defContext.newDefinitionBuilder().setOutputMetrics(MuleMetrics.TRANSFORMATIONS.key()).build();
	}

	@Override
	public void compute(MeasureComputerContext context) {
		logger.info("Computing Mule Transformation Count");

		if (context.getComponent().getType() != Component.Type.FILE) {
			int sumTransformations = 0;
			for (Measure child : context.getChildrenMeasures(MuleMetrics.TRANSFORMATIONS.key())) {
				sumTransformations += child.getIntValue();
			}
			context.addMeasure(MuleMetrics.TRANSFORMATIONS.key(), sumTransformations);

		}

	}

}
