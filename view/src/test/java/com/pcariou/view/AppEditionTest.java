package com.pcariou.view;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the centralised edition policy for the Community Edition.
 */
public class AppEditionTest {

    @Test
    public void editionLabelIsCommunity() {
        assertEquals("Community Edition", AppEdition.LABEL);
    }

    @Test
    public void communityShowsUpgradeToPro() {
        assertTrue(AppEdition.showUpgradeToPro());
    }

    @Test
    public void upgradeUrlIsCentralisedProLink() {
        assertEquals(AppLinks.PRO, AppEdition.upgradeUrl());
        assertEquals("https://sepa-xml-generator.com/pro", AppEdition.upgradeUrl());
    }
}
