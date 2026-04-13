package com.mulesoft.services.tools.sonarqube.dataweave;

/**
 * Small utilities for DataWeave text processing.
 */
public final class DataWeaveTextUtils {

	private DataWeaveTextUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Counts the number of lines in a string.
	 *
	 * <p>Uses '\n' as the line separator (treating both LF and CRLF as containing '\n' once decoded).
	 *
	 * <p>Examples:
	 * <ul>
	 *   <li>{@code "a"} -> 1</li>
	 *   <li>{@code "a\nb"} -> 2</li>
	 *   <li>{@code "a\nb\n"} -> 3</li>
	 * </ul>
	 *
	 * @param content text to inspect
	 * @return line count, or 0 for null/empty input
	 */
	public static int countLines(String content) {
		if (content == null || content.isEmpty()) {
			return 0;
		}
		int lines = 1;
		for (int i = 0; i < content.length(); i++) {
			if (content.charAt(i) == '\n') {
				lines++;
			}
		}
		return lines;
	}
}

