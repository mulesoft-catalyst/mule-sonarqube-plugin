package com.mulesoft.services.tools.sonarqube.dataweave;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Heuristic detector for commented-out DataWeave code.
 *
 * <p>The goal is to catch the common case (code pasted/disabled inside comments) without
 * requiring a full DWL parser.
 *
 * <p>This detector is intentionally conservative: it targets comments that contain code-like tokens
 * (keywords, mappings, function calls, property access, etc.) and ignores plain explanatory comments.
 */
public final class DataWeaveCommentedOutCodeDetector {

	private static final Pattern CODE_HINT = Pattern.compile(
			"(?is).*("
					+ "%dw\\b"
					+ "|\\b(output|import|ns|var|fun|type|if|else|do|using)\\b"
					+ "|\\b(payload|vars|attributes|error)\\b"
					+ "|---"
					+ "|->"
					+ "|==|!=|<=|>=|="
					+ "|\\b[A-Za-z_][\\w-]*\\s*\\("              // function call
					+ "|\\b[A-Za-z_][\\w-]*\\s*:"               // object key (JSON/DWL mapping)
					+ "|\\b[A-Za-z_][\\w-]*\\.[A-Za-z_][\\w-]*" // property access
					+ "|\\{|\\}|\\(|\\)|\\[|\\]"
					+ ").*");

	private static final Pattern NON_CODE_PREFIX = Pattern.compile("(?is)^\\s*(todo|note|why|because)\\b.*");

	/**
	 * A single detection result.
	 */
	public static final class Finding {
		/**
		 * 1-based line number where the comment starts.
		 */
		public final int line;
		/**
		 * Trimmed snippet of comment content (best-effort, not necessarily full content).
		 */
		public final String snippet;

		public Finding(int line, String snippet) {
			this.line = line;
			this.snippet = snippet;
		}
	}

	/**
	 * Finds occurrences of commented-out code within DataWeave comments.
	 *
	 * <p>Supported comment forms:
	 * <ul>
	 *   <li>single-line comments starting with {@code //} (only when preceded by whitespace)</li>
	 *   <li>block comments starting with {@code /*} and ending with {@code *\/}</li>
	 * </ul>
	 *
	 * <p>Limitations:
	 * <ul>
	 *   <li>This is a heuristic and may have false positives/negatives.</li>
	 *   <li>It does not attempt to parse strings or escape sequences; it uses simple "comment at line start" heuristics.</li>
	 * </ul>
	 *
	 * @param content full DWL file content
	 * @return a list of findings with line numbers; empty when none are found
	 */
	public List<Finding> findFindings(String content) {
		List<Finding> findings = new ArrayList<>();
		if (content == null || content.isEmpty()) {
			return findings;
		}

		String[] lines = content.split("\\r?\\n", -1);
		boolean inBlock = false;
		int blockStartLine = -1;
		StringBuilder blockBody = new StringBuilder();

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			int lineNo = i + 1;

			if (!inBlock) {
				int slIdx = indexOfLineCommentStart(line);
				int blIdx = indexOfBlockCommentStart(line);

				if (blIdx >= 0 && (slIdx < 0 || blIdx < slIdx)) {
					inBlock = true;
					blockStartLine = lineNo;
					blockBody.setLength(0);
					String after = line.substring(blIdx + 2);
					int endIdx = after.indexOf("*/");
					if (endIdx >= 0) {
						// block starts and ends on same line
						blockBody.append(after, 0, endIdx);
						maybeAddFinding(findings, blockStartLine, blockBody.toString());
						inBlock = false;
						blockStartLine = -1;
						blockBody.setLength(0);
					} else {
						blockBody.append(after);
						blockBody.append('\n');
					}
					continue;
				}

				if (slIdx >= 0) {
					String comment = line.substring(slIdx + 2);
					maybeAddFinding(findings, lineNo, comment);
				}
			} else {
				int endIdx = line.indexOf("*/");
				if (endIdx >= 0) {
					blockBody.append(line, 0, endIdx);
					maybeAddFinding(findings, blockStartLine, blockBody.toString());
					inBlock = false;
					blockStartLine = -1;
					blockBody.setLength(0);
				} else {
					blockBody.append(line);
					blockBody.append('\n');
				}
			}
		}

		return findings;
	}

	private static void maybeAddFinding(List<Finding> findings, int line, String commentText) {
		if (looksLikeCommentedOutCode(commentText)) {
			String snippet = commentText == null ? "" : commentText.trim();
			if (snippet.length() > 200) {
				snippet = snippet.substring(0, 200) + "...";
			}
			findings.add(new Finding(line, snippet));
		}
	}

	private static boolean looksLikeCommentedOutCode(String commentText) {
		if (commentText == null) {
			return false;
		}
		String t = commentText.trim();
		if (t.isEmpty()) {
			return false;
		}
		if (NON_CODE_PREFIX.matcher(t).matches()) {
			return false;
		}
		return CODE_HINT.matcher(t).matches();
	}

	/**
	 * Returns index of '//' when it starts a comment (not inside a string),
	 * using a simple heuristic: it must appear after only whitespace.
	 */
	private static int indexOfLineCommentStart(String line) {
		int idx = line.indexOf("//");
		if (idx < 0) {
			return -1;
		}
		for (int i = 0; i < idx; i++) {
			char c = line.charAt(i);
			if (!Character.isWhitespace(c)) {
				return -1;
			}
		}
		return idx;
	}

	/**
	 * Returns index of '/*' when it starts a comment, using the same whitespace-only prefix heuristic.
	 */
	private static int indexOfBlockCommentStart(String line) {
		int idx = line.indexOf("/*");
		if (idx < 0) {
			return -1;
		}
		for (int i = 0; i < idx; i++) {
			char c = line.charAt(i);
			if (!Character.isWhitespace(c)) {
				return -1;
			}
		}
		return idx;
	}
}

