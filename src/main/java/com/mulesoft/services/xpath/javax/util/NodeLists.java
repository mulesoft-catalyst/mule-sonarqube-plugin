package com.mulesoft.services.xpath.javax.util;

import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility methods for dealing with {@link NodeList}s.
 *
 * Use {@link org.sonarsource.analyzer.commons.xml.XmlFile#asList(NodeList)} to convert a {@link NodeList} to a {@link List}.
 *
 * @version 1.1.0
 * @since 1.1.0
 */
public final class NodeLists {
	private NodeLists() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Wraps a {@link List} of {@link Node}s as a {@link NodeList}.
	 *
	 * @param list list containing DOM nodes
	 * @return a {@link NodeList} view over the list
	 */
	public static NodeList asNodeList(List<?> list) {
		return new NodeList() {
			@Override
			public Node item(int index) {
				return (Node) list.get(index);
			}

			@Override
			public int getLength() {
				return list.size();
			}
		};
	}
}

