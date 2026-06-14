package com.pcariou.view;

/**
 * Central definition of the running application edition and its Community-only
 * "funnel" behaviour (the <em>Upgrade to Pro</em> action).
 *
 * <p>Edition policy is kept here so that generic UI classes (header, footer)
 * stay focused on rendering and do not each decide edition labels or whether
 * commercial actions are shown.</p>
 *
 * <p>This is the public <strong>Community</strong> repository: Pro features are
 * never implemented here. This class only exposes the entry point that takes the
 * user to the Pro page, using the centralised {@link AppLinks} URLs.</p>
 */
public final class AppEdition {

    /** Human-readable edition name shown in the header and footer. */
    public static final String LABEL = "Community Edition";

    /** Visible text of the Community "Upgrade to Pro" action. */
    public static final String UPGRADE_TEXT = "Upgrade to Pro";

    /** Tooltip of the Community "Upgrade to Pro" action. */
    public static final String UPGRADE_TOOLTIP = "Discover SEPA Generator Pro";

    private AppEdition() {
    }

    /** Whether the Community "Upgrade to Pro" action should be shown. */
    public static boolean showUpgradeToPro() {
        return true;
    }

    /** Destination of the "Upgrade to Pro" action (centralised in {@link AppLinks}). */
    public static String upgradeUrl() {
        return AppLinks.PRO;
    }
}
