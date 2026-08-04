package com.pcariou.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import org.junit.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * The Niryosys company icon is a publisher asset, distinct from the product
 * icon. Both theme variants must be on the classpath and must render as the
 * brand colours through the SVG infrastructure the UI already uses.
 */
public class CompanyIconTest {

    private static final Color NAVY = new Color(0x08, 0x2B, 0x4B);
    private static final Color CREAM = new Color(0xFD, 0xF6, 0xEC);

    @Test
    public void bothThemeVariantsAreOnTheClasspath() {
        assertNotNull("navy variant", getClass().getResource(AppResources.companyIcon(false)));
        assertNotNull("light variant", getClass().getResource(AppResources.companyIcon(true)));
    }

    @Test
    public void lightThemeUsesNavyAndDarkThemeUsesTheLightVariant() {
        assertTrue(AppResources.companyIcon(false).contains("navy"));
        assertTrue(AppResources.companyIcon(true).contains("light"));
    }

    @Test
    public void navyVariantRendersSquareInTheBrandColour() {
        assertEquals(NAVY, dominantColour(AppResources.companyIcon(false)));
    }

    @Test
    public void lightVariantRendersSquareInTheBrandColour() {
        assertEquals(CREAM, dominantColour(AppResources.companyIcon(true)));
    }

    /**
     * Renders the icon at the About-dialog size and returns the most frequent
     * fully opaque colour. Also proves the asset paints something at all (a
     * blank render would find no opaque pixel) and keeps its square aspect.
     */
    private static Color dominantColour(String resource) {
        int size = 32;
        FlatSVGIcon icon = new FlatSVGIcon(resource.substring(1), size, size);
        assertEquals("icons must stay square", icon.getIconWidth(), icon.getIconHeight());

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.dispose();

        java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int argb = image.getRGB(x, y);
                if (((argb >>> 24) & 0xFF) == 0xFF) {
                    counts.merge(argb & 0xFFFFFF, 1, Integer::sum);
                }
            }
        }
        assertTrue("the icon must actually paint", !counts.isEmpty());

        int best = 0;
        int bestCount = 0;
        for (java.util.Map.Entry<Integer, Integer> e : counts.entrySet()) {
            if (e.getValue() > bestCount) {
                bestCount = e.getValue();
                best = e.getKey();
            }
        }
        return new Color(best);
    }
}
