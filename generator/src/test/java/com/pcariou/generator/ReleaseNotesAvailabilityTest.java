package com.pcariou.generator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.pcariou.view.help.ReleaseNotes;

import org.junit.Test;

/**
 * Release safety: the curated release notes shipped with the application must
 * exist for the version the application actually reports.
 *
 * <p>Help &rarr; Release notes is offered unconditionally and works offline, so a
 * release whose notes were never written would silently show the "not available"
 * fallback to every customer. This test makes that a build failure instead: when
 * the version in the POM is raised, the matching
 * {@code /help/release-notes-<version>.html} must be added in the same change.</p>
 */
public class ReleaseNotesAvailabilityTest {

    @Test
    public void notesAreBundledForTheApplicationVersion() {
        String version = AppInfo.getVersion();
        String notes = ReleaseNotes.forVersion(version);

        assertNotNull(notes);
        assertFalse("No release notes are bundled for version " + version
                        + ". Add view/src/main/resources/help/release-notes-"
                        + ReleaseNotes.normalizeVersion(version) + ".html",
                notes.isEmpty());
    }

    @Test
    public void bundledNotesNameTheirRelease() {
        String release = ReleaseNotes.normalizeVersion(AppInfo.getVersion());
        String notes = ReleaseNotes.forVersion(release);

        assertNotNull("The application version must resolve to a release", release);
        assertTrue("The release notes must state the release they describe",
                notes.contains(release));
    }

    @Test
    public void bundledNotesAreNotAPlaceholder() {
        String notes = ReleaseNotes.forVersion(AppInfo.getVersion());

        assertFalse("Release notes must be curated, not a leftover template",
                notes.contains("TODO") || notes.contains("${"));
    }

    /**
     * The notes are shipped through a filtered Maven resource directory: an
     * unresolved token would reach customers verbatim.
     */
    @Test
    public void bundledNotesCarryNoResourceFilteringToken() {
        String notes = ReleaseNotes.forVersion(AppInfo.getVersion());

        assertFalse("A Maven property placeholder survived resource filtering",
                notes.contains("${"));
        assertFalse("An @-delimited filtering token survived resource filtering",
                notes.matches("(?s).*@[A-Za-z0-9._-]+@.*"));
    }

    /** Community notes must never advertise a capability this edition does not have. */
    @Test
    public void bundledNotesClaimNoProOnlyCapability() {
        String notes = ReleaseNotes.forVersion(AppInfo.getVersion()).toLowerCase();

        assertFalse(notes.contains("pain.008"));
        assertFalse(notes.contains("direct debit generation"));
        assertFalse(notes.contains("address readiness"));
        assertFalse(notes.contains("saved import mappings"));
        assertFalse("macOS packages are unsigned by default on this branch",
                notes.contains("notariz"));
    }
}
