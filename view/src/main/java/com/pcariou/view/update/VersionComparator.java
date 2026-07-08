package com.pcariou.view.update;

import java.util.Comparator;

/**
 * Compares dotted numeric application versions (e.g. {@code "1.3.1"}).
 *
 * <p>Versions are compared segment by segment as integers. Missing trailing
 * segments are treated as {@code 0} (so {@code "1.3"} equals {@code "1.3.0"}). A
 * leading {@code v} is ignored, and any pre-release / build metadata after a
 * {@code -} or {@code +} is dropped before comparison. Non-numeric or unknown
 * input degrades to {@code 0}, so an {@code "unknown"} development version is
 * always considered older than any published release.</p>
 */
public final class VersionComparator implements Comparator<String> {

    /** Shared stateless instance. */
    public static final VersionComparator INSTANCE = new VersionComparator();

    private VersionComparator() {
    }

    @Override
    public int compare(String a, String b) {
        return compareVersions(a, b);
    }

    /** Returns a negative value, zero, or a positive value as {@code a} is older, equal to, or newer than {@code b}. */
    public static int compareVersions(String a, String b) {
        int[] pa = parse(a);
        int[] pb = parse(b);
        int length = Math.max(pa.length, pb.length);
        for (int i = 0; i < length; i++) {
            int va = i < pa.length ? pa[i] : 0;
            int vb = i < pb.length ? pb[i] : 0;
            if (va != vb) {
                return va < vb ? -1 : 1;
            }
        }
        return 0;
    }

    /** True when {@code candidate} is strictly newer than {@code current}. */
    public static boolean isNewer(String candidate, String current) {
        return compareVersions(candidate, current) > 0;
    }

    private static int[] parse(String version) {
        if (version == null) {
            return new int[0];
        }
        String v = version.trim();
        if (v.isEmpty()) {
            return new int[0];
        }
        if (v.charAt(0) == 'v' || v.charAt(0) == 'V') {
            v = v.substring(1);
        }
        int meta = indexOfMetadata(v);
        if (meta >= 0) {
            v = v.substring(0, meta);
        }
        if (v.isEmpty()) {
            return new int[0];
        }
        String[] parts = v.split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            numbers[i] = parseSegment(parts[i]);
        }
        return numbers;
    }

    private static int indexOfMetadata(String v) {
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c == '-' || c == '+') {
                return i;
            }
        }
        return -1;
    }

    /** Parses the leading run of digits in a segment; anything else yields {@code 0}. */
    private static int parseSegment(String segment) {
        int end = 0;
        while (end < segment.length() && Character.isDigit(segment.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(segment.substring(0, end));
        } catch (NumberFormatException overflow) {
            return Integer.MAX_VALUE;
        }
    }
}
