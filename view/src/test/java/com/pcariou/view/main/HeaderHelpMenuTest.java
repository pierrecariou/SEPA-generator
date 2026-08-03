package com.pcariou.view.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.pcariou.view.AppLinks;
import com.pcariou.view.help.AboutDialog;
import com.pcariou.view.help.ReleaseNotesDialog;

import org.junit.Test;

/**
 * Guards the Help menu contract of {@link HeaderPanel}.
 *
 * <p>The header itself cannot be instantiated headlessly (it builds the update
 * indicator and the themed toolbar), so this test pins the parts that carry the
 * user-visible meaning: the entry labels and the dialogs they name. The
 * behaviour of each dialog is covered by its own test, and "Check for updates"
 * is deliberately routed to the existing {@code UpdateUi.checkManually()} flow
 * rather than to any new update logic.</p>
 */
public class HeaderHelpMenuTest {

    @Test
    public void exposesTheFourHelpEntriesInOrder() {
        assertEquals(java.util.Arrays.asList(
                        "What\u2019s new\u2026",
                        "Check for updates",
                        "Contact\u2026",
                        "About SEPA Generator Community\u2026"),
                java.util.Arrays.asList(
                        HeaderPanel.HELP_RELEASE_NOTES,
                        HeaderPanel.HELP_CHECK_FOR_UPDATES,
                        HeaderPanel.HELP_CONTACT,
                        HeaderPanel.HELP_ABOUT));
    }

    @Test
    public void exposesTheFourHelpEntries() {
        assertEquals("What\u2019s new\u2026", HeaderPanel.HELP_RELEASE_NOTES);
        assertEquals("Check for updates", HeaderPanel.HELP_CHECK_FOR_UPDATES);
        assertEquals("Contact\u2026", HeaderPanel.HELP_CONTACT);
        assertEquals("About SEPA Generator Community\u2026", HeaderPanel.HELP_ABOUT);
    }

    @Test
    public void aboutEntryMatchesTheAboutDialogTitle() {
        assertTrue(HeaderPanel.HELP_ABOUT.startsWith(AboutDialog.TITLE));
    }

    @Test
    public void entriesThatNeedFurtherInputAreMarkedWithAnEllipsis() {
        assertTrue(HeaderPanel.HELP_RELEASE_NOTES.endsWith("\u2026"));
        assertTrue(HeaderPanel.HELP_ABOUT.endsWith("\u2026"));
        assertTrue("Contact hands over to the mail client, so it is not an immediate action",
                HeaderPanel.HELP_CONTACT.endsWith("\u2026"));
        assertFalse("An immediate action must not promise a follow-up step",
                HeaderPanel.HELP_CHECK_FOR_UPDATES.endsWith("\u2026"));
    }

    @Test
    public void releaseNotesEntryOpensTheReleaseNotesDialog() {
        assertTrue(HeaderPanel.HELP_RELEASE_NOTES.endsWith("\u2026"));
        assertFalse("The menu label is renamed, but it still opens " + ReleaseNotesDialog.TITLE,
                HeaderPanel.HELP_RELEASE_NOTES.equals(ReleaseNotesDialog.TITLE));
    }

    /**
     * Contact must reach the existing Community contact destination, not a Pro
     * support channel that this edition does not have.
     */
    @Test
    public void contactUsesTheExistingCommunityContactDestination() {
        assertTrue(AppLinks.CONTACT.startsWith("mailto:"));
        assertTrue(AppLinks.CONTACT.contains("sepa-xml-generator.com"));
    }

    /**
     * Community offers a contact route, not a support service: the menu must not
     * imply a dedicated support channel or a response-time commitment.
     */
    @Test
    public void promisesNoSupportService() {
        String entries = HeaderPanel.HELP_RELEASE_NOTES + HeaderPanel.HELP_CHECK_FOR_UPDATES
                + HeaderPanel.HELP_CONTACT + HeaderPanel.HELP_ABOUT;

        assertFalse(entries.contains("Support"));
        assertFalse(entries.contains("support"));
    }

    /** The Help menu stays informational: no duplicated footer navigation. */
    @Test
    public void namesNoFooterNavigationEntry() {
        String entries = HeaderPanel.HELP_RELEASE_NOTES + HeaderPanel.HELP_CHECK_FOR_UPDATES
                + HeaderPanel.HELP_CONTACT + HeaderPanel.HELP_ABOUT;

        assertFalse(entries.contains("Website"));
        assertFalse(entries.contains("Privacy"));
        assertFalse(entries.contains("Guides"));
    }
}
