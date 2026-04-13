package com.mulesoft.services.tools.sonarqube.dataweave;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/**
 * Unit tests for {@link DataWeaveCommentedOutCodeDetector}.
 */
public class DataWeaveCommentedOutCodeDetectorTest {

	@Test
	public void detectsSingleLineCommentedOutCode() {
		String dwl = ""
				+ "%dw 2.0\n"
				+ "output application/json\n"
				+ "// var x = 1\n"
				+ "{ a: 1 }\n";

		DataWeaveCommentedOutCodeDetector d = new DataWeaveCommentedOutCodeDetector();
		List<DataWeaveCommentedOutCodeDetector.Finding> findings = d.findFindings(dwl);
		assertEquals(1, findings.size());
		assertEquals(3, findings.get(0).line);
	}

	@Test
	public void detectsBlockCommentedOutCode() {
		String dwl = ""
				+ "%dw 2.0\n"
				+ "/*\n"
				+ "output application/json\n"
				+ "---\n"
				+ "{ a: 1 }\n"
				+ "*/\n"
				+ "{ b: 2 }\n";

		DataWeaveCommentedOutCodeDetector d = new DataWeaveCommentedOutCodeDetector();
		List<DataWeaveCommentedOutCodeDetector.Finding> findings = d.findFindings(dwl);
		assertEquals(1, findings.size());
		assertEquals(2, findings.get(0).line);
	}

	@Test
	public void ignoresTrailingDoubleSlashInCodeLine() {
		String dwl = ""
				+ "%dw 2.0\n"
				+ "output application/json\n"
				+ "{ url: \"http://example\" }\n";

		DataWeaveCommentedOutCodeDetector d = new DataWeaveCommentedOutCodeDetector();
		assertTrue(d.findFindings(dwl).isEmpty());
	}

	@Test
	public void detectsCommentedOutMappingEntry() {
		String dwl = ""
				+ "%dw 2.0\n"
				+ "output application/json\n"
				+ "---\n"
				+ "{\n"
				+ "  // foo: payload.bar\n"
				+ "  a: 1\n"
				+ "}\n";

		DataWeaveCommentedOutCodeDetector d = new DataWeaveCommentedOutCodeDetector();
		List<DataWeaveCommentedOutCodeDetector.Finding> findings = d.findFindings(dwl);
		assertEquals(1, findings.size());
		assertEquals(5, findings.get(0).line);
	}
}

