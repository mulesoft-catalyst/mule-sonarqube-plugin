package com.mulesoft.services.tools.sonarqube.rule.scope;

import java.io.IOException;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.sonarsource.analyzer.commons.xml.XmlTextRange;
import org.w3c.dom.Element;

final class IssueLocations {
	private IssueLocations() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Returns a best-effort "safe" range to anchor file-level issues.
	 *
	 * SonarQube requires start &lt; end; a zero-length range is rejected.
	 */
	static TextRange primaryRange(InputFile file) {
		if (file == null) {
			return null;
		}
		try {
			// Prefer anchoring to the root element name (stable and non-empty).
			XmlFile xmlFile = XmlFile.create(file);
			Element root = xmlFile.getDocument().getDocumentElement();
			if (root != null) {
				XmlTextRange r = XmlFile.nameLocation(root);
				if (r != null) {
					return file.newRange(r.getStartLine(), r.getStartColumn(), r.getEndLine(), r.getEndColumn());
				}
			}
		} catch (IOException | RuntimeException e) {
			// ignore; we'll fall back to "no range"
		}
		return null;
	}
}

