package de.symeda.sormas.api.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Covers {@link VersionHelper}, which parses and compares the version strings the mobile app and
 * the server exchange when a device asks whether it is allowed to sync.
 *
 * This is also the first test in this module, so it doubles as proof that Surefire picks up
 * sormas-api tests and that main classes are on the test classpath. It deliberately touches no
 * files or resources, so it cannot fail for environmental reasons.
 */
public class VersionHelperTest {

	@Test
	public void extractsAThreePartVersion() {
		assertArrayEquals(new int[] {
			1,
			0,
			14 }, VersionHelper.extractVersion("1.0.14"));
	}

	@Test
	public void returnsNullRatherThanThrowingOnUnparseableInput() {
		// isCompatibleToApi relies on this: it turns a null here into IllegalArgumentException,
		// so anything that changes this to throw would move that error to a different place.
		assertNull(VersionHelper.extractVersion(null));
		assertNull(VersionHelper.extractVersion(""));
		assertNull(VersionHelper.extractVersion("not a version"));
		assertNull(VersionHelper.extractVersion("1.0"));
	}

	@Test
	public void recognisesOnlyAThreePartVersionAsValid() {
		assertTrue(VersionHelper.isVersion(VersionHelper.extractVersion("1.0.14")));
		assertFalse(VersionHelper.isVersion(null));
		assertFalse(VersionHelper.isVersion(new int[] {
			1,
			0 }));
	}

	@Test
	public void comparesVersionsInOrder() {
		int[] older = VersionHelper.extractVersion("1.0.13");
		int[] reference = VersionHelper.extractVersion("1.0.14");
		int[] newer = VersionHelper.extractVersion("1.0.15");

		assertTrue(VersionHelper.isBefore(older, reference));
		assertFalse(VersionHelper.isBefore(reference, reference));
		assertTrue(VersionHelper.isAfter(newer, reference));
		assertFalse(VersionHelper.isAfter(reference, reference));
		assertTrue(VersionHelper.isEqual(reference, VersionHelper.extractVersion("1.0.14")));
	}

	@Test
	public void comparesAcrossMinorAndMajorBoundaries() {
		assertTrue(VersionHelper.isBefore(VersionHelper.extractVersion("1.0.99"), VersionHelper.extractVersion("1.1.0")));
		assertTrue(VersionHelper.isBefore(VersionHelper.extractVersion("1.99.99"), VersionHelper.extractVersion("2.0.0")));
	}
}
