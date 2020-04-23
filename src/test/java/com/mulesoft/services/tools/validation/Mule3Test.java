package com.mulesoft.services.tools.validation;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;

public class Mule3Test {

	Validator val = null;
	String testPath = "src/test/resources/mule3";

	@Before
	public void setUp() {
		val = new Validator(Constants.Ruleset.MULE3);
	}

	@Test
	public void testValidateProject() throws JAXBException, IOException {
		String testDirectory = System.getProperty("user.dir") + File.separator + testPath;
		System.out.println("Working Directory = " + System.getProperty("user.dir"));
		val.validate(new File(testDirectory));
	}
}
