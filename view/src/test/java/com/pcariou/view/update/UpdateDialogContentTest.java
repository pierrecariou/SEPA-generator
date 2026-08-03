package com.pcariou.view.update;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.formdev.flatlaf.FlatClientProperties;

import org.junit.Test;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;

/**
 * Content tests for {@link UpdateDialog}. They verify that the established
 * presentation (versions, release date, criticality, Not now / Download update)
 * is preserved and that the optional curated summary and release-notes link only
 * appear when the manifest really provides them.
 *
 * <p>Only the content panel is built, so the assertions stay headless.</p>
 */
public class UpdateDialogContentTest {

    private final UpdateManifestClient client = new UpdateManifestClient();

    private static String manifest(String extraFields) {
        return "{"
                + "\"schemaVersion\":1,"
                + "\"edition\":\"community\","
                + "\"latestVersion\":\"1.5.0\","
                + "\"releaseDate\":\"2026-09-01\","
                + "\"downloadPageUrl\":\"https://sepa-xml-generator.com/download/\""
                + extraFields
                + "}";
    }

    private JPanel content(String extraFields) throws IOException {
        UpdateInfo info = client.parse(manifest(extraFields));
        return UpdateDialog.buildContent(null, null, info, "1.4.0");
    }

    private String contentText(String extraFields) throws IOException {
        return text(content(extraFields));
    }

    @Test
    public void keepsTheEstablishedVersionPresentationAndActions() throws IOException {
        String text = contentText("");

        assertTrue(text.contains("Version 1.5.0 is available"));
        assertTrue(text.contains("Current version"));
        assertTrue(text.contains("v1.4.0"));
        assertTrue(text.contains("Latest version"));
        assertTrue(text.contains("v1.5.0"));
        assertTrue(text.contains("Release date"));
        assertTrue(text.contains("2026-09-01"));
        assertTrue(text.contains("Not now"));
        assertTrue(text.contains("Download update"));
    }

    @Test
    public void showsNoSummarySectionWhenTheManifestHasNone() throws IOException {
        String text = contentText("");

        assertFalse(text.contains(UpdateDialog.HIGHLIGHTS_HEADING));
        assertFalse(text.contains(UpdateDialog.FULL_RELEASE_NOTES));
    }

    @Test
    public void showsCuratedHighlightsWhenProvided() throws IOException {
        String text = contentText(",\"highlights\":[\"Faster imports\",\"Clearer errors\"]");

        assertTrue(text.contains(UpdateDialog.HIGHLIGHTS_HEADING));
        assertTrue(text.contains("Faster imports"));
        assertTrue(text.contains("Clearer errors"));
        assertTrue("The established actions must survive the addition",
                text.contains("Download update") && text.contains("Not now"));
    }

    @Test
    public void showsAtMostFiveHighlights() throws IOException {
        String text = contentText(",\"highlights\":[\"one\",\"two\",\"three\",\"four\",\"five\",\"six\"]");

        assertTrue(text.contains("five"));
        assertFalse("The dialog is a summary, not a changelog", text.contains("six"));
    }

    @Test
    public void escapesManifestSuppliedText() throws IOException {
        String text = contentText(",\"highlights\":[\"Handles <b>bold</b> input\"]");

        assertTrue("Manifest text must be shown literally, not interpreted as markup",
                text.contains("Handles &lt;b&gt;bold&lt;/b&gt; input"));
    }

    @Test
    public void offersFullReleaseNotesOnlyForAUsableUrl() throws IOException {
        assertFalse(contentText("").contains(UpdateDialog.FULL_RELEASE_NOTES));
        assertFalse(contentText(",\"releaseNotesUrl\":\"/download/\"")
                .contains(UpdateDialog.FULL_RELEASE_NOTES));
        assertTrue(contentText(",\"releaseNotesUrl\":\"https://sepa-xml-generator.com/download/\"")
                .contains(UpdateDialog.FULL_RELEASE_NOTES));
    }

    @Test
    public void malformedOptionalDataStillRendersTheUpdate() throws IOException {
        String text = contentText(",\"highlights\":\"not an array\",\"releaseNotesUrl\":42");

        assertTrue(text.contains("Version 1.5.0 is available"));
        assertTrue(text.contains("Download update"));
        assertFalse(text.contains(UpdateDialog.HIGHLIGHTS_HEADING));
        assertFalse(text.contains(UpdateDialog.FULL_RELEASE_NOTES));
    }

    @Test
    public void showsTheCriticalNoticeWhenFlagged() throws IOException {
        assertTrue(contentText(",\"critical\":true").contains("important update"));
    }

    /**
     * {@code pressedBorderColor} is not a FlatLaf style key: leaving it in the
     * inline style makes the look and feel raise an unknown-style failure when
     * the dialog is realised. Only the supported hover key may remain.
     */
    @Test
    public void declaresNoUnsupportedFlatLafStyleKey() throws IOException {
        AbstractButton later = button(content(""), "Not now");

        assertNotNull("The 'Not now' action must still exist", later);
        Object style = later.getClientProperty(FlatClientProperties.STYLE);
        String declared = style == null ? "" : style.toString();

        assertFalse("pressedBorderColor is not a supported FlatLaf key",
                declared.contains("pressedBorderColor"));
        assertTrue("The supported hover style must be preserved",
                declared.contains("hoverBorderColor"));
    }

    private static AbstractButton button(Component root, String label) {
        if (root instanceof AbstractButton && label.equals(((AbstractButton) root).getText())) {
            return (AbstractButton) root;
        }
        if (root instanceof Container) {
            for (Component child : ((Container) root).getComponents()) {
                AbstractButton found = button(child, label);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String text(Component root) {
        StringBuilder out = new StringBuilder();
        collect(root, out);
        return out.toString();
    }

    private static void collect(Component component, StringBuilder out) {
        if (component instanceof AbstractButton) {
            append(((AbstractButton) component).getText(), out);
        } else if (component instanceof JLabel) {
            append(((JLabel) component).getText(), out);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                collect(child, out);
            }
        }
    }

    private static void append(String value, StringBuilder out) {
        if (value != null) {
            out.append(value).append('\n');
        }
    }
}
