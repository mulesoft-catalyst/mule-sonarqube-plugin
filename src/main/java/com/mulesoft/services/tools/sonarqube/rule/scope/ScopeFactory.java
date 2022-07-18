package com.mulesoft.services.tools.sonarqube.rule.scope;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
		strategies.put(ScopeStrategy.NODE, new NodeStrategyScope());
	}

	public ScopeStrategy getStrategy(String scope, String pluginVersion) {
		ScopeStrategy defaultStrategy = Objects.equals(pluginVersion, "1.1") ? 
		strategies.get(ScopeStrategy.NODE) :
		strategies.get(ScopeStrategy.FILE);

		return strategies.getOrDefault(scope, defaultStrategy);
	}
}
