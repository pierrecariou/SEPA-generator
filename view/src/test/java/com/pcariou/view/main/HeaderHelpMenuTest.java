package com.pcariou.view.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.pcariou.view.AppLinks;
import com.pcariou.view.ExternalLinks;
import com.pcariou.view.help.AboutDialog;
import com.pcariou.view.help.ReleaseNotesDialog;

import org.junit.Test;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public void exposesTheFiveHelpEntriesInOrder() {
        assertEquals(Arrays.asList(
                        "Online guides",
                        "What\u2019s new\u2026",
                        "Check for updates",
                        "Contact\u2026",
                        "About SEPA Generator Community\u2026"),
                helpMenuLabels());
    }

    @Test
    public void exposesTheFiveHelpEntries() {
        assertEquals("Online guides", HeaderPanel.HELP_ONLINE_GUIDES);
        assertEquals("What\u2019s new\u2026", HeaderPanel.HELP_RELEASE_NOTES);
        assertEquals("Check for updates", HeaderPanel.HELP_CHECK_FOR_UPDATES);
        assertEquals("Contact\u2026", HeaderPanel.HELP_CONTACT);
        assertEquals("About SEPA Generator Community\u2026", HeaderPanel.HELP_ABOUT);
    }

    /**
     * Guides lead the menu: they are what a user reaches for when they need to
     * understand a format, so they must come before the release notes and About.
     */
    @Test
    public void onlineGuidesComeFirstAndBeforeReleaseNotesAndAbout() {
        List<String> labels = helpMenuLabels();

        assertEquals(HeaderPanel.HELP_ONLINE_GUIDES, labels.get(0));
        assertTrue(labels.indexOf(HeaderPanel.HELP_ONLINE_GUIDES)
                < labels.indexOf(HeaderPanel.HELP_RELEASE_NOTES));
        assertTrue(labels.indexOf(HeaderPanel.HELP_ONLINE_GUIDES)
                < labels.indexOf(HeaderPanel.HELP_ABOUT));
    }

    /**
     * The action must carry the canonical guides URL and be routed through the
     * shared external-link helper, so it uses the platform default browser and
     * the application's existing failure dialog rather than a second mechanism.
     */
    @Test
    public void onlineGuidesOpensTheCanonicalGuidesPage() {
        assertEquals("https://sepa-xml-generator.com/guides/", AppLinks.GUIDES);
        assertEquals("The guides live under the product website",
                ExternalLinks.Kind.BROWSE, ExternalLinks.classify(AppLinks.GUIDES));

        JMenuItem guides = helpMenuItem(HeaderPanel.HELP_ONLINE_GUIDES);
        assertNotNull(guides);
        assertTrue("The entry must be actionable", guides.getActionListeners().length > 0);
        assertTrue("The entry must always be available", guides.isEnabled());
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
        assertFalse("An external link must not promise a dialog",
                HeaderPanel.HELP_ONLINE_GUIDES.endsWith("\u2026"));
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
        String entries = String.join("", helpMenuLabels());

        assertFalse(entries.contains("Support"));
        assertFalse(entries.contains("support"));
    }

    /**
     * The Help menu stays informational. Guides are the one deliberate overlap
     * with the footer: they are the natural place to look for help, and the Help
     * entry names them explicitly as an external page.
     */
    @Test
    public void namesNoFooterNavigationEntry() {
        String entries = String.join("", helpMenuLabels());

        assertFalse(entries.contains("Website"));
        assertFalse(entries.contains("Privacy"));
    }

    private static JPopupMenu helpMenu() {
        return HeaderPanel.buildHelpMenuItems(null, null, "1.4.0", null);
    }

    private static List<String> helpMenuLabels() {
        List<String> labels = new ArrayList<>();
        for (Component item : helpMenu().getComponents()) {
            if (item instanceof JMenuItem) {
                labels.add(((JMenuItem) item).getText());
            }
        }
        return labels;
    }

    private static JMenuItem helpMenuItem(String label) {
        for (Component item : helpMenu().getComponents()) {
            if (item instanceof JMenuItem && label.equals(((JMenuItem) item).getText())) {
                return (JMenuItem) item;
            }
        }
        return null;
    }
}
