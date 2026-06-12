package com.pcariou.view.main;

/**
 * Application status shown in the footer.
 *
 * Priority: lower ordinal = higher importance. When multiple conditions apply
 * simultaneously, the status with the lowest ordinal wins.
 */
public enum AppStatus {

    // ── Transient / action states ─────────────────────────────────────────
    GENERATION_FAILED ("Generation failed",              "$App.errorColor",            false),
    GENERATING        ("Generating SEPA XML...",          "$Component.accentColor",     true),
    GENERATED         ("File generated successfully",     "$App.successColor",          false),

    // ── Blocking / input-required states ─────────────────────────────────
    DEBTOR_INFO_REQUIRED ("Debtor information required", "$Component.accentColor",     false),
    SELECT_FILE          ("Select a credit transfer file","$Label.disabledForeground", false),
    SELECT_DATE          ("Select an execution date",     "$Label.disabledForeground", false),

    // ── Default ───────────────────────────────────────────────────────────
    READY             ("Ready",                           "$Label.disabledForeground",  false);

    /** Human-readable message shown in the footer label. */
    public final String label;

    /**
     * FlatLaf STYLE-compatible color string.
     * Strings starting with '$' are UIManager key references (e.g. {@code $Component.accentColor});
     * other strings are treated as literal hex colours (e.g. {@code #D32F2F}).
     */
    public final String styleColor;

    /** Whether the indeterminate progress bar should be visible. */
    public final boolean showProgress;

    AppStatus(String label, String styleColor, boolean showProgress) {
        this.label        = label;
        this.styleColor   = styleColor;
        this.showProgress = showProgress;
    }

    /**
     * Returns the highest-priority status (lowest ordinal) from the given candidates.
     * Candidates that are {@code null} are ignored.
     */
    public static AppStatus highest(AppStatus... candidates) {
        AppStatus best = READY;
        for (AppStatus s : candidates) {
            if (s != null && s.ordinal() < best.ordinal()) best = s;
        }
        return best;
    }
}
