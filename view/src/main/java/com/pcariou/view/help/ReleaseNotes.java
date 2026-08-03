package com.pcariou.view.help;

import com.pcariou.view.AppEdition;
import com.pcariou.view.AppLinks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Loads the curated release notes bundled with the application.
 *
 * <p>The notes are the single in-repository source of truth for "what changed"
 * in a given release: one small HTML fragment per version, shipped on the
 * classpath as {@code /help/release-notes-<version>.html}. Because they are
 * bundled, {@link #forVersion(String)} never touches the network and works
 * offline, which is why the Help entry can always be offered.</p>
 *
 * <p>Lookup is deliberately strict: notes are selected from the running
 * application version only, never from a file name, a manifest or a guess. Only
 * the usual build decorations are normalised away, so a development build such
 * as {@code 1.4.0-SNAPSHOT} or {@code 1.4.0+build.7} still shows the notes of
 * the release it is based on. When no notes are bundled for a version, the
 * caller gets an empty result and is expected to say so calmly rather than to
 * fail.</p>
 */
public final class ReleaseNotes {

    private static final String RESOURCE_PREFIX = "/help/release-notes-";
    private static final String RESOURCE_SUFFIX = ".html";

    /** A dotted numeric release such as {@code 1.4.0}; anything else is not a lookup key. */
    private static final String RELEASE_PATTERN = "\\d+(\\.\\d+)*";

    private ReleaseNotes() {
    }

    /**
     * Normalises {@code version} to the release it belongs to: build metadata
     * ({@code +...}) and pre-release/development suffixes ({@code -SNAPSHOT},
     * {@code -rc1}, ...) are dropped, so development builds resolve to the notes
     * of their base release.
     *
     * @return the dotted numeric release, or {@code null} when {@code version}
     *         is missing, unresolved or not a release number
     */
    public static String normalizeVersion(String version) {
        if (version == null) {
            return null;
        }
        String value = version.trim();
        int build = value.indexOf('+');
        if (build >= 0) {
            value = value.substring(0, build);
        }
        int preRelease = value.indexOf('-');
        if (preRelease >= 0) {
            value = value.substring(0, preRelease);
        }
        value = value.trim();
        return value.matches(RELEASE_PATTERN) ? value : null;
    }

    /** The classpath resource holding the notes of {@code release}, without checking that it exists. */
    static String resourcePath(String release) {
        return RESOURCE_PREFIX + release + RESOURCE_SUFFIX;
    }

    /** Whether curated notes are bundled for {@code version}. */
    public static boolean areAvailableFor(String version) {
        return !forVersion(version).isEmpty();
    }

    /**
     * Returns the bundled notes for {@code version} as an HTML fragment, or an
     * empty string when none are bundled or the resource cannot be read. Never
     * throws: missing notes are a normal, non-fatal situation.
     */
    public static String forVersion(String version) {
        String release = normalizeVersion(version);
        if (release == null) {
            return "";
        }
        try (InputStream in = ReleaseNotes.class.getResourceAsStream(resourcePath(release))) {
            if (in == null) {
                return "";
            }
            return read(in).trim();
        } catch (IOException failure) {
            return "";
        }
    }

    /**
     * Public website page presenting the notes of {@code version} online, for
     * example {@code https://sepa-xml-generator.com/releases/community/1.4.0/}.
     *
     * <p>The page is derived from the installed version through the same
     * normalisation used to select the bundled notes, so a development build
     * such as {@code 1.4.0-SNAPSHOT} links to the page of the release it is
     * based on rather than to a page that cannot exist. When the version cannot
     * be resolved at all the product website is used, which always exists - the
     * action therefore never leads nowhere.</p>
     */
    public static String onlineUrl(String version) {
        String release = normalizeVersion(version);
        return release == null ? AppLinks.WEBSITE : AppEdition.releasePageUrl(release);
    }

    private static String read(InputStream in) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        }
        return content.toString();
    }
}
