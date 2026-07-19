package com.pcariou.view.custom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Dimension;
import java.awt.Rectangle;

import org.junit.Test;

/**
 * Initial main-window sizing policy ({@link WindowBoundsPolicy}).
 *
 * <p>Representative usable work areas (screen bounds minus taskbar/dock insets) are
 * checked against the designed preferred size (1000x760) and minimum (860x520) with
 * the standard 24px edge margin. The policy must keep the preferred size when it
 * fits, shrink only as needed otherwise, never exceed the usable area, and stay
 * centred and non-negative.
 */
public class WindowBoundsPolicyTest {

    private static final int PREF_W = 1000;
    private static final int PREF_H = 760;
    private static final int MIN_W = 860;
    private static final int MIN_H = 520;
    private static final int MARGIN = 24;

    private Rectangle compute(Rectangle usable) {
        return WindowBoundsPolicy.compute(PREF_W, PREF_H, MIN_W, MIN_H, usable, MARGIN);
    }

    @Test
    public void largeDesktopKeepsThePreferredSize() {
        Rectangle usable = new Rectangle(0, 0, 1920, 1040);
        Rectangle b = compute(usable);

        assertEquals(PREF_W, b.width);
        assertEquals(PREF_H, b.height);
        assertEquals((1920 - 1000) / 2, b.x);
        assertEquals((1040 - 760) / 2, b.y);
    }

    @Test
    public void laptop1366CapsHeightButKeepsWidth() {
        Rectangle b = compute(new Rectangle(0, 0, 1366, 728));

        assertEquals("width still fits", PREF_W, b.width);
        assertEquals("height reduced to keep the margin", 728 - 2 * MARGIN, b.height);
        assertFitsInside(b, new Rectangle(0, 0, 1366, 728));
    }

    @Test
    public void area1280x680CapsHeight() {
        Rectangle b = compute(new Rectangle(0, 0, 1280, 680));

        assertEquals(PREF_W, b.width);
        assertEquals(680 - 2 * MARGIN, b.height);
        assertFitsInside(b, new Rectangle(0, 0, 1280, 680));
    }

    @Test
    public void narrowArea1024ReducesWidthAndHeight() {
        Rectangle b = compute(new Rectangle(0, 0, 1024, 728));

        assertEquals(1024 - 2 * MARGIN, b.width);
        assertEquals(728 - 2 * MARGIN, b.height);
        assertFitsInside(b, new Rectangle(0, 0, 1024, 728));
    }

    @Test
    public void areaCloseToMinimumFallsBackToTheDesignedMinimum() {
        Rectangle usable = new Rectangle(0, 0, 880, 540);
        Rectangle b = compute(usable);

        assertEquals(MIN_W, b.width);
        assertEquals(MIN_H, b.height);
        assertFitsInside(b, usable);
    }

    @Test
    public void areaSmallerThanMinimumUsesTheWholeUsableArea() {
        Rectangle usable = new Rectangle(0, 0, 800, 480);
        Rectangle b = compute(usable);

        assertEquals("never wider than the screen", 800, b.width);
        assertEquals("never taller than the screen", 480, b.height);
        assertFitsInside(b, usable);
    }

    @Test
    public void honoursUsableOriginFromLeftAndTopInsets() {
        // e.g. a left dock (x offset) plus a top menu bar (y offset).
        Rectangle usable = new Rectangle(60, 30, 1860, 1010);
        Rectangle b = compute(usable);

        assertTrue("x not left of the usable origin", b.x >= usable.x);
        assertTrue("y not above the usable origin", b.y >= usable.y);
        assertFitsInside(b, usable);
    }

    @Test
    public void boundsAreAlwaysCentredAndNonNegative() {
        Rectangle[] areas = {
                new Rectangle(0, 0, 1920, 1040),
                new Rectangle(0, 0, 1366, 728),
                new Rectangle(0, 0, 1280, 680),
                new Rectangle(0, 0, 1024, 728),
                new Rectangle(0, 0, 880, 540),
                new Rectangle(0, 0, 800, 480),
        };
        for (Rectangle usable : areas) {
            Rectangle b = compute(usable);
            assertTrue("non-negative x for " + usable, b.x >= 0);
            assertTrue("non-negative y for " + usable, b.y >= 0);
            assertFitsInside(b, usable);
            int leftGap = b.x - usable.x;
            int rightGap = (usable.x + usable.width) - (b.x + b.width);
            int topGap = b.y - usable.y;
            int bottomGap = (usable.y + usable.height) - (b.y + b.height);
            assertTrue("horizontally centred for " + usable, Math.abs(leftGap - rightGap) <= 1);
            assertTrue("vertically centred for " + usable, Math.abs(topGap - bottomGap) <= 1);
        }
    }

    @Test
    public void marginNeverPushesTheWindowOffScreen() {
        Rectangle usable = new Rectangle(0, 0, 1200, 700);
        Rectangle b = compute(usable);
        assertFitsInside(b, usable);
    }

    @Test
    public void effectiveMinimumIsClampedToTheUsableArea() {
        Dimension normal = WindowBoundsPolicy.effectiveMinimum(MIN_W, MIN_H,
                new Rectangle(0, 0, 1920, 1040));
        assertEquals(MIN_W, normal.width);
        assertEquals(MIN_H, normal.height);

        Dimension tiny = WindowBoundsPolicy.effectiveMinimum(MIN_W, MIN_H,
                new Rectangle(0, 0, 800, 480));
        assertEquals("min width never exceeds the screen", 800, tiny.width);
        assertEquals("min height never exceeds the screen", 480, tiny.height);
    }

    private static void assertFitsInside(Rectangle b, Rectangle usable) {
        assertTrue("width within usable: " + b + " in " + usable, b.width <= usable.width);
        assertTrue("height within usable: " + b + " in " + usable, b.height <= usable.height);
        assertTrue("right edge on screen", b.x + b.width <= usable.x + usable.width);
        assertTrue("bottom edge on screen", b.y + b.height <= usable.y + usable.height);
    }
}
