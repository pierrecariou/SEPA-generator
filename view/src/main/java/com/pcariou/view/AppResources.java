package com.pcariou.view;

/**
 * Centralised classpath resource paths and helpers for application icons.
 */
public final class AppResources {

    /** Window/taskbar icon sizes shipped on the classpath. */
    public static final int[] APP_ICON_SIZES = {16, 32, 48, 64, 256};

    private static final String APP_ICON_PREFIX = "/sepa-generator-icon-app-bg-";
    private static final String HEADER_ICON_LIGHT = "/sepa-generator-icon-light-theme.png";
    private static final String HEADER_ICON_DARK  = "/sepa-generator-icon-dark-theme.png";

    /**
     * Publisher (Niryosys) icon variants. Distinct from the product icon: the
     * product keeps its own identity, this only identifies who publishes it.
     */
    private static final String COMPANY_ICON_LIGHT_THEME = "/niryosys-icon-navy.svg";
    private static final String COMPANY_ICON_DARK_THEME  = "/niryosys-icon-light.svg";

    /** Classpath path of the app icon for the given size. */
    public static String appIcon(int size) {
        return APP_ICON_PREFIX + size + ".png";
    }

    /** Classpath path of the header logo for the current theme. */
    public static String headerIcon(boolean dark) {
        return dark ? HEADER_ICON_DARK : HEADER_ICON_LIGHT;
    }

    /**
     * Classpath path of the Niryosys publisher icon for the current theme: the
     * navy mark on light backgrounds, the cream mark on dark ones.
     */
    public static String companyIcon(boolean dark) {
        return dark ? COMPANY_ICON_DARK_THEME : COMPANY_ICON_LIGHT_THEME;
    }

    private AppResources() {
    }
}
