package com.mulesoft.services.tools.sonarqube.rule.scope;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
		strategies.put(ScopeStrategy.NODE, new NodeStrategyScope());
		strategies.put(ScopeStrategy.PROJECT, new ProjectStrategyScope());
	}

	public ScopeStrategy getStrategy(String scope) {
		return getStrategy(scope, "1.0");
	}

	public ScopeStrategy getStrategy(String scope, String pluginVersion) {
		if (Constants.Applicability.APPLICATION.equals(scope)) {
			return strategies.get(ScopeStrategy.APPLICATION);
		}
		if (Constants.Applicability.PROJECT.equals(scope)) {
			return strategies.get(ScopeStrategy.PROJECT);
		}
		if (ScopeStrategy.NODE.equals(scope)) {
			return strategies.get(ScopeStrategy.NODE);
		}

		ScopeStrategy defaultStrategy = Objects.equals(pluginVersion, "1.1") ? strategies.get(ScopeStrategy.NODE)
				: strategies.get(ScopeStrategy.FILE);
		return strategies.getOrDefault(scope, defaultStrategy);
	}
}
