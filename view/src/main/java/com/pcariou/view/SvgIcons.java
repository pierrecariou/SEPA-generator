package com.pcariou.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.UIManager;
import java.awt.Color;

/**
 * Builds themed {@link FlatSVGIcon}s from the SVG assets on the classpath.
 *
 * The icons are stroke-based ({@code stroke="currentColor"}); a color filter
 * maps them to the current Look-and-Feel foreground so they stay readable in
 * both light and dark themes. FlatLaf invalidates the icon cache on theme
 * changes, so the filter is re-evaluated and the icons re-tint automatically.
 */
public final class SvgIcons {

    /** Subtle, consistent toolbar icon size. */
    public static final int TOOLBAR_ICON_SIZE = 16;

    public static final String SUN      = "sun.svg";
    public static final String MOON     = "moon.svg";
    public static final String SETTINGS = "settings.svg";

    /** A {@value #TOOLBAR_ICON_SIZE}px icon tinted with the current foreground. */
    public static FlatSVGIcon toolbarIcon(String name) {
        return themed(name, TOOLBAR_ICON_SIZE);
    }

    /** An SVG icon at the given square size, tinted with the current foreground. */
    public static FlatSVGIcon themed(String name, int size) {
        FlatSVGIcon icon = new FlatSVGIcon(name, size, size);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(SvgIcons::foreground));
        return icon;
    }

    private static Color foreground(Color source) {
        Color fg = UIManager.getColor("Button.foreground");
        return fg != null ? fg : source;
    }

    private SvgIcons() {
    }
}
