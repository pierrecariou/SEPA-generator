package com.pcariou.generator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Guards the single source of truth for the application version.
 *
 * <p>The runtime version is filtered from the Maven module version into
 * {@code app.properties} ({@code version=${project.version}}) and read back by
 * {@link AppInfo#getVersion()}. This test fails fast if that wiring breaks so the
 * version cannot silently drift from the POM (e.g. resource filtering disabled or
 * the placeholder shipped verbatim). It intentionally does not hard-code the
 * release number, which would itself become a competing source of truth.
 */
public class AppVersionTest {

    @Test
    public void versionIsResolvedFromMavenNotAPlaceholder() {
        String version = AppInfo.getVersion();

        assertTrue("Version must be resolved", version != null && !version.isEmpty());
        assertFalse("Version must not be the unresolved Maven placeholder",
                version.startsWith("$"));
        assertFalse("Version must be resolved, not 'unknown'",
                "unknown".equals(version));
    }

    @Test
    public void versionIsADottedNumericRelease() {
        String version = AppInfo.getVersion();

        assertTrue("Version '" + version + "' must be a dotted numeric release (e.g. 1.4.0)",
                version.matches("\\d+(\\.\\d+)+"));
    }
}
