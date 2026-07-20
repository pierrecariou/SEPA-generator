package com.pcariou.view.main;

import com.pcariou.view.SvgIcons;

/**
 * Application status shown in the footer.
 *
 * Priority: lower ordinal = higher importance. When multiple conditions apply
 * simultaneously, the status with the lowest ordinal wins.
 *
 * <p>Terminal semantic outcomes (generated / failed) carry a small semantic
 * icon and use neutral text, so the semantic colour annotates the state via the
 * icon rather than colouring the whole sentence. Neutral, instructional and
 * in-progress states are icon-free and use plain neutral text (primary for
 * actionable prompts and progress, muted for idle guidance) — no status dot and
 * no accent/semantic sentences.
 */
public enum AppStatus {

    // ── Transient / action states ─────────────────────────────────────────
    GENERATION_FAILED ("Generation failed",              "$Label.foreground",          false, SvgIcons.CIRCLE_X,     "App.errorColor"),
    GENERATING        ("Generating SEPA XML...",          "$Label.foreground",          true,  null,                  null),
    GENERATED         ("File generated successfully",     "$Label.foreground",          false, SvgIcons.CIRCLE_CHECK, "App.successColor"),

    // ── Blocking / input-required states ─────────────────────────────────
    DEBTOR_INFO_REQUIRED ("Debtor information required", "$Label.foreground",          false, null, null),
    SELECT_FILE          ("Select a CSV or Excel input file","$Label.disabledForeground", false, null, null),
    SELECT_DATE          ("Select an execution date",     "$Label.disabledForeground", false, null, null),

    // ── Default ───────────────────────────────────────────────────────────
    READY             ("Ready",                           "$Label.disabledForeground",  false, null, null);

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

    /** SVG resource name for the semantic icon, or {@code null} for icon-free states. */
    public final String iconName;

    /** UIManager color key used to tint {@link #iconName}, or {@code null}. */
    public final String iconColorKey;

    AppStatus(String label, String styleColor, boolean showProgress, String iconName, String iconColorKey) {
        this.label        = label;
        this.styleColor   = styleColor;
        this.showProgress = showProgress;
        this.iconName     = iconName;
        this.iconColorKey = iconColorKey;
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
