package com.mulesoft.services.tools.sonarqube.rule.scope;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.mulesoft.services.tools.validation.Constants;

/**
 * Factory and registry for {@link ScopeStrategy} implementations.
 *
 * <p>Rules in this plugin can apply at different scopes (file, node, application, project).
 * This factory maps the configured scope parameter to an executable {@link ScopeStrategy}.
 *
 * <p>Some behaviors are version-sensitive: for plugin version {@code 1.1} the default scope
 * is node-based; earlier versions default to file-based behavior.
 *
 * @version 1.1.0
 * @since 1.1.0
 * @see ScopeStrategy
 */
public class ScopeFactory {

	private static ScopeFactory instance = null;

	private Map<String, ScopeStrategy> strategies = new HashMap<String, ScopeStrategy>();

	/**
	 * Returns the singleton factory instance.
	 *
	 * @return the global {@link ScopeFactory} instance
	 */
	public static ScopeFactory getInstance() {
		if (instance == null) {
			instance = new ScopeFactory();
		}
		return instance;
	}

	/**
	 * Creates the factory and registers built-in strategies.
	 */
	public ScopeFactory() {
		strategies.put(ScopeStrategy.FILE, new FileStrategyScope());
		strategies.put(ScopeStrategy.APPLICATION, new ApplicationStrategyScope());
		strategies.put(ScopeStrategy.NODE, new NodeStrategyScope());
		strategies.put(ScopeStrategy.PROJECT, new ProjectStrategyScope());
	}

	/**
	 * Resolves a scope to a strategy using the default plugin version behavior ({@code 1.0}).
	 *
	 * @param scope scope string configured on the rule
	 * @return the corresponding strategy (or a default strategy when unknown)
	 */
	public ScopeStrategy getStrategy(String scope) {
		return getStrategy(scope, "1.0");
	}

	/**
	 * Resolves a scope to a strategy, taking plugin-version-specific defaults into account.
	 *
	 * @param scope scope string configured on the rule (may be null)
	 * @param pluginVersion plugin version string used to determine defaults
	 * @return the corresponding strategy (or a default strategy when unknown)
	 */
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
