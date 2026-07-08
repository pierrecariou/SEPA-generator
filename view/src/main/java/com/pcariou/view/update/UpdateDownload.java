package com.pcariou.view.update;

/**
 * A single platform-specific download entry from the Community update manifest.
 *
 * <p>Plain data object: fields map 1:1 to the {@code downloads.*} JSON keys and
 * are populated by Gson. Consumers see it as read-only (getters only).</p>
 */
public final class UpdateDownload {

    private String label;
    private String url;

    /** Required by Gson. */
    public UpdateDownload() {
    }

    public UpdateDownload(String label, String url) {
        this.label = label;
        this.url = url;
    }

    /** Human-readable label (e.g. {@code "Windows x64 MSI"}); may be {@code null}. */
    public String getLabel() {
        return label;
    }

    /** Direct download URL for the platform asset; may be {@code null} or blank. */
    public String getUrl() {
        return url;
    }

    /** True when a usable, non-blank download URL is present. */
    public boolean hasUrl() {
        return url != null && !url.trim().isEmpty();
    }
}
