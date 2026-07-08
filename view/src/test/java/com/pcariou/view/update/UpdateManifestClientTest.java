package com.pcariou.view.update;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Verifies parsing of the Community manifest and derived helpers, using a document
 * that mirrors the real {@code latest.json}.
 */
public class UpdateManifestClientTest {

    private static final String MANIFEST =
            "{"
                    + "\"schemaVersion\":1,"
                    + "\"edition\":\"community\","
                    + "\"latestVersion\":\"1.3.1\","
                    + "\"releaseDate\":\"2026-07-07\","
                    + "\"critical\":false,"
                    + "\"downloadPageUrl\":\"https://sepa-xml-generator.com/download/\","
                    + "\"releaseNotesUrl\":\"https://sepa-xml-generator.com/download/\","
                    + "\"downloads\":{"
                    + "  \"windows-x64\":{\"label\":\"Windows x64 MSI\",\"url\":\"https://example.com/app-1.3.1-windows-x64.msi\"},"
                    + "  \"linux-x64\":{\"label\":\"Linux x64 DEB\",\"url\":\"https://example.com/app-1.3.1-linux-x64.deb\"}"
                    + "}"
                    + "}";

    private final UpdateManifestClient client = new UpdateManifestClient();

    @Test
    public void parsesAllScalarFields() throws IOException {
        UpdateInfo info = client.parse(MANIFEST);
        assertEquals(1, info.getSchemaVersion());
        assertEquals("community", info.getEdition());
        assertEquals("1.3.1", info.getLatestVersion());
        assertEquals("2026-07-07", info.getReleaseDate());
        assertFalse(info.isCritical());
        assertTrue(info.hasReleaseDate());
        assertEquals("https://sepa-xml-generator.com/download/", info.getDownloadPageUrl());
    }

    @Test
    public void platformDownloadIsUsedWhenAvailable() throws IOException {
        UpdateInfo info = client.parse(MANIFEST);
        assertEquals("https://example.com/app-1.3.1-windows-x64.msi",
                info.downloadUrlFor("windows-x64"));
    }

    @Test
    public void fallsBackToDownloadPageWhenPlatformMissing() throws IOException {
        UpdateInfo info = client.parse(MANIFEST);
        assertEquals("https://sepa-xml-generator.com/download/",
                info.downloadUrlFor("macos-arm64"));
        assertEquals("https://sepa-xml-generator.com/download/",
                info.downloadUrlFor(null));
    }

    @Test
    public void parsedManifestDrivesNewerComparison() throws IOException {
        UpdateInfo info = client.parse(MANIFEST);
        assertTrue(VersionComparator.isNewer(info.getLatestVersion(), "1.3.0"));
        assertFalse(VersionComparator.isNewer(info.getLatestVersion(), "1.3.1"));
    }

    @Test
    public void malformedJsonFails() {
        try {
            client.parse("{ not valid json ");
            fail("Expected IOException for malformed JSON");
        } catch (IOException expected) {
            // ok
        }
    }

    @Test
    public void manifestWithoutLatestVersionFails() {
        try {
            client.parse("{\"schemaVersion\":1,\"edition\":\"community\"}");
            fail("Expected IOException for missing latestVersion");
        } catch (IOException expected) {
            // ok
        }
    }
}
