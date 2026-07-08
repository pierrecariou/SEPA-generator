package com.pcariou.view.update;

import java.time.LocalDate;
import java.util.prefs.Preferences;

/**
 * Small local persistence for the update checker, backed by
 * {@link java.util.prefs.Preferences}. It remembers the day of the last automatic
 * check (so the app checks at most once per day) and caches the last manifest it
 * successfully fetched (so a known update can be shown immediately at startup,
 * even on days when the network check is skipped).
 *
 * <p>No payment data is stored here, only a day counter and the small public
 * manifest document.</p>
 */
public final class UpdatePreferences {

    private static final String KEY_LAST_CHECK_EPOCH_DAY = "lastCheckEpochDay";
    private static final String KEY_CACHED_MANIFEST = "cachedManifest";

    private final Preferences prefs;

    public UpdatePreferences() {
        this(Preferences.userNodeForPackage(UpdatePreferences.class));
    }

    /** Package-visible for testing with an injected preferences node. */
    UpdatePreferences(Preferences prefs) {
        this.prefs = prefs;
    }

    /** True when an automatic check has not yet run today. */
    public boolean isCheckDueToday() {
        long last = prefs.getLong(KEY_LAST_CHECK_EPOCH_DAY, Long.MIN_VALUE);
        return isDue(last, LocalDate.now().toEpochDay());
    }

    /** Records that a check completed today, suppressing further automatic checks until tomorrow. */
    public void markCheckedToday() {
        prefs.putLong(KEY_LAST_CHECK_EPOCH_DAY, LocalDate.now().toEpochDay());
    }

    /** True when a manifest has been cached from a previous successful check. */
    public boolean hasCachedManifest() {
        String cached = prefs.get(KEY_CACHED_MANIFEST, null);
        return cached != null && !cached.isEmpty();
    }

    /** The last successfully fetched manifest JSON, or {@code null} if none is cached. */
    public String getCachedManifest() {
        return prefs.get(KEY_CACHED_MANIFEST, null);
    }

    /**
     * Caches the given manifest JSON. No-op when the document is blank or exceeds
     * the {@link java.util.prefs.Preferences} per-value length limit (the manifest
     * is small, so this only guards against unexpected input).
     */
    public void putCachedManifest(String manifestJson) {
        if (manifestJson == null || manifestJson.isEmpty()
                || manifestJson.length() > Preferences.MAX_VALUE_LENGTH) {
            return;
        }
        prefs.put(KEY_CACHED_MANIFEST, manifestJson);
    }

    /**
     * Pure once-per-day decision: a check is due when it has never run
     * ({@code lastCheckEpochDay} unset) or last ran on an earlier day.
     */
    static boolean isDue(long lastCheckEpochDay, long todayEpochDay) {
        return lastCheckEpochDay < todayEpochDay;
    }
}
