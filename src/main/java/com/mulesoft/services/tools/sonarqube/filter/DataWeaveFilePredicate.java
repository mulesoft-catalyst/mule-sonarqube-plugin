package com.mulesoft.services.tools.sonarqube.filter;

import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;

/**
 * Filters SonarQube {@link InputFile}s to only those matching configured DataWeave suffixes.
 *
 * <p>This predicate is intentionally suffix-based only (no parsing), because DataWeave files are
 * plain text and should never be fed through the XML/Mule namespace detection logic.
 */
public class DataWeaveFilePredicate implements FilePredicate {

	private final String[] fileExtensions;

	/**
	 * Creates a predicate that matches files by suffix.
	 *
	 * @param aFileSuffixes suffixes to match (for example {@code ".dwl"}). If null/empty/blank, defaults to {@code ".dwl"}.
	 */
	public DataWeaveFilePredicate(String[] aFileSuffixes) {
		if (aFileSuffixes == null || aFileSuffixes.length == 0) {
			fileExtensions = new String[] { ".dwl" };
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
			fileExtensions = cleaned.isEmpty() ? new String[] { ".dwl" } : cleaned.toArray(new String[0]);
		}
	}

	/**
	 * Returns {@code true} when the {@link InputFile#filename()} ends with one of the configured suffixes.
	 *
	 * <p>Comparison is case-sensitive (consistent with how SonarQube typically treats suffix configuration).
	 */
	@Override
	public boolean apply(InputFile inputFile) {
		String name = inputFile.filename();
		for (String fileExtension : fileExtensions) {
			if (name.endsWith(fileExtension)) {
				return true;
			}
		}
		return false;
	}
}

