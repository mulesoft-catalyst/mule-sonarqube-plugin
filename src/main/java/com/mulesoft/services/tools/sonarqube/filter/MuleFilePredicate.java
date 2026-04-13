package com.mulesoft.services.tools.sonarqube.filter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.mulesoft.services.tools.sonarqube.xml.SecureSaxBuilder;

/**
 * Filters SonarQube {@link InputFile}s to only those that look like Mule configuration files.
 *
 * <p>The predicate first checks the filename against configured suffixes, then parses the XML
 * and verifies that the root element belongs to Mule’s core namespace. This protects the sensor
 * from running on non-Mule XML files that happen to share the same suffix.
 *
 * @version 1.1.0
 * @since 1.1.0
 * @see com.mulesoft.services.tools.sonarqube.sensor.MuleSensor
 */
public class MuleFilePredicate implements FilePredicate {

	private final Logger logger = Loggers.get(MuleFilePredicate.class);
	SAXBuilder saxBuilder = SecureSaxBuilder.create();
	String muleNamespace = "http://www.mulesoft.org/schema/mule/core";
	String[] fileExtensions;

	/**
	 * Creates a predicate that recognizes Mule files by suffix (and by Mule namespace).
	 *
	 * <p>Null/blank suffix entries are ignored. When no suffixes are provided, {@code .xml}
	 * is used as a default.
	 *
	 * @param aFileSuffixes file suffixes to consider (for example {@code ".xml"}); may be null
	 */
	public MuleFilePredicate(String[] aFileSuffixes) {
		super();
		if (aFileSuffixes == null || aFileSuffixes.length == 0) {
			fileExtensions = new String[] { ".xml" };
		} else {
			java.util.List<String> cleaned = new java.util.ArrayList<>();
			for (String s : aFileSuffixes) {
				if (s == null) {
					continue;
				}
				String t = s.trim();
				if (!t.isEmpty()) {
					cleaned.add(t);
				}
			}
			fileExtensions = cleaned.isEmpty() ? new String[] { ".xml" } : cleaned.toArray(new String[0]);
		}
	}

	/**
	 * Determines whether the given file should be treated as a Mule configuration XML file.
	 *
	 * <p>A file matches when:
	 * <ul>
	 *   <li>its name ends with one of the configured suffixes</li>
	 *   <li>its root element namespace URI equals {@code http://www.mulesoft.org/schema/mule/core}</li>
	 * </ul>
	 *
	 * <p>Non-Mule or unparseable XML files are treated as a non-match so analysis can continue. Parse
	 * errors are logged at debug level to avoid noisy logs when projects contain unrelated XML files.
	 *
	 * @param inputFile the SonarQube file to test
	 * @return {@code true} when the file is a Mule configuration file; {@code false} otherwise
	 */
	@Override
	public boolean apply(InputFile inputFile) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing Mule Sensor on file:" + inputFile.filename());
		}

		for (String fileExtension : fileExtensions) {
			if (inputFile.filename().endsWith(fileExtension)) {
				try {
					// Fast path: avoid parsing XML that doesn't even reference Mule's core namespace.
					// This prevents noisy parse errors on unrelated XML files (or non-XML files with .xml extension).
					if (!containsMuleNamespaceHint(inputFile)) {
						return false;
					}

					byte[] bytes;
					try (InputStream in = inputFile.inputStream()) {
						bytes = readAllBytes(in);
					}

					Document document = saxBuilder.build(new ByteArrayInputStream(bytes));

					String namespace = document.getRootElement().getNamespaceURI();
					if (muleNamespace.equals(namespace))
						return true;
				} catch (JDOMException | IOException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipping unparseable/non-Mule XML file: {} ({})", inputFile.filename(),
								e.getMessage());
					}
				}
			}
		}
		return false;
	}

	private boolean containsMuleNamespaceHint(InputFile inputFile) {
		// The Mule core namespace is ASCII; a UTF-8 decode of the leading bytes is sufficient for the hint check.
		final int maxBytes = 64 * 1024;
		try (InputStream in = inputFile.inputStream()) {
			ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(8192, maxBytes));
			byte[] buf = new byte[8192];
			int total = 0;
			int r;
			while (total < maxBytes && (r = in.read(buf, 0, Math.min(buf.length, maxBytes - total))) >= 0) {
				out.write(buf, 0, r);
				total += r;
			}
			String head = new String(out.toByteArray(), StandardCharsets.UTF_8);
			return head.contains(muleNamespace);
		} catch (IOException e) {
			// If we can't read the file, treat as non-match to keep analysis resilient.
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to read XML file header for {} ({})", inputFile.filename(), e.getMessage());
			}
			return false;
		}
	}

	private static byte[] readAllBytes(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		int r;
		while ((r = in.read(buf)) >= 0) {
			out.write(buf, 0, r);
		}
		return out.toByteArray();
	}

}
