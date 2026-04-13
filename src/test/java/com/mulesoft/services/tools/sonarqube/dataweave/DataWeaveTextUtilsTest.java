package com.mulesoft.services.tools.sonarqube.dataweave;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link DataWeaveTextUtils}.
 */
public class DataWeaveTextUtilsTest {

	@Test
	public void countLinesReturnsZeroForEmpty() {
		assertEquals(0, DataWeaveTextUtils.countLines(""));
		assertEquals(0, DataWeaveTextUtils.countLines(null));
	}

	@Test
	public void countLinesCountsNewlines() {
		assertEquals(1, DataWeaveTextUtils.countLines("a"));
		assertEquals(2, DataWeaveTextUtils.countLines("a\nb"));
		assertEquals(3, DataWeaveTextUtils.countLines("a\nb\n"));
	}
}

