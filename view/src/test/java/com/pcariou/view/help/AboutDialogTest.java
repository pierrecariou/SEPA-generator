package com.pcariou.view.help;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.pcariou.view.AppLinks;

import org.junit.Test;

import javax.swing.AbstractButton;
import javax.swing.JPanel;
import java.awt.Cursor;

/**
 * Tests for {@link AboutDialog}: it states the product, the installed version
 * and the publisher, and nothing it cannot know reliably.
 */
public class AboutDialogTest {

    @Test
    public void showsProductVersionAndPublisher() {
        JPanel content = AboutDialog.buildContent(null, null, "1.4.0");
        String text = HelpDialogText.of(content);

        assertTrue(text.contains("SEPA Generator Community"));
        assertTrue(text.contains("Version 1.4.0"));
        assertTrue(text.contains("Published by Niryosys"));
        assertTrue(text.contains("Website"));
        assertTrue(text.contains("Close"));
    }

    @Test
    public void titleNamesTheProductWithoutThePublisher() {
        assertTrue(AboutDialog.TITLE.contains("SEPA Generator Community"));
        assertFalse("Niryosys publishes the product; it is not part of the product name",
                AboutDialog.TITLE.contains("Niryosys"));
    }

    @Test
    public void toleratesAnUnknownVersion() {
        String text = HelpDialogText.of(AboutDialog.buildContent(null, null, null));

        assertTrue(text.contains("Version unknown"));
        assertTrue(text.contains("Published by Niryosys"));
    }

    @Test
    public void staysRestrained() {
        String text = HelpDialogText.of(AboutDialog.buildContent(null, null, "1.4.0"));

        assertFalse("About must not claim a licence state it does not read here",
                text.toLowerCase().contains("licence"));
        assertFalse(text.toLowerCase().contains("copyright \u00a9"));
    }

    @Test
    public void namesNoProOnlyCapability() {
        String text = HelpDialogText.of(AboutDialog.buildContent(null, null, "1.4.0")).toLowerCase();

        assertFalse("Community About must not advertise Pro", text.contains("pro edition"));
        assertFalse(text.contains("pain.008"));
    }

    /**
     * About points at the product homepage on purpose. It must not drift to the
     * releases archive (that is the release-notes dialog's destination) nor to a
     * publisher site.
     */
    @Test
    public void websiteStillPointsAtTheProductHomepage() {
        assertEquals("https://sepa-xml-generator.com", AppLinks.WEBSITE);

        String text = HelpDialogText.of(AboutDialog.buildContent(null, null, "1.4.0"));
        assertTrue("The About action stays labelled Website", text.contains("Website"));
        assertFalse("About must not adopt the release-notes destination",
                text.contains(ReleaseNotesDialog.VIEW_ONLINE));
    }

    @Test
    public void websiteLooksAndBehavesLikeALink() {
        JPanel content = AboutDialog.buildContent(null, null, "1.4.0");
        AbstractButton website = HelpDialogText.button(content, "Website");

        assertNotNull(website);
        assertEquals("A link must show the hand cursor",
                Cursor.HAND_CURSOR, website.getCursor().getType());
        assertTrue("Keyboard focus must stay visible", website.isFocusPainted());
        assertTrue(website.isFocusable());
    }

    @Test
    public void ordinaryButtonsAreNotStyledAsLinks() {
        JPanel content = AboutDialog.buildContent(null, null, "1.4.0");
        AbstractButton close = HelpDialogText.button(content, "Close");

        assertNotNull(close);
        assertFalse("Close is an ordinary button, not a hyperlink",
                close.getCursor().getType() == Cursor.HAND_CURSOR);
    }
}
