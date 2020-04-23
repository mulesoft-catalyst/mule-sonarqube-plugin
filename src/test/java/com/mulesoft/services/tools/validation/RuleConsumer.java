package com.mulesoft.services.tools.validation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mulesoft.services.tools.validation.rules.Rule;
import com.mulesoft.services.tools.validation.rules.Ruleset;
import com.mulesoft.services.tools.validation.rules.Rulestore;
import com.mulesoft.services.xpath.XPathProcessor;

/**
 * Consume each file evaluating all the rules
 * 
 * @author franco.parma
 *
 */
public class RuleConsumer implements Consumer<Path> {

	Logger logger = LoggerFactory.getLogger(getClass());
	SAXBuilder saxBuilder = new SAXBuilder();
	Rulestore store;
	XPathProcessor xpathProcessor;

	public RuleConsumer(Rulestore store) {
		this.store = store;
		xpathProcessor = new XPathProcessor().loadNamespaces(
				store.getType().equals(Constants.Ruleset.MULE3) ? "namespace-3.properties" : "namespace-4.properties");
	}

	@Override
	public void accept(Path t) {
		if (logger.isDebugEnabled()) {
			logger.debug(t.toString());
		}

		if (t.toFile().isFile()) {
			validateRule(store.getRuleset(), t.toFile());
		}
	}

	private void validateRule(List<Ruleset> rulesets, File file) {
		try {
			Document document = saxBuilder.build(file);
			Element rootElement = document.getRootElement();

			for (Iterator<Ruleset> iterator = rulesets.iterator(); iterator.hasNext();) {
				Ruleset ruleset = iterator.next();

				List<Rule> rules = ruleset.getRule();
				for (Iterator<Rule> iteratorRule = rules.iterator(); iteratorRule.hasNext();) {
					Rule rule = iteratorRule.next();
					boolean valid = xpathProcessor.processXPath(rule.getValue().trim(), rootElement, Boolean.class)
							.booleanValue();
					if (logger.isInfoEnabled())
						logger.info("Validation Result: " + valid + " : File: " + file.getName() + " :Rule:"
								+ rule.getDescription() + "- Applies:" + rule.getApplies());

				}
			}
		} catch (JDOMException | IOException e) {
			logger.error("Error Parsing Mule App", e);
		}
	}

}
