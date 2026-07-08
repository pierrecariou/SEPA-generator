package com.pcariou.view.update;

import com.google.gson.Gson;
import com.pcariou.view.AppLinks;

import javax.swing.SwingWorker;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinates the Community update check: fetches the manifest, compares versions
 * and reports an {@link UpdateCheckResult}.
 *
 * <p>The synchronous {@link #check(String)} contains the whole decision and is
 * easy to unit test. The asynchronous {@link #checkAutomatic} /
 * {@link #checkManual} run it off the Event Dispatch Thread with a
 * {@link SwingWorker} and deliver the result back on the EDT, so a slow or failed
 * network never blocks the UI or application startup.</p>
 *
 * <p>Automatic checks honour a once-per-day limit; manual checks ignore it.
 * Failures are reported as {@link UpdateCheckResult.Status#FAILED} and never throw.</p>
 */
public class UpdateCheckService {

    private static final Logger LOGGER = Logger.getLogger(UpdateCheckService.class.getName());

    private final UpdateManifestClient client;
    private final UpdatePreferences preferences;
    private final String manifestUrl;
    private final Gson gson = new Gson();

    public UpdateCheckService() {
        this(new UpdateManifestClient(), new UpdatePreferences(), AppLinks.UPDATE_MANIFEST_COMMUNITY);
    }

    public UpdateCheckService(UpdateManifestClient client, UpdatePreferences preferences, String manifestUrl) {
        this.client = client;
        this.preferences = preferences;
        this.manifestUrl = manifestUrl;
    }

    /**
     * Synchronously fetches the manifest and compares it against
     * {@code currentVersion}. Never throws: any failure yields
     * {@link UpdateCheckResult.Status#FAILED}. Intended to run off the EDT.
     */
    public UpdateCheckResult check(String currentVersion) {
        try {
            UpdateInfo info = client.fetch(manifestUrl);
            if (VersionComparator.isNewer(info.getLatestVersion(), currentVersion)) {
                return UpdateCheckResult.updateAvailable(currentVersion, info);
            }
            return UpdateCheckResult.upToDate(currentVersion, info);
        } catch (Exception failure) {
            LOGGER.log(Level.FINE, "Update check failed", failure);
            return UpdateCheckResult.failed(currentVersion);
        }
    }

    /**
     * Evaluates the last cached manifest against {@code currentVersion} without any
     * network access. Returns {@code null} when nothing usable is cached; otherwise
     * an {@link UpdateCheckResult} reflecting whether the cached version is newer.
     * Used at startup to surface a previously discovered update immediately.
     */
    public UpdateCheckResult cachedResult(String currentVersion) {
        String json = preferences.getCachedManifest();
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            UpdateInfo info = client.parse(json);
            if (VersionComparator.isNewer(info.getLatestVersion(), currentVersion)) {
                return UpdateCheckResult.updateAvailable(currentVersion, info);
            }
            return UpdateCheckResult.upToDate(currentVersion, info);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Ignoring unreadable cached manifest", e);
            return null;
        }
    }

    /**
     * Runs an automatic background check. The network request is throttled to once
     * per day, but it always runs when nothing has been cached yet, so a first
     * launch (or a reset preferences state) still discovers an update the same day.
     * Results are delivered on the EDT.
     */
    public void checkAutomatic(String currentVersion, Consumer<UpdateCheckResult> onResult) {
        if (!preferences.isCheckDueToday() && preferences.hasCachedManifest()) {
            return;
        }
        runInBackground(currentVersion, onResult);
    }

    /**
     * Runs a background check regardless of the once-per-day limit (user-initiated).
     * Results are delivered on the EDT.
     */
    public void checkManual(String currentVersion, Consumer<UpdateCheckResult> onResult) {
        runInBackground(currentVersion, onResult);
    }

    private void runInBackground(final String currentVersion, final Consumer<UpdateCheckResult> onResult) {
        new SwingWorker<UpdateCheckResult, Void>() {
            @Override
            protected UpdateCheckResult doInBackground() {
                return check(currentVersion);
            }

            @Override
            protected void done() {
                UpdateCheckResult result;
                try {
                    result = get();
                } catch (Exception e) {
                    result = UpdateCheckResult.failed(currentVersion);
                }
                // A completed check (even "up to date") satisfies today's automatic
                // check and refreshes the cached manifest for the next startup.
                if (result.getStatus() != UpdateCheckResult.Status.FAILED) {
                    preferences.markCheckedToday();
                    if (result.getInfo() != null) {
                        preferences.putCachedManifest(gson.toJson(result.getInfo()));
                    }
                }
                if (onResult != null) {
                    onResult.accept(result);
                }
            }
        }.execute();
    }
}
