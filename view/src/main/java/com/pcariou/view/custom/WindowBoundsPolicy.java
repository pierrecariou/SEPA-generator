package com.pcariou.view.custom;

import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * Pure policy for the initial bounds of the main application window.
 *
 * <p>Given the preferred size, the designed minimum size, the usable work area of
 * the target monitor (screen bounds minus taskbar/dock insets, in DPI-aware logical
 * coordinates) and a modest edge margin, it produces a centred rectangle that:
 *
 * <ul>
 *   <li>keeps the preferred size when it fits within the usable area (with margin);</li>
 *   <li>otherwise shrinks only as much as necessary, retaining the margin where the
 *       screen permits it;</li>
 *   <li>never exceeds the usable area, so the window always fits fully on screen;</li>
 *   <li>never places any edge off-screen and keeps coordinates non-negative;</li>
 *   <li>centres the window within the usable area (honouring left/top insets).</li>
 * </ul>
 *
 * <p>The designed minimum is respected on normal displays but is itself clamped to
 * the usable area, so a very small work area can never make the window larger than
 * the screen. This class contains no Swing state and is fully unit-testable.
 */
public final class WindowBoundsPolicy {

    private WindowBoundsPolicy() {
    }

    /**
     * Computes centred, fully on-screen initial window bounds.
     *
     * @param preferredW preferred width (e.g. the packed largest-layout width)
     * @param preferredH preferred height
     * @param minW       designed minimum width
     * @param minH       designed minimum height
     * @param usable     the target monitor's usable work area (bounds minus insets)
     * @param margin     desired gap to keep from the usable edges when the screen permits
     * @return a centred rectangle clamped to the usable area
     */
    public static Rectangle compute(int preferredW, int preferredH,
            int minW, int minH, Rectangle usable, int margin) {
        int m = Math.max(0, margin);
        int width = fit(preferredW, minW, usable.width, m);
        int height = fit(preferredH, minH, usable.height, m);
        int x = usable.x + Math.max(0, (usable.width - width) / 2);
        int y = usable.y + Math.max(0, (usable.height - height) / 2);
        return new Rectangle(x, y, width, height);
    }

    /**
     * The effective minimum size to apply to the frame: the designed minimum, but
     * never larger than the usable area so it cannot prevent the window fitting.
     */
    public static Dimension effectiveMinimum(int minW, int minH, Rectangle usable) {
        return new Dimension(Math.min(minW, usable.width), Math.min(minH, usable.height));
    }

    private static int fit(int preferred, int min, int usable, int margin) {
        int withMargin = usable - 2 * margin;
        int size;
        if (withMargin >= preferred) {
            size = preferred;          // fits comfortably: keep the preferred extent
        } else if (withMargin > 0) {
            size = withMargin;         // shrink just enough to keep the margin
        } else {
            size = usable;             // tiny work area: use the whole usable extent
        }
        int effectiveMin = Math.min(min, usable);
        size = Math.max(size, effectiveMin);
        return Math.min(size, usable);
    }
}
