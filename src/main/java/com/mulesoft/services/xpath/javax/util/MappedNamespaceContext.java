package com.mulesoft.services.xpath.javax.util;

import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

/**
 * Adapts a {@link Map}&lt;String, String&gt; into a {@link NamespaceContext}.
 * Keys are namespace prefixes and values are namespace URIs.
 *
 * @version 1.1.0
 * @since 1.1.0
 */
public class MappedNamespaceContext implements NamespaceContext {
	private final Map<String, String> prefixToNamespace;

	/**
	 * Creates a namespace context backed by the given map.
	 *
	 * @param prefixToNamespace mapping from prefix to namespace URI
	 */
	public MappedNamespaceContext(Map<String, String> prefixToNamespace) {
		this.prefixToNamespace = prefixToNamespace;
	}

	/**
	 * Resolves a namespace URI for the given prefix.
	 *
	 * @param prefix prefix to resolve
	 * @return namespace URI, or null when no mapping exists
	 */
	@Override
	public String getNamespaceURI(String prefix) {
		return prefixToNamespace.get(prefix);
	}

	/**
	 * Finds a single prefix for the given namespace URI.
	 *
	 * @param namespaceURI namespace URI
	 * @return a matching prefix, or null when none exists
	 */
	@Override
	public String getPrefix(String namespaceURI) {
		return prefixToNamespace.entrySet().stream().filter(entry -> entry.getValue().equals(namespaceURI))
				.map(Map.Entry::getKey).findFirst().orElse(null);
	}

	/**
	 * Returns all prefixes that map to the given namespace URI.
	 *
	 * @param namespaceURI namespace URI
	 * @return iterator of matching prefixes
	 */
	@Override
	public Iterator<String> getPrefixes(String namespaceURI) {
		return prefixToNamespace.entrySet().stream().filter(entry -> entry.getValue().equals(namespaceURI))
				.map(Map.Entry::getKey).iterator();
	}
}

