package com.pcariou.view.help;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import javax.swing.JPanel;

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
}
