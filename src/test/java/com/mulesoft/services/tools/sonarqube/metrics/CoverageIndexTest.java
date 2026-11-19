package com.mulesoft.services.tools.sonarqube.metrics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Verifies how {@link CoverageIndex} matches MUnit report entries to project files, in particular when several
 * files share the same file name in different folders.
 */
public class CoverageIndexTest {

	private static final String ROOT = "src/main/mule/";

	/** Files with the same name must each get their own entry, not the last one in the report. */
	@Test
	public void sameFileNameInDifferentFoldersGetTheirOwnCoverage() {
		FlowCoverageCounter a = new FlowCoverageCounter();
		FlowCoverageCounter b = new FlowCoverageCounter();
		FlowCoverageCounter c = new FlowCoverageCounter();
		CoverageIndex index = new CoverageIndex();
		index.put("dirA/map-common-data-model.xml", a);
		index.put("dirB/map-common-data-model.xml", b);
		index.put("dirC/sub/map-common-data-model.xml", c);

		assertSame(a, index.find(ROOT + "dirA/map-common-data-model.xml"));
		assertSame(b, index.find(ROOT + "dirB/map-common-data-model.xml"));
		assertSame(c, index.find(ROOT + "dirC/sub/map-common-data-model.xml"));
	}

	/** Windows separators in the report or in the path must not change the result. */
	@Test
	public void backslashesAreNormalized() {
		FlowCoverageCounter a = new FlowCoverageCounter();
		CoverageIndex index = new CoverageIndex();
		index.put("dirA\\x.xml", a);

		assertSame(a, index.find("src\\main\\mule\\dirA\\x.xml"));
	}

	/** When the base name is shared and no entry matches the path, nothing is attached. */
	@Test
	public void ambiguousBaseNameWithoutPathMatchReturnsNull() {
		CoverageIndex index = new CoverageIndex();
		index.put("dirA/x.xml", new FlowCoverageCounter());
		index.put("dirB/x.xml", new FlowCoverageCounter());

		assertNull(index.find(ROOT + "dirC/x.xml"));
		assertTrue(index.isBaseNameAmbiguous(ROOT + "dirC/x.xml"));
	}

	/** A report entry with the file name only is still used when it is the only one with that name. */
	@Test
	public void uniqueBaseNameMatchesWhenReportHasNoFolder() {
		FlowCoverageCounter a = new FlowCoverageCounter();
		CoverageIndex index = new CoverageIndex();
		index.put("x.xml", a);

		assertSame(a, index.find(ROOT + "dirA/x.xml"));
		assertFalse(index.isBaseNameAmbiguous(ROOT + "dirA/x.xml"));
	}

	/** The most specific (longest) suffix wins over a shorter one. */
	@Test
	public void longestSuffixWins() {
		FlowCoverageCounter shortEntry = new FlowCoverageCounter();
		FlowCoverageCounter longEntry = new FlowCoverageCounter();
		CoverageIndex index = new CoverageIndex();
		index.put("sub/x.xml", shortEntry);
		index.put("dirA/sub/x.xml", longEntry);

		assertSame(longEntry, index.find(ROOT + "dirA/sub/x.xml"));
		assertSame(shortEntry, index.find(ROOT + "other/sub/x.xml"));
	}

	/** An entry with a folder is not attached to a same-named file in another folder that is not in the report. */
	@Test
	public void sameNameInAnotherFolderIsNotMatched() {
		CoverageIndex index = new CoverageIndex();
		index.put("dirA/x.xml", new FlowCoverageCounter());

		assertNull(index.find(ROOT + "dirB/x.xml"));
	}

	/** A file that is not in the report gets no coverage. */
	@Test
	public void unknownFileReturnsNull() {
		CoverageIndex index = new CoverageIndex();
		index.put("dirA/x.xml", new FlowCoverageCounter());

		assertNull(index.find(ROOT + "dirA/y.xml"));
	}
}
