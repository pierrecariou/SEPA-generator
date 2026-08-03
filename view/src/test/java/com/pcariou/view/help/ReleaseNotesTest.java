package com.pcariou.view.help;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for {@link ReleaseNotes}: how the running application version is turned
 * into a bundled resource, and that a missing resource is a normal empty result
 * rather than a failure.
 */
public class ReleaseNotesTest {

    @Test
    public void normalizesPlainRelease() {
        assertEquals("1.4.0", ReleaseNotes.normalizeVersion("1.4.0"));
        assertEquals("1.4.0", ReleaseNotes.normalizeVersion("  1.4.0  "));
        assertEquals("2.1", ReleaseNotes.normalizeVersion("2.1"));
    }

    @Test
    public void dropsDevelopmentAndBuildSuffixes() {
        assertEquals("1.4.0", ReleaseNotes.normalizeVersion("1.4.0-SNAPSHOT"));
        assertEquals("1.4.0", ReleaseNotes.normalizeVersion("1.4.0-rc1"));
        assertEquals("1.4.0", ReleaseNotes.normalizeVersion("1.4.0+build.7"));
        assertEquals("1.4.0", ReleaseNotes.normalizeVersion("1.4.0+build-9"));
    }

    @Test
    public void rejectsVersionsThatAreNotReleases() {
        assertNull(ReleaseNotes.normalizeVersion(null));
        assertNull(ReleaseNotes.normalizeVersion(""));
        assertNull(ReleaseNotes.normalizeVersion("   "));
        assertNull(ReleaseNotes.normalizeVersion("unknown"));
        assertNull("The unresolved Maven placeholder must never become a lookup key",
                ReleaseNotes.normalizeVersion("${project.version}"));
        assertNull("A path must never be usable as a version",
                ReleaseNotes.normalizeVersion("../secrets"));
    }

    @Test
    public void resourcePathIsDerivedFromTheReleaseOnly() {
        assertEquals("/help/release-notes-1.4.0.html", ReleaseNotes.resourcePath("1.4.0"));
    }

    @Test
    public void missingNotesYieldAnEmptyResultInsteadOfFailing() {
        assertEquals("", ReleaseNotes.forVersion("99.99.99"));
        assertEquals("", ReleaseNotes.forVersion(null));
        assertEquals("", ReleaseNotes.forVersion("unknown"));
        assertFalse(ReleaseNotes.areAvailableFor("99.99.99"));
    }

    @Test
    public void bundledNotesAreLoadedForAReleaseAndItsDevelopmentBuilds() {
        String notes = ReleaseNotes.forVersion("1.4.0");

        assertFalse("The 1.4.0 notes must be bundled", notes.isEmpty());
        assertTrue("The notes must name the release they describe", notes.contains("1.4.0"));
        assertTrue(ReleaseNotes.areAvailableFor("1.4.0-SNAPSHOT"));
        assertEquals(notes, ReleaseNotes.forVersion("1.4.0+build.3"));
    }

    @Test
    public void bundledNotesCarryNoUnresolvedPlaceholder() {
        String notes = ReleaseNotes.forVersion("1.4.0");

        assertFalse("Resource filtering must not leave a Maven placeholder in the notes",
                notes.contains("${"));
    }
}
