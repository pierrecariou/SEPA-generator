package com.pcariou.view.update;

import com.pcariou.view.AppDialogs;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Wires the update check into the UI: owns the {@link UpdateCheckService} and the
 * footer {@link UpdateIndicator}, and translates check results into non-intrusive
 * UI reactions.
 *
 * <ul>
 *   <li><b>Automatic</b> (once per day, at startup): silently shows the footer
 *       badge when an update exists; does nothing otherwise.</li>
 *   <li><b>Manual</b> ("Check for updates"): always reports a result - opens the
 *       dialog when an update exists, or shows "you are up to date" / "unable to
 *       check" messages.</li>
 * </ul>
 *
 * <p>Community Edition only; no Pro behaviour.</p>
 */
public final class UpdateUi {

    private final JFrame owner;
    private final String currentVersion;
    private final UpdateCheckService service;
    private final UpdateIndicator indicator;

    public UpdateUi(JFrame owner, String currentVersion) {
        this(owner, currentVersion, new UpdateCheckService());
    }

    public UpdateUi(JFrame owner, String currentVersion, UpdateCheckService service) {
        this.owner = owner;
        this.currentVersion = currentVersion;
        this.service = service;
        this.indicator = new UpdateIndicator(owner);
    }

    /** The footer badge component to add to the layout (hidden until an update is found). */
    public UpdateIndicator getIndicator() {
        return indicator;
    }

    /**
     * Starts the update flow. Shows any previously discovered update immediately
     * from the local cache (no network), then refreshes in the background at most
     * once per day. Marshalled onto the EDT; never blocks startup.
     */
    public void startAutomaticCheck() {
        SwingUtilities.invokeLater(() -> {
            UpdateCheckResult cached = service.cachedResult(currentVersion);
            if (cached != null && cached.isUpdateAvailable()) {
                indicator.showUpdate(cached.getInfo(), currentVersion);
            }
            service.checkAutomatic(currentVersion, this::onAutomaticResult);
        });
    }

    /** User-initiated check that ignores the once-per-day limit and always reports a result. */
    public void checkManually() {
        service.checkManual(currentVersion, this::onManualResult);
    }

    private void onAutomaticResult(UpdateCheckResult result) {
        switch (result.getStatus()) {
            case UPDATE_AVAILABLE:
                indicator.showUpdate(result.getInfo(), currentVersion);
                break;
            case UP_TO_DATE:
                // The app is current (e.g. it was updated since the cache was written):
                // clear any stale pill. Stay silent otherwise.
                indicator.clear();
                break;
            case FAILED:
            default:
                // Keep whatever is already shown; failures never disturb the UI.
                break;
        }
    }

    private void onManualResult(UpdateCheckResult result) {
        switch (result.getStatus()) {
            case UPDATE_AVAILABLE:
                indicator.showUpdate(result.getInfo(), currentVersion);
                indicator.openDialogIfAvailable();
                break;
            case UP_TO_DATE:
                indicator.clear();
                AppDialogs.show(owner, "You are up to date",
                        "You are running the latest version (v" + currentVersion + ").",
                        AppDialogs.Kind.INFO);
                break;
            case FAILED:
            default:
                AppDialogs.show(owner, "Update check failed",
                        "Unable to check for updates right now.\n"
                                + "Please check your internet connection and try again later.",
                        AppDialogs.Kind.WARNING);
                break;
        }
    }
}
