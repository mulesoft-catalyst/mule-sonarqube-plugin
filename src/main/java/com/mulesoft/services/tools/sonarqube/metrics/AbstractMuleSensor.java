package com.mulesoft.services.tools.sonarqube.metrics;

import org.jdom2.input.SAXBuilder;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;

import com.mulesoft.services.tools.sonarqube.filter.MuleFilePredicate;
import com.mulesoft.services.tools.sonarqube.language.MuleLanguage;
import com.mulesoft.services.tools.sonarqube.sensor.MuleSensor;

public abstract class AbstractMuleSensor implements Sensor {

	SAXBuilder saxBuilder = new SAXBuilder();

	@Override
	public void execute(SensorContext context) {

		FileSystem fs = context.fileSystem();
		// Only ConfigurationFiles
		Iterable<InputFile> files = fs.inputFiles(new MuleFilePredicate(new MuleLanguage(context.config()).getFileSuffixes()));
		for (InputFile file : files) {
			process(context, file, MuleSensor.getLanguage(context));
		}
	}

	protected abstract void process(SensorContext context, InputFile file, String language);

}
