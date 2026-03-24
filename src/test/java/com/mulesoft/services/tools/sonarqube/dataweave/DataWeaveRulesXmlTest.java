package com.mulesoft.services.tools.sonarqube.dataweave;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.mulesoft.services.tools.validation.RuleFactory;
import com.mulesoft.services.tools.validation.rules.Rulestore;

/**
 * Verifies the embedded DataWeave rules XML is present and parsable.
 */
public class DataWeaveRulesXmlTest {

	@Test
	public void rulesDataweaveXmlIsLoadableFromClasspath() throws Exception {
		Rulestore store = RuleFactory.loadRulesFromXml("classpath:rules-dataweave.xml");
		assertNotNull(store);
	}
}

