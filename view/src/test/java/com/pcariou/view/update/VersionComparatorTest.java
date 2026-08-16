package com.pcariou.view.update;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies dotted numeric version comparison, including the required cases from
 * the update-checker specification.
 */
public class VersionComparatorTest {

    @Test
    public void patchIncrementIsNewer() {
        assertTrue(VersionComparator.isNewer("1.3.1", "1.3.0"));
    }

    @Test
    public void minorBumpBeatsHigherPatch() {
        assertTrue(VersionComparator.isNewer("1.4.0", "1.3.9"));
    }

    @Test
    public void equalVersionsAreNotNewer() {
        assertEquals(0, VersionComparator.compareVersions("1.3.0", "1.3.0"));
        assertFalse(VersionComparator.isNewer("1.3.0", "1.3.0"));
    }

    @Test
    public void olderPatchIsNotNewer() {
        assertFalse(VersionComparator.isNewer("1.3.0", "1.3.1"));
    }

    @Test
    public void missingTrailingSegmentEqualsZero() {
        assertEquals(0, VersionComparator.compareVersions("1.3", "1.3.0"));
        assertTrue(VersionComparator.isNewer("1.3.1", "1.3"));
    }

    @Test
    public void leadingVAndMetadataAreIgnored() {
        assertEquals(0, VersionComparator.compareVersions("v1.3.0", "1.3.0"));
        assertEquals(0, VersionComparator.compareVersions("1.3.0-rc1", "1.3.0"));
        assertTrue(VersionComparator.isNewer("1.3.1", "1.3.0+build.7"));
    }

    @Test
    public void unknownOrNullIsOldest() {
        assertTrue(VersionComparator.isNewer("1.0.0", "unknown"));
        assertTrue(VersionComparator.isNewer("1.0.0", null));
        assertFalse(VersionComparator.isNewer("unknown", "1.0.0"));
    }

    @Test
    public void multiDigitSegmentsCompareNumerically() {
        assertTrue(VersionComparator.isNewer("1.10.0", "1.9.0"));
        assertTrue(VersionComparator.isNewer("2.0.0", "1.99.99"));
    }

    @Test
    public void hasNumericVersionAcceptsRealReleases() {
        assertTrue(VersionComparator.hasNumericVersion("1.3.1"));
        assertTrue(VersionComparator.hasNumericVersion("v2.0.0"));
        assertTrue(VersionComparator.hasNumericVersion(" 10.20.300 "));
    }

    @Test
    public void hasNumericVersionRejectsNonVersions() {
        assertFalse(VersionComparator.hasNumericVersion(null));
        assertFalse(VersionComparator.hasNumericVersion(""));
        assertFalse(VersionComparator.hasNumericVersion("unknown"));
        assertFalse(VersionComparator.hasNumericVersion("latest"));
        assertFalse(VersionComparator.hasNumericVersion("v"));
    }
}
