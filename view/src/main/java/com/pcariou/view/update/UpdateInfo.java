package com.pcariou.view.update;

import java.util.Map;

/**
 * In-memory representation of the Community update manifest
 * ({@code /releases/community/latest.json}).
 *
 * <p>Field names map 1:1 to the manifest JSON keys and are populated by Gson.
 * The manifest is intentionally small and machine-readable: it advertises the
 * latest version and where to download it, not a changelog. Consumers treat this
 * object as read-only.</p>
 */
public final class UpdateInfo {

    /** The only edition this Community build will act on. */
    public static final String EDITION_COMMUNITY = "community";

    private int schemaVersion;
    private String edition;
    private String latestVersion;
    private String releaseDate;
    private boolean critical;
    private String downloadPageUrl;
    private String releaseNotesUrl;
    private Map<String, UpdateDownload> downloads;

    /** Manifest schema version, used to stay forward-compatible. */
    public int getSchemaVersion() {
        return schemaVersion;
    }

    /** Edition the manifest describes (expected {@code "community"}). */
    public String getEdition() {
        return edition;
    }

    /** Latest published version (e.g. {@code "1.3.1"}). */
    public String getLatestVersion() {
        return latestVersion;
    }

    /** Release date (ISO {@code yyyy-MM-dd}); may be {@code null}. */
    public String getReleaseDate() {
        return releaseDate;
    }

    /** Whether this release is flagged as critical. */
    public boolean isCritical() {
        return critical;
    }

    /** Page users are sent to when no platform-specific asset is available. */
    public String getDownloadPageUrl() {
        return downloadPageUrl;
    }

    /** Where to read more about the release (may point to the download page). */
    public String getReleaseNotesUrl() {
        return releaseNotesUrl;
    }

    /** Platform-specific downloads keyed by platform (e.g. {@code "windows-x64"}). */
    public Map<String, UpdateDownload> getDownloads() {
        return downloads;
    }

    /** True when a release date is present. */
    public boolean hasReleaseDate() {
        return releaseDate != null && !releaseDate.trim().isEmpty();
    }

    /**
     * True only when this manifest explicitly declares the Community edition.
     * A Pro manifest, a missing edition, or an unknown edition all return
     * {@code false} so a foreign manifest can never drive a Community update.
     */
    public boolean isCommunityEdition() {
        return edition != null && EDITION_COMMUNITY.equalsIgnoreCase(edition.trim());
    }

    /** True when {@link #latestVersion} is a real dotted numeric release. */
    public boolean hasUsableVersion() {
        return VersionComparator.hasNumericVersion(latestVersion);
    }

    /**
     * True when there is at least one absolute {@code http(s)} URL to send the
     * user to: a platform-specific download or, failing that, the download page.
     * A manifest with no usable destination is treated as unusable.
     */
    public boolean hasUsableDownloadUrl() {
        if (isUsableUrl(downloadPageUrl)) {
            return true;
        }
        if (downloads != null) {
            for (UpdateDownload download : downloads.values()) {
                if (download != null && isUsableUrl(download.getUrl())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * A manifest is usable only when it is a Community manifest, advertises a
     * real version to compare against, and offers a usable download URL. This is
     * the edition-safe contract: any other manifest is rejected rather than
     * offered as an update.
     */
    public boolean isValid() {
        return isCommunityEdition() && hasUsableVersion() && hasUsableDownloadUrl();
    }

    private static boolean isUsableUrl(String url) {
        if (url == null) {
            return false;
        }
        String u = url.trim();
        return u.startsWith("https://") || u.startsWith("http://");
    }

    /**
     * Best download URL for {@code platformKey}: the platform-specific asset when
     * available, otherwise the manifest's download page. Never returns {@code null}
     * as long as a download page is present.
     *
     * @param platformKey a key such as {@code "windows-x64"}, or {@code null} when
     *                    the platform could not be detected
     */
    public String downloadUrlFor(String platformKey) {
        if (platformKey != null && downloads != null) {
            UpdateDownload download = downloads.get(platformKey);
            if (download != null && download.hasUrl()) {
                return download.getUrl();
            }
        }
        return downloadPageUrl;
    }
}
