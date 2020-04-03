package com.mulesoft.services.tools.sonarqube.rule.scope;

import java.util.HashMap;
import java.util.Map;

import com.mulesoft.services.tools.validation.Constants;

public class ScopeFactory {

	private static ScopeFactory instance = null;

	private Map<String, ScopeStrategy> strategies = new HashMap<String, ScopeStrategy>();

	public static ScopeFactory getInstance() {
		if (instance == null) {
			instance = new ScopeFactory();
		}
		return instance;
	}

	public ScopeFactory() {
		strategies.put(ScopeStrategy.FILE, new FileStrategyScope());
		strategies.put(ScopeStrategy.APPLICATION, new ApplicationStrategyScope());
	}

	public ScopeStrategy getStrategy(String scope) {
		if (Constants.Applicability.APPLICATION.equals(scope)) {
			return strategies.get(ScopeStrategy.APPLICATION);
		}
		return strategies.get(ScopeStrategy.FILE);
	}
}
