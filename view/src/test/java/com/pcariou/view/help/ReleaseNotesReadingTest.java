package com.pcariou.view.help;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.pcariou.view.AppLinks;

import org.junit.Test;

import javax.swing.AbstractButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.text.Caret;
import java.awt.Cursor;

/**
 * Reading affordances of the release-notes document and of its online action.
 *
 * <p>The notes must read as a document, not as a form: the text stays
 * selectable and copyable, but nothing suggests it can be typed into. These
 * tests pin that balance, because the obvious ways to hide the caret (disabling
 * the caret, or making the component non-focusable) would silently take
 * selection, copying or keyboard access away with it.</p>
 */
public class ReleaseNotesReadingTest {

    private static JEditorPane notesPane() {
        JPanel content = ReleaseNotesDialog.buildContent(null, null, "1.4.0");
        JEditorPane pane = HelpDialogText.editorPane(content);
        assertNotNull("The notes must be presented in a text component", pane);
        return pane;
    }

    @Test
    public void notesAreReadOnly() {
        assertFalse("The notes are a document, not an editor", notesPane().isEditable());
    }

    @Test
    public void notesRemainSelectableAndCopyable() {
        JEditorPane pane = notesPane();

        pane.selectAll();
        String selected = pane.getSelectedText();

        assertNotNull("Selection must still work in the read-only document", selected);
        assertTrue("The selection must carry the actual notes text",
                selected.contains("pain.001.001.09"));
        assertNotNull("Copy relies on the caret being present", pane.getCaret());
    }

    @Test
    public void notesKeepKeyboardAccess() {
        JEditorPane pane = notesPane();

        assertTrue("The document must stay reachable with Tab and readable with the keyboard",
                pane.isFocusable());
    }

    @Test
    public void insertionCaretIsNeverPainted() {
        Caret caret = notesPane().getCaret();

        assertFalse("A visible caret would suggest the notes can be edited", caret.isVisible());

        // Focus gain is what would normally show the insertion bar.
        caret.setVisible(true);
        assertFalse("The caret must stay hidden even once the document has focus",
                caret.isVisible());
    }

    @Test
    public void hiddenCaretDoesNotBlink() {
        assertEquals("An invisible caret must not keep a blink timer running",
                0, notesPane().getCaret().getBlinkRate());
    }

    @Test
    public void bodyTextDoesNotPretendToBeClickable() {
        assertFalse("Only real links carry a hand cursor",
                notesPane().getCursor().getType() == Cursor.HAND_CURSOR);
    }

    @Test
    public void viewOnlineResolvesToTheInstalledVersionPage() {
        assertEquals("https://sepa-xml-generator.com/releases/community/1.4.0/",
                ReleaseNotes.onlineUrl("1.4.0"));
    }

    @Test
    public void viewOnlineFollowsTheInstalledVersion() {
        assertEquals("https://sepa-xml-generator.com/releases/community/1.3.1/",
                ReleaseNotes.onlineUrl("1.3.1"));
        assertEquals("https://sepa-xml-generator.com/releases/community/2.0.0/",
                ReleaseNotes.onlineUrl("2.0.0"));
    }

    @Test
    public void developmentBuildsLinkToTheReleaseTheyAreBasedOn() {
        assertEquals("https://sepa-xml-generator.com/releases/community/1.4.0/",
                ReleaseNotes.onlineUrl("1.4.0-SNAPSHOT"));
        assertEquals("https://sepa-xml-generator.com/releases/community/1.4.0/",
                ReleaseNotes.onlineUrl("1.4.0+build.7"));
    }

    /** The online page must be resolved exactly like the bundled notes are. */
    @Test
    public void onlineUrlFollowsTheBundledNoteNormalisation() {
        for (String version : new String[] {"1.4.0", "1.4.0-SNAPSHOT", "1.4.0+build.7", " 1.4.0 "}) {
            assertEquals("Same normalisation for notes and link: " + version,
                    ReleaseNotes.onlineUrl("1.4.0"), ReleaseNotes.onlineUrl(version));
        }
    }

    @Test
    public void anUnresolvableVersionFallsBackToTheProductWebsite() {
        assertEquals(AppLinks.WEBSITE, ReleaseNotes.onlineUrl(null));
        assertEquals(AppLinks.WEBSITE, ReleaseNotes.onlineUrl("unknown"));
        assertEquals(AppLinks.WEBSITE, ReleaseNotes.onlineUrl("${project.version}"));
    }

    /** The Community release archive must never point at the Pro one. */
    @Test
    public void theOnlinePageStaysOnTheCommunityRoute() {
        String url = ReleaseNotes.onlineUrl("1.4.0");

        assertTrue(url.startsWith("https://sepa-xml-generator.com/releases/community/"));
        assertFalse("Community must never link into the Pro release archive",
                url.contains("/releases/pro/"));
    }

    /** The updater keeps its own manifest destination; the link must not disturb it. */
    @Test
    public void theUpdateManifestDestinationIsUnchanged() {
        assertEquals("https://sepa-xml-generator.com/releases/community/latest.json",
                AppLinks.UPDATE_MANIFEST_COMMUNITY);
    }

    @Test
    public void theDialogOffersViewOnlineAndNoSecondDestination() {
        JPanel content = ReleaseNotesDialog.buildContent(null, null, "1.4.0");
        String text = HelpDialogText.of(content);

        assertTrue(text.contains(ReleaseNotesDialog.VIEW_ONLINE));
        assertFalse("The generic Website action was replaced, not duplicated",
                text.contains("Website"));
        assertFalse("No releases-archive action belongs here", text.contains("All releases"));
    }

    @Test
    public void viewOnlineLooksAndBehavesLikeALink() {
        JPanel content = ReleaseNotesDialog.buildContent(null, null, "1.4.0");
        AbstractButton link = HelpDialogText.button(content, ReleaseNotesDialog.VIEW_ONLINE);

        assertNotNull(link);
        assertEquals("A link must show the hand cursor",
                Cursor.HAND_CURSOR, link.getCursor().getType());
        assertTrue("Keyboard focus must stay visible on a small dialog", link.isFocusPainted());
        assertTrue("A link must be activatable with the keyboard", link.isFocusable());
    }

    @Test
    public void ordinaryButtonsAreNotStyledAsLinks() {
        JPanel content = ReleaseNotesDialog.buildContent(null, null, "1.4.0");
        AbstractButton close = HelpDialogText.button(content, "Close");

        assertNotNull(close);
        assertFalse("Close is an ordinary button, not a hyperlink",
                close.getCursor().getType() == Cursor.HAND_CURSOR);
    }
}
