package com.mulesoft.services.tools.sonarqube.metrics;

import org.jdom2.input.SAXBuilder;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;

import com.mulesoft.services.tools.sonarqube.filter.MuleFilePredicate;
import com.mulesoft.services.tools.sonarqube.language.MuleLanguage;
import com.mulesoft.services.tools.sonarqube.sensor.MuleSensor;

/**
 * Base class for sensors that iterate over Mule configuration files.
 *
 * <p>Concrete sensors implement {@link #process(SensorContext, InputFile, String)} and rely on this base
 * implementation to:
 * <ul>
 *   <li>resolve which file suffixes should be scanned</li>
 *   <li>filter Mule XML files using {@link MuleFilePredicate}</li>
 *   <li>derive the Mule variant (Mule 3 vs Mule 4) using {@link MuleSensor#getLanguage(SensorContext)}</li>
 * </ul>
 *
 * @version 1.1.0
 * @since 1.1.0
 */
public abstract class AbstractMuleSensor implements Sensor {

	SAXBuilder saxBuilder = new SAXBuilder();

	/**
	 * Executes the sensor by iterating over Mule configuration files and delegating each file to
	 * {@link #process(SensorContext, InputFile, String)}.
	 *
	 * @param context the sensor execution context
	 */
	@Override
	public void execute(SensorContext context) {

		FileSystem fs = context.fileSystem();
		// Only ConfigurationFiles
		String[] scanSuffixes = context.config().getStringArray(MuleLanguage.SCAN_FILE_SUFFIXES_KEY);
		if (scanSuffixes.length == 0) {
			scanSuffixes = MuleLanguage.SCAN_FILE_SUFFIXES_DEFAULT_VALUE.split(",");
		}
		Iterable<InputFile> files = fs.inputFiles(new MuleFilePredicate(scanSuffixes));
		for (InputFile file : files) {
			process(context, file, MuleSensor.getLanguage(context));
		}
	}

	/**
	 * Processes a single Mule configuration file.
	 *
	 * @param context sensor execution context
	 * @param file the Mule configuration file
	 * @param language Mule variant key (for example {@code mule3} or {@code mule4})
	 */
	protected abstract void process(SensorContext context, InputFile file, String language);

}
