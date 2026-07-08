package com.pcariou.view.update;

import java.util.Locale;

/**
 * Maps the current operating system and architecture to a manifest download key
 * ({@code "windows-x64"}, {@code "macos-arm64"}, {@code "macos-x64"},
 * {@code "linux-x64"}).
 *
 * <p>Returns {@code null} for platforms the Community manifest does not publish an
 * asset for; callers then fall back to the manifest's download page.</p>
 */
public final class PlatformDetector {

    private PlatformDetector() {
    }

    /** Download key for the running platform, or {@code null} when unknown. */
    public static String currentKey() {
        return keyFor(System.getProperty("os.name"), System.getProperty("os.arch"));
    }

    /** Package-visible for testing: derives the key from raw {@code os.name} / {@code os.arch}. */
    static String keyFor(String osName, String osArch) {
        if (osName == null) {
            return null;
        }
        String os = osName.toLowerCase(Locale.ROOT);
        String arch = osArch == null ? "" : osArch.toLowerCase(Locale.ROOT);
        boolean arm = arch.contains("aarch64") || arch.contains("arm");

        if (os.contains("win")) {
            return "windows-x64";
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return arm ? "macos-arm64" : "macos-x64";
        }
        if (os.contains("nux") || os.contains("nix") || os.contains("aix")) {
            return "linux-x64";
        }
        return null;
    }
}
