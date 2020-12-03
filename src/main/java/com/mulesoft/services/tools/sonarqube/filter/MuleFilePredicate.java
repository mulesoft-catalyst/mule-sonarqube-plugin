package com.mulesoft.services.tools.sonarqube.filter;

import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class MuleFilePredicate implements FilePredicate {

	private final Logger logger = Loggers.get(MuleFilePredicate.class);
	SAXBuilder saxBuilder = new SAXBuilder();
	String muleNamespace = "http://www.mulesoft.org/schema/mule/core";
	String[] fileExtensions;
	/**
	 * 
	 */
	public MuleFilePredicate(String[] aFileSuffixes) {
		super();
		fileExtensions = aFileSuffixes;
	}

	@Override
	public boolean apply(InputFile inputFile) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing Mule Sensor on file:" + inputFile.filename());
		}

		for (String fileExtension : fileExtensions) {
			if (inputFile.filename().endsWith(fileExtension)) {
				try {
					Document document = saxBuilder.build(inputFile.inputStream());
	
					String namespace = document.getRootElement().getNamespaceURI();
					if (muleNamespace.equals(namespace))
						return true;
				} catch (JDOMException | IOException e) {
					logger.error("Parsing document:" + inputFile.filename(), e);
				}
			}
		}
		return false;
	}

}
