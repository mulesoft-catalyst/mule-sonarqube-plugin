package com.mulesoft.services.xpath.javax.util;

import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <p>Utility methods for dealing with {@link org.w3c.dom.NodeList}s.</p>
 * <p>Use {@link org.sonarsource.analyzer.commons.xml.XmlFile#asList} to convert a {@link NodeList to a {@link java.util.List}.</p>
 */
public class NodeLists {
    public static NodeList asNodeList(List<?> list) {
		return new NodeList() {
			@Override
			public Node item(int index) {
				return (Node)list.get(index);
			}

			@Override
			public int getLength() {
				return list.size();
			}
		};
	}
}
