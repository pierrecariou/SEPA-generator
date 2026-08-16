package com.pcariou.view.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * Verifies the optional {@code highlights} and {@code releaseNotesUrl} manifest
 * data used by the update dialog.
 *
 * <p>These fields are additive: a manifest without them - or with a malformed
 * value - must still parse and still drive update detection, because the update
 * itself is far more important than its summary.</p>
 */
public class UpdateInfoHighlightsTest {

    private final UpdateManifestClient client = new UpdateManifestClient();

    private static String manifest(String extraFields) {
        return "{"
                + "\"schemaVersion\":1,"
                + "\"edition\":\"community\","
                + "\"latestVersion\":\"1.5.0\","
                + "\"downloadPageUrl\":\"https://sepa-xml-generator.com/download/\""
                + extraFields
                + "}";
    }

    @Test
    public void readsCuratedHighlights() throws IOException {
        UpdateInfo info = client.parse(manifest(
                ",\"highlights\":[\"Faster imports\",\"Clearer errors\"]"));

        assertTrue(info.hasHighlights());
        assertEquals(java.util.Arrays.asList("Faster imports", "Clearer errors"),
                info.getHighlights());
        assertTrue("Optional data must never invalidate the update", info.isValid());
    }

    @Test
    public void absentHighlightsAreAnEmptyList() throws IOException {
        UpdateInfo info = client.parse(manifest(""));

        assertFalse(info.hasHighlights());
        assertTrue(info.getHighlights().isEmpty());
        assertTrue(info.isValid());
    }

    @Test
    public void nullEmptyAndBlankEntriesAreDropped() throws IOException {
        UpdateInfo info = client.parse(manifest(
                ",\"highlights\":[null,\"  \",\"Real change\",\"\"]"));

        assertEquals(java.util.Collections.singletonList("Real change"), info.getHighlights());
    }

    @Test
    public void entriesAreTrimmed() throws IOException {
        UpdateInfo info = client.parse(manifest(",\"highlights\":[\"  Padded  \"]"));

        assertEquals(java.util.Collections.singletonList("Padded"), info.getHighlights());
    }

    @Test
    public void atMostFiveHighlightsAreExposed() throws IOException {
        UpdateInfo info = client.parse(manifest(
                ",\"highlights\":[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\",\"7\"]"));

        List<String> highlights = info.getHighlights();
        assertEquals(UpdateInfo.MAX_HIGHLIGHTS, highlights.size());
        assertEquals("5", highlights.get(4));
    }

    @Test
    public void nullHighlightsValueIsTolerated() throws IOException {
        UpdateInfo info = client.parse(manifest(",\"highlights\":null"));

        assertFalse(info.hasHighlights());
        assertTrue(info.isValid());
    }

    @Test
    public void malformedHighlightsNeverBreakUpdateDetection() throws IOException {
        String[] malformed = {
                ",\"highlights\":\"not an array\"",
                ",\"highlights\":{\"unexpected\":\"object\"}",
                ",\"highlights\":42",
                ",\"highlights\":[1,2,3]",
                ",\"highlights\":[{\"nested\":\"object\"},\"Kept\"]",
        };

        for (String extra : malformed) {
            UpdateInfo info = client.parse(manifest(extra));

            assertTrue("A malformed summary must not invalidate the manifest: " + extra,
                    info.isValid());
            assertEquals("1.5.0", info.getLatestVersion());
        }
    }

    @Test
    public void nonTextEntriesAreSkippedWhileTextIsKept() throws IOException {
        UpdateInfo info = client.parse(manifest(
                ",\"highlights\":[{\"nested\":\"object\"},\"Kept\"]"));

        assertEquals(java.util.Collections.singletonList("Kept"), info.getHighlights());
    }

    @Test
    public void highlightsAreImmutableForCallers() throws IOException {
        UpdateInfo info = client.parse(manifest(",\"highlights\":[\"Only line\"]"));

        try {
            info.getHighlights().add("Injected");
            org.junit.Assert.fail("The curated summary must not be modifiable by callers");
        } catch (UnsupportedOperationException expected) {
            // The dialog reads the summary; nothing may rewrite it.
        }
    }

    @Test
    public void releaseNotesUrlMustBeAbsoluteWebToBeUsable() throws IOException {
        assertFalse(client.parse(manifest("")).hasUsableReleaseNotesUrl());
        assertFalse(client.parse(manifest(",\"releaseNotesUrl\":\"\"")).hasUsableReleaseNotesUrl());
        assertFalse(client.parse(manifest(",\"releaseNotesUrl\":\"/download/\"")).hasUsableReleaseNotesUrl());
        assertFalse(client.parse(manifest(",\"releaseNotesUrl\":\"file:///tmp/notes.html\""))
                .hasUsableReleaseNotesUrl());
        assertTrue(client.parse(manifest(",\"releaseNotesUrl\":\"https://sepa-xml-generator.com/download/\""))
                .hasUsableReleaseNotesUrl());
    }

    /** A Pro manifest must stay rejected no matter what optional data it carries. */
    @Test
    public void highlightsDoNotWeakenTheEditionGuard() {
        try {
            client.parse("{"
                    + "\"schemaVersion\":1,"
                    + "\"edition\":\"pro\","
                    + "\"latestVersion\":\"9.9.9\","
                    + "\"downloadPageUrl\":\"https://sepa-xml-generator.com/pro/\","
                    + "\"highlights\":[\"Tempting line\"]"
                    + "}");
            org.junit.Assert.fail("A Pro manifest must never drive a Community update");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("community"));
        }
    }
}
