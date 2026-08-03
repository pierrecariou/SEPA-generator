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

    /**
     * Full product name of this edition, used where the product identifies
     * itself as a whole (the About dialog and the Help entry naming it).
     * Niryosys publishes the product; it is not part of the product name.
     */
    public static final String PRODUCT_NAME = "SEPA Generator Community";

    /** Visible text of the Community "Upgrade to Pro" action. */
    public static final String UPGRADE_TEXT = "Upgrade to Pro";

    /** Tooltip of the Community "Upgrade to Pro" action. */
    public static final String UPGRADE_TOOLTIP = "Discover SEPA Generator Pro";

    /** Root of this edition's public release archive on the product website. */
    private static final String RELEASES_BASE = "https://sepa-xml-generator.com/releases/community/";

    private AppEdition() {
    }

    /**
     * Public website page of one release of this edition, for example
     * {@code https://sepa-xml-generator.com/releases/community/1.4.0/}.
     *
     * <p>Derived from the installed version rather than hard-coded, so a release
     * never has to remember to update a link. {@code release} is expected to be
     * an already-normalised dotted release number; callers that hold a raw
     * application version should normalise it first (see
     * {@code ReleaseNotes.onlineUrl}).</p>
     */
    public static String releasePageUrl(String release) {
        return RELEASES_BASE + release + "/";
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
