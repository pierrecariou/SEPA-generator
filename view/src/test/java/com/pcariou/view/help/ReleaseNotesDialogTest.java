package com.pcariou.view.help;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Assume;
import org.junit.Test;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.KeyEvent;

/**
 * Tests for {@link ReleaseNotesDialog}: the bundled notes of the installed
 * version are presented in a scrollable area, and a version without bundled
 * notes degrades to a calm explanation instead of an error.
 *
 * <p>Only the content panel is built, so the assertions stay headless.</p>
 */
public class ReleaseNotesDialogTest {

    @Test
    public void showsTheBundledNotesOfTheInstalledVersion() {
        JPanel content = ReleaseNotesDialog.buildContent(null, null, "1.4.0");
        String text = HelpDialogText.of(content);

        assertTrue("The bundled notes must be shown", text.contains("pain.001.001.09"));
        assertFalse("Bundled notes must not fall back to the unavailable message",
                text.contains(ReleaseNotesDialog.UNAVAILABLE.split("\n")[0]));
        assertTrue("Long notes must stay scrollable", HelpDialogText.containsScrollPane(content));
    }

    /**
     * The bundled notes already open with their own versioned heading, so the
     * dialog must not add a second one above them.
     */
    @Test
    public void doesNotRepeatTheHeadingAboveTheNotes() {
        JPanel content = ReleaseNotesDialog.buildContent(null, null, "1.4.0");

        for (JLabel label : HelpDialogText.labels(content)) {
            assertFalse("A label duplicating the heading was found: " + label.getText(),
                    label.getText() != null && label.getText().contains("1.4.0"));
        }
    }

    @Test
    public void explainsCalmlyWhenNoNotesAreBundled() {
        JPanel content = ReleaseNotesDialog.buildContent(null, null, "99.99.99");
        String text = HelpDialogText.of(content);

        assertTrue("The user must be told why nothing is shown",
                text.contains("Release notes are not available in this build."));
        assertTrue("The notes must stay reachable online",
                text.contains(ReleaseNotesDialog.VIEW_ONLINE));
        assertFalse("No scrollable notes area when there is nothing to show",
                HelpDialogText.containsScrollPane(content));
    }

    @Test
    public void toleratesAnUnknownVersion() {
        JPanel content = ReleaseNotesDialog.buildContent(null, null, null);
        String text = HelpDialogText.of(content);

        assertTrue(text.contains("Release notes are not available in this build."));
        assertTrue("The notes must stay reachable online",
                text.contains(ReleaseNotesDialog.VIEW_ONLINE));
    }

    @Test
    public void alwaysOffersAWayOut() {
        JPanel content = ReleaseNotesDialog.buildContent(null, null, "1.4.0");

        assertTrue(HelpDialogText.of(content).contains("Close"));
    }

    /** Guards the assumption that the notes area is a scroll pane, not a plain label. */
    @Test
    public void notesAreaIsScrollable() {
        JPanel content = ReleaseNotesDialog.buildContent(null, null, "1.4.0");

        assertTrue(HelpDialogText.firstScrollPane(content) instanceof JScrollPane);
    }

    @Test
    public void bundledNotesClaimNoProOnlyCapability() {
        String notes = ReleaseNotes.forVersion("1.4.0").toLowerCase();

        assertFalse("Community notes must not claim direct debits", notes.contains("pain.008"));
        assertFalse(notes.contains("address readiness"));
        assertFalse(notes.contains("payment profiles"));
        assertFalse("macOS packages are unsigned by default on this branch",
                notes.contains("notariz"));
    }

    /**
     * Links inside the notes must keep opening in the browser. The read-only
     * caret hides the insertion bar only; it must not cost the pane its
     * hyperlink handling.
     */
    @Test
    public void notesKeepTheirHyperlinkHandling() {
        JPanel content = ReleaseNotesDialog.buildContent(null, null, "1.4.0");
        JEditorPane pane = HelpDialogText.editorPane(content);

        assertTrue("The notes must still be rendered as HTML",
                "text/html".equals(pane.getContentType()));
        assertTrue("An activated link must still reach the browser helper",
                pane.getHyperlinkListeners().length > 0);
    }

    /** Esc must still close the dialog after the button rework. */
    @Test
    public void escapeStillClosesTheDialog() {
        Assume.assumeFalse("A real dialog is needed to inspect the root pane bindings",
                GraphicsEnvironment.isHeadless());

        JDialog dialog = new JDialog((Window) null, ReleaseNotesDialog.TITLE);
        try {
            dialog.setContentPane(ReleaseNotesDialog.buildContent(dialog, null, "1.4.0"));
            HelpDialogs.closeOnEscape(dialog);

            JComponent root = dialog.getRootPane();
            Object binding = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));

            assertTrue("Esc must be bound", binding != null);
            assertTrue("Esc must have an action", root.getActionMap().get(binding) != null);
        } finally {
            dialog.dispose();
        }
    }
}
