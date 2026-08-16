package com.pcariou.view.update;

import org.junit.Test;

import java.io.IOException;
import java.util.prefs.Preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the synchronous decision logic of {@link UpdateCheckService} with a
 * stubbed manifest client, covering the update-available, up-to-date and failure
 * outcomes without touching the network.
 */
public class UpdateCheckServiceTest {

    private static UpdateInfo manifest(String latestVersion) throws IOException {
        return new UpdateManifestClient().parse(
                "{\"schemaVersion\":1,\"edition\":\"community\",\"latestVersion\":\"" + latestVersion + "\","
                        + "\"downloadPageUrl\":\"https://sepa-xml-generator.com/download/\"}");
    }

    /** Client stub returning a fixed manifest. */
    private static UpdateManifestClient returning(final UpdateInfo info) {
        return new UpdateManifestClient() {
            @Override
            public UpdateInfo fetch(String manifestUrl) {
                return info;
            }
        };
    }

    /** Client stub that always fails, simulating an offline / bad-response situation. */
    private static UpdateManifestClient failing() {
        return new UpdateManifestClient() {
            @Override
            public UpdateInfo fetch(String manifestUrl) throws IOException {
                throw new IOException("offline");
            }
        };
    }

    private static UpdatePreferences prefs() {
        return new UpdatePreferences(); // not exercised by the synchronous check()
    }

    @Test
    public void reportsUpdateWhenManifestIsNewer() throws IOException {
        UpdateInfo info = manifest("1.3.1");
        UpdateCheckService service = new UpdateCheckService(returning(info), prefs(), "unused");

        UpdateCheckResult result = service.check("1.3.0");

        assertEquals(UpdateCheckResult.Status.UPDATE_AVAILABLE, result.getStatus());
        assertSame(info, result.getInfo());
        assertEquals("1.3.0", result.getCurrentVersion());
    }

    @Test
    public void reportsUpToDateWhenSameVersion() throws IOException {
        UpdateCheckService service = new UpdateCheckService(returning(manifest("1.3.0")), prefs(), "unused");

        UpdateCheckResult result = service.check("1.3.0");

        assertEquals(UpdateCheckResult.Status.UP_TO_DATE, result.getStatus());
    }

    @Test
    public void reportsUpToDateWhenLocalIsNewer() throws IOException {
        UpdateCheckService service = new UpdateCheckService(returning(manifest("1.3.0")), prefs(), "unused");

        UpdateCheckResult result = service.check("1.4.0");

        assertEquals(UpdateCheckResult.Status.UP_TO_DATE, result.getStatus());
    }

    @Test
    public void reportsFailureWhenFetchThrows() {
        UpdateCheckService service = new UpdateCheckService(failing(), prefs(), "unused");

        UpdateCheckResult result = service.check("1.3.0");

        assertEquals(UpdateCheckResult.Status.FAILED, result.getStatus());
        assertNull(result.getInfo());
    }

    @Test
    public void cachedResultReflectsStoredManifestWithoutNetwork() throws Exception {
        Preferences node = Preferences.userRoot().node("com/pcariou/view/update/test-cached-result");
        try {
            UpdatePreferences prefs = new UpdatePreferences(node);
            prefs.putCachedManifest("{\"schemaVersion\":1,\"edition\":\"community\",\"latestVersion\":\"1.3.1\","
                    + "\"downloadPageUrl\":\"https://sepa-xml-generator.com/download/\"}");

            // Client that would fail if used, proving the cache path avoids the network.
            UpdateCheckService service = new UpdateCheckService(failing(), prefs, "unused");

            UpdateCheckResult available = service.cachedResult("1.3.0");
            assertEquals(UpdateCheckResult.Status.UPDATE_AVAILABLE, available.getStatus());
            assertEquals("1.3.1", available.getInfo().getLatestVersion());

            UpdateCheckResult current = service.cachedResult("1.3.1");
            assertEquals(UpdateCheckResult.Status.UP_TO_DATE, current.getStatus());
            assertTrue(prefs.hasCachedManifest());
        } finally {
            node.removeNode();
        }
    }

    @Test
    public void cachedResultIsNullWhenNothingCached() throws Exception {
        Preferences node = Preferences.userRoot().node("com/pcariou/view/update/test-empty-cache");
        try {
            UpdatePreferences prefs = new UpdatePreferences(node);
            UpdateCheckService service = new UpdateCheckService(returning(manifest("1.3.1")), prefs, "unused");

            assertNull(service.cachedResult("1.3.0"));
        } finally {
            node.removeNode();
        }
    }
}
