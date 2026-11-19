package com.mulesoft.services.tools.sonarqube.metrics;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Index of the files listed in a MUnit coverage report, used to find the coverage entry that belongs to a
 * given SonarQube input file.
 *
 * <p>Report entries are keyed by their (normalized) path, relative to the Mule sources folder. Looking them up
 * by base name only is wrong as soon as two files in different folders share the same name (for example two
 * {@code map-common-data-model.xml}): the entries would overwrite each other and the coverage lines of one
 * file would be attached to another one.
 *
 * <p>Lookup order for a file with relative path {@code src/main/mule/a/b/x.xml}:
 * <ol>
 *   <li>exact match of the entry name with the path;</li>
 *   <li>the longest entry name that is a path suffix of the file path ({@code a/b/x.xml}); an entry that is
 *       just a file name ({@code x.xml}) matches any file with that name.</li>
 * </ol>
 * An entry that has a folder is never attached to a file in a different folder, even if the file name is the
 * same: a project can contain a file with that name that is not part of the report.
 *
 * @see CoverageSensor
 */
class CoverageIndex {

	private final Map<String, FlowCoverageCounter> entries = new LinkedHashMap<>();

	/**
	 * Adds a report entry. A later entry with the same normalized name replaces the previous one.
	 *
	 * @param name    file name or path as written in the report
	 * @param counter coverage data of the file
	 */
	void put(String name, FlowCoverageCounter counter) {
		entries.put(normalize(name), counter);
	}

	/**
	 * Returns the number of entries in the index.
	 *
	 * @return entries count
	 */
	int size() {
		return entries.size();
	}

	/**
	 * Finds the coverage entry for a file.
	 *
	 * @param relativePath path of the file relative to the project base directory
	 * @return the coverage data, or {@code null} when no entry matches the path
	 */
	FlowCoverageCounter find(String relativePath) {
		String path = normalize(relativePath);

		FlowCoverageCounter exact = entries.get(path);
		if (exact != null) {
			return exact;
		}

		String bestName = null;
		for (String name : entries.keySet()) {
			if (path.endsWith("/" + name) && (bestName == null || name.length() > bestName.length())) {
				bestName = name;
			}
		}
		if (bestName != null) {
			return entries.get(bestName);
		}

		return null;
	}

	/**
	 * Tells whether more than one entry has the same base name as the given path.
	 *
	 * @param relativePath path of the file relative to the project base directory
	 * @return {@code true} if the base name is shared by two or more entries
	 */
	boolean isBaseNameAmbiguous(String relativePath) {
		String base = baseName(normalize(relativePath));
		int count = 0;
		for (String name : entries.keySet()) {
			if (baseName(name).equals(base)) {
				count++;
			}
		}
		return count > 1;
	}

	private static String normalize(String path) {
		return path.replace("\\", "/");
	}

	private static String baseName(String normalizedPath) {
		int idx = normalizedPath.lastIndexOf('/');
		return idx < 0 ? normalizedPath : normalizedPath.substring(idx + 1);
	}
}
