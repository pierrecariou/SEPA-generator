package com.pcariou.view.update;

/**
 * Outcome of an update check: whether a newer version exists, the app is current,
 * or the check could not be completed.
 */
public final class UpdateCheckResult {

    /** The three possible outcomes of a check. */
    public enum Status {
        /** A newer version than the running one is available. */
        UPDATE_AVAILABLE,
        /** The running version is the latest published version. */
        UP_TO_DATE,
        /** The check could not be completed (offline, timeout, bad manifest). */
        FAILED
    }

    private final Status status;
    private final String currentVersion;
    private final UpdateInfo info;

    private UpdateCheckResult(Status status, String currentVersion, UpdateInfo info) {
        this.status = status;
        this.currentVersion = currentVersion;
        this.info = info;
    }

    public static UpdateCheckResult updateAvailable(String currentVersion, UpdateInfo info) {
        return new UpdateCheckResult(Status.UPDATE_AVAILABLE, currentVersion, info);
    }

    public static UpdateCheckResult upToDate(String currentVersion, UpdateInfo info) {
        return new UpdateCheckResult(Status.UP_TO_DATE, currentVersion, info);
    }

    public static UpdateCheckResult failed(String currentVersion) {
        return new UpdateCheckResult(Status.FAILED, currentVersion, null);
    }

    public Status getStatus() {
        return status;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    /** Parsed manifest, present for {@link Status#UPDATE_AVAILABLE} and {@link Status#UP_TO_DATE}; otherwise {@code null}. */
    public UpdateInfo getInfo() {
        return info;
    }

    public boolean isUpdateAvailable() {
        return status == Status.UPDATE_AVAILABLE;
    }
}
