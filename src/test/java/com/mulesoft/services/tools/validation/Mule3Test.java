package com.mulesoft.services.tools.validation;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;

/**
 * Integration-style test that loads the Mule 3 rule store and runs validation across the Mule 3
 * fixture project directory.
 *
 * <p>This test is primarily a smoke test: it ensures rules can be loaded and evaluated without
 * throwing exceptions when scanning a representative Mule 3 project tree.
 *
 * @version 1.1.0
 * @since 1.1.0
 * @see Validator
 */
public class Mule3Test {

	Validator val = null;
	String testPath = "src/test/resources/mule3";

	/**
	 * Creates a {@link Validator} configured for the Mule 3 ruleset.
	 */
	@Before
	public void setUp() {
		val = new Validator(Constants.Ruleset.MULE3);
	}

	/**
	 * Validates the Mule 3 fixture directory using the configured rules.
	 *
	 * @throws JAXBException when rule store parsing fails
	 * @throws IOException when fixture files cannot be read
	 */
	@Test
	public void testValidateProject() throws JAXBException, IOException {
		String testDirectory = System.getProperty("user.dir") + File.separator + testPath;
		System.out.println("Working Directory = " + System.getProperty("user.dir"));
		val.validate(new File(testDirectory));
	}
}
