package com.mulesoft.services.xpath.javax.util;

import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

/**
 * Adapts a {@link Map}&lt;String, String&gt; into a {@link NamespaceContext}. Keys are namespace prefixes and values are namespace URIs.
 */
public class MappedNamespaceContext implements NamespaceContext {
    private Map<String, String> prefixToNamespace;

    public MappedNamespaceContext(Map<String, String> prefixToNamespace) {
        this.prefixToNamespace = prefixToNamespace;
    }
    @Override
    public String getNamespaceURI(String prefix) {
        return prefixToNamespace.get(prefix);
    }

    @Override
    public String getPrefix(String namespaceURI) {
        return prefixToNamespace.entrySet().stream()
            .filter(entry -> entry.getValue().equals(namespaceURI))
            .map(entry -> entry.getKey())
            .findFirst()
            .orElse(null);
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        return prefixToNamespace.entrySet().stream()
            .filter(entry -> entry.getValue().equals(namespaceURI))
            .map(entry -> entry.getKey())
            .iterator();
    }
}
